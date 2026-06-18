package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.dto.SendDingDTO;
import com.example.dingtalk.entity.SysDing;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.DingMapper;
import com.example.dingtalk.mapper.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DingService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter HM_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MDHM_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final DingMapper dingMapper;
    private final UserMapper userMapper;
    private final WorkNoticeService workNoticeService;

    public DingService(DingMapper dingMapper, UserMapper userMapper, WorkNoticeService workNoticeService) {
        this.dingMapper = dingMapper;
        this.userMapper = userMapper;
        this.workNoticeService = workNoticeService;
    }

    public List<SysDing> list(Long userId) {
        ensureSeeded(userId);
        List<SysDing> list = dingMapper.selectList(
                new LambdaQueryWrapper<SysDing>()
                        .eq(SysDing::getUserId, userId)
                        .orderByDesc(SysDing::getUpdateTime)
                        .orderByDesc(SysDing::getSentTime)
        );
        list.forEach(this::hydrate);
        return list;
    }

    public void resetSamples(Long userId) {
        dingMapper.delete(new LambdaQueryWrapper<SysDing>().eq(SysDing::getUserId, userId));
        seedSamples(userId);
    }

    @Transactional
    public void send(Long userId, SendDingDTO dto) {
        if (dto.getTargetUserId() == null || userId.equals(dto.getTargetUserId())) {
            throw new BizException("请选择一位有效接收人");
        }
        String title = requireText(dto.getTitle(), "请输入 DING 标题");
        String content = requireText(dto.getContent(), "请输入 DING 说明");
        if (dto.getDeadlineTime() == null) {
            throw new BizException("请选择截止时间");
        }

        SysUser sender = requireUser(userId);
        SysUser target = requireUser(dto.getTargetUserId());
        ensureSeeded(userId);
        ensureSeeded(target.getId());

        String senderName = displayName(sender);
        String targetName = displayName(target);
        String type = normalizeType(dto.getType());
        String scene = normalizeScene(dto.getScene());
        String priority = normalizePriority(dto.getPriority());
        String linkKey = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        SysDing sentDing = insertDingRecord(userId, userId, target.getId(), linkKey, title, content, "sent", senderName, targetName,
                type, scene, "pending", priority, dto.getDeadlineTime(), now, now, 0, "",
                List.of(
                        new SysDing.UseCaseItem("继续催办一次", "适合对方还没回执，继续追进度。", "催办", "remind", "再催一下", null),
                        new SysDing.UseCaseItem("补充一条要求", "适合你需要再补充交付口径或说明。", "备注", "note", "写入备注", "补充说明：请处理完成后同步结果和阻塞项。")
                ),
                List.of(new SysDing.HistoryItem(currentTimeLabel(), senderName + " 发起 DING"))
        );
        workNoticeService.notifyUser(
                userId, "ding", "sent", "info", title,
                "已发给 " + targetName + "，等待对方确认", "/ding", sentDing.getId());

        SysDing receivedDing = insertDingRecord(target.getId(), userId, target.getId(), linkKey, title, content, "received", senderName, targetName,
                type, scene, "pending", priority, dto.getDeadlineTime(), now, now, 0, "",
                List.of(
                        new SysDing.UseCaseItem("先确认已接收", "适合你已经看到消息，但还要花时间处理。", "确认收到", "confirm", "确认收到", null),
                        new SysDing.UseCaseItem("补一句处理计划", "适合先告诉对方预计什么时候能完成。", "备注", "note", "写入备注", "预计会在截止前完成处理，处理中如果有风险会及时同步。")
                ),
                List.of(new SysDing.HistoryItem(currentTimeLabel(), senderName + " 发起 DING"))
        );
        workNoticeService.notifyUser(
                target.getId(), "ding", "received", "warning", title,
                senderName + " 给你发来一条 DING", "/ding", receivedDing.getId());
    }

    @Transactional
    public void confirm(Long userId, Long id) {
        SysDing ding = requireOwned(userId, id);
        if ("sent".equals(resolveDirection(ding))) {
            throw new BizException("发起人侧不需要确认收到");
        }
        if ("pending".equals(ding.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            ding.setStatus("confirmed");
            ding.setUpdateTime(now);
            prependHistory(ding, "已确认收到");
            dingMapper.updateById(ding);
            syncLinkedDings(ding, now, "confirmed", null,
                    currentUserName(userId) + " 已确认收到", null);
            Long ownerUserId = ding.getOwnerUserId() != null ? ding.getOwnerUserId() : resolveUserIdByName(ding.getOwnerName());
            workNoticeService.notifyUser(
                    userId, "ding", "confirmed_self", "info", ding.getTitle(),
                    "你已确认收到，事项进入处理中", "/ding", ding.getId());
            workNoticeService.notifyUser(
                    ownerUserId, "ding", "confirmed", "info", ding.getTitle(),
                    currentUserName(userId) + " 已确认收到", "/ding", null);
        }
    }

    @Transactional
    public void done(Long userId, Long id) {
        SysDing ding = requireOwned(userId, id);
        if (!"done".equals(ding.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            ding.setStatus("done");
            ding.setUpdateTime(now);
            prependHistory(ding, "已标记完成");
            dingMapper.updateById(ding);

            String historyForSender = "received".equals(resolveDirection(ding))
                    ? currentUserName(userId) + " 已标记完成"
                    : null;
            String historyForReceiver = "sent".equals(resolveDirection(ding))
                    ? currentUserName(userId) + " 已标记完成"
                    : null;
            syncLinkedDings(ding, now, "done", null, historyForSender, historyForReceiver);
            workNoticeService.notifyUser(
                    userId, "ding", "done_self", "success", ding.getTitle(),
                    "你已完成这条 DING", "/ding", ding.getId());

            Long counterpartUserId = "received".equals(resolveDirection(ding))
                    ? (ding.getOwnerUserId() != null ? ding.getOwnerUserId() : resolveUserIdByName(ding.getOwnerName()))
                    : (ding.getTargetUserId() != null ? ding.getTargetUserId() : resolveUserIdByName(ding.getTargetName()));
            if (counterpartUserId != null && !counterpartUserId.equals(userId)) {
                workNoticeService.notifyUser(
                        counterpartUserId, "ding", "done", "success", ding.getTitle(),
                        currentUserName(userId) + " 已标记完成", "/ding", null);
            }
        }
    }

    @Transactional
    public void remind(Long userId, Long id) {
        SysDing ding = requireOwned(userId, id);
        if (!"sent".equals(resolveDirection(ding))) {
            throw new BizException("只能对已发出的 DING 催办");
        }
        LocalDateTime now = LocalDateTime.now();
        int remindCount = (ding.getRemindCount() == null ? 0 : ding.getRemindCount()) + 1;
        ding.setRemindCount(remindCount);
        ding.setUpdateTime(now);
        prependHistory(ding, "再次催办，累计 " + remindCount + " 次");
        dingMapper.updateById(ding);
        syncLinkedDings(ding, now, null, remindCount, null,
                ding.getOwnerName() + " 再次催办，累计 " + remindCount + " 次");
        Long targetUserId = ding.getTargetUserId() != null ? ding.getTargetUserId() : resolveUserIdByName(ding.getTargetName());
        workNoticeService.notifyUser(
                userId, "ding", "remind_self", "info", ding.getTitle(),
                "已再次催办，累计 " + remindCount + " 次", "/ding", ding.getId());
        workNoticeService.notifyUser(
                targetUserId, "ding", "remind", "warning", ding.getTitle(),
                ding.getOwnerName() + " 再次催办，累计 " + remindCount + " 次", "/ding", null);
    }

    public void saveNote(Long userId, Long id, String note) {
        SysDing ding = requireOwned(userId, id);
        String value = note == null ? "" : note;
        ding.setNote(value);
        ding.setUpdateTime(LocalDateTime.now());
        prependHistory(ding, value.isBlank() ? "清空备注" : "更新备注：" + shorten(value, 26));
        dingMapper.updateById(ding);
    }

    private void ensureSeeded(Long userId) {
        Long count = dingMapper.selectCount(new LambdaQueryWrapper<SysDing>().eq(SysDing::getUserId, userId));
        if (count == 0) {
            seedSamples(userId);
        }
    }

    private void seedSamples(Long userId) {
        String me = currentUserName(userId);
        LocalDateTime now = LocalDateTime.now();

        insertSample(userId, "季度复盘材料今天 18:00 前确认",
                "需要你确认数据页、风险页和下季度资源申请页，确认后我再发给管理层。",
                "received", "陈豫琪", me, "review", "材料确认", "pending", "high",
                now.withHour(18).withMinute(0).withSecond(0).withNano(0),
                now.withHour(9).withMinute(25).withSecond(0).withNano(0),
                now.withHour(9).withMinute(25).withSecond(0).withNano(0),
                1, "",
                List.of(
                        new SysDing.UseCaseItem("先确认已接收", "适合你已经看到消息，但还要花时间处理内容。", "确认收到", "confirm", "确认收到", null),
                        new SysDing.UseCaseItem("补一句处理计划", "适合告诉对方预计什么时候能给结论。", "备注", "note", "写入备注", "预计 16:30 前完成核对，确认后回给你。")
                ),
                List.of(
                        new SysDing.HistoryItem(formatDingTime(now.withHour(9).withMinute(25)), "陈豫琪 发起 DING"),
                        new SysDing.HistoryItem(formatDingTime(now.withHour(9).withMinute(40)), "已催办 1 次")
                )
        );

        insertSample(userId, "压测问题整改进度同步",
                "连接池参数和缓存命中率优化需要在明天中午前给出处理结果。",
                "sent", me, "刘玉林", "task", "研发跟进", "confirmed", "medium",
                now.plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0),
                now.withHour(10).withMinute(12).withSecond(0).withNano(0),
                now.withHour(10).withMinute(45).withSecond(0).withNano(0),
                2, "",
                List.of(
                        new SysDing.UseCaseItem("继续催办一次", "适合你已经收到回执，但还需要追结果。", "催办", "remind", "再催一下", null),
                        new SysDing.UseCaseItem("直接收口为完成", "适合对方已经线下口头同步结束。", "完成", "complete", "标记完成", null)
                ),
                List.of(
                        new SysDing.HistoryItem(formatDingTime(now.withHour(10).withMinute(12)), me + " 发起 DING"),
                        new SysDing.HistoryItem(formatDingTime(now.withHour(10).withMinute(30)), "刘玉林 已确认收到"),
                        new SysDing.HistoryItem(formatDingTime(now.withHour(10).withMinute(45)), "已催办 2 次")
                )
        );

        insertSample(userId, "候选人终面反馈回传",
                "今天下午的前端候选人终面结束后，请在 30 分钟内同步评价。",
                "sent", me, "姜泽之", "feedback", "招聘反馈", "pending", "high",
                now.withHour(17).withMinute(30).withSecond(0).withNano(0),
                now.withHour(13).withMinute(5).withSecond(0).withNano(0),
                now.withHour(13).withMinute(5).withSecond(0).withNano(0),
                0, "",
                List.of(
                        new SysDing.UseCaseItem("提醒对方尽快回传", "适合还没收到任何回执，先追加一次催办。", "催办", "remind", "再催一下", null),
                        new SysDing.UseCaseItem("补一句要求格式", "适合提前说明要按什么格式回反馈。", "备注", "note", "写入备注", "请按 技术面 / 文化面 / 是否建议录用 这三个维度回传。")
                ),
                List.of(new SysDing.HistoryItem(formatDingTime(now.withHour(13).withMinute(5)), me + " 发起 DING"))
        );

        insertSample(userId, "云服务续费审批补充说明",
                "请补一段续费原因和资源增长预估，补完后我会继续流转。",
                "received", "姜泽之", me, "approval", "审批补件", "confirmed", "medium",
                now.withHour(20).withMinute(0).withSecond(0).withNano(0),
                now.withHour(14).withMinute(48).withSecond(0).withNano(0),
                now.withHour(15).withMinute(6).withSecond(0).withNano(0),
                0, "预计在审批备注里补充扩容原因和资源增长预估。",
                List.of(
                        new SysDing.UseCaseItem("直接完成闭环", "适合补充说明已经写完，准备结束这条 DING。", "完成", "complete", "标记完成", null),
                        new SysDing.UseCaseItem("补充处理说明", "适合先记录你准备怎么改，再继续处理。", "备注", "note", "写入备注", "已补充续费原因、成本变化和 Q3 资源预估，准备回传审批。")
                ),
                List.of(
                        new SysDing.HistoryItem(formatDingTime(now.withHour(14).withMinute(48)), "姜泽之 发起 DING"),
                        new SysDing.HistoryItem(formatDingTime(now.withHour(15).withMinute(6)), me + " 已确认收到")
                )
        );

        insertSample(userId, "客户复盘 PPT 已确认",
                "客户复盘简报已经确认完成，可以直接进入发送阶段。",
                "received", "客户成功组", me, "review", "客户复盘", "done", "low",
                now.minusDays(2).withHour(17).withMinute(0).withSecond(0).withNano(0),
                now.minusDays(2).withHour(16).withMinute(18).withSecond(0).withNano(0),
                now.minusDays(2).withHour(16).withMinute(36).withSecond(0).withNano(0),
                0, "已确认版本锁定，允许对外发送。",
                List.of(
                        new SysDing.UseCaseItem("查看闭环样例", "适合给老师演示一条已经完整走完流程的 DING。", "完成态", "focus", "定位当前项", null),
                        new SysDing.UseCaseItem("补一条收尾备注", "适合把“可以发送”这类结论沉淀到记录里。", "备注", "note", "写入备注", "确认已结束，发送前只需再核对客户名称页。")
                ),
                List.of(
                        new SysDing.HistoryItem(formatDingTime(now.minusDays(2).withHour(16).withMinute(18)), "客户成功组 发起 DING"),
                        new SysDing.HistoryItem(formatDingTime(now.minusDays(2).withHour(16).withMinute(20)), me + " 已确认收到"),
                        new SysDing.HistoryItem(formatDingTime(now.minusDays(2).withHour(16).withMinute(36)), "已标记完成")
                )
        );
    }

    private void insertSample(Long userId, String title, String content, String direction, String ownerName,
                              String targetName, String type, String scene, String status, String priority,
                              LocalDateTime deadlineTime, LocalDateTime sentTime, LocalDateTime updateTime,
                              int remindCount, String note, List<SysDing.UseCaseItem> useCases,
                              List<SysDing.HistoryItem> history) {
        insertDingRecord(userId, resolveUserIdByName(ownerName), resolveUserIdByName(targetName), null,
                title, content, direction, ownerName, targetName, type, scene, status, priority,
                deadlineTime, sentTime, updateTime, remindCount, note, useCases, history);
    }

    private SysDing insertDingRecord(Long ownerRecordUserId, Long ownerUserId, Long targetUserId, String linkKey,
                                     String title, String content, String direction, String ownerName,
                                     String targetName, String type, String scene, String status, String priority,
                                     LocalDateTime deadlineTime, LocalDateTime sentTime, LocalDateTime updateTime,
                                     int remindCount, String note, List<SysDing.UseCaseItem> useCases,
                                     List<SysDing.HistoryItem> history) {
        SysDing ding = new SysDing();
        ding.setUserId(ownerRecordUserId);
        ding.setOwnerUserId(ownerUserId);
        ding.setTargetUserId(targetUserId);
        ding.setLinkKey(linkKey);
        ding.setTitle(title);
        ding.setContent(content);
        ding.setDirection(direction);
        ding.setOwnerName(ownerName);
        ding.setTargetName(targetName);
        ding.setType(type);
        ding.setScene(scene);
        ding.setStatus(status);
        ding.setPriority(priority);
        ding.setDeadlineTime(deadlineTime);
        ding.setSentTime(sentTime);
        ding.setUpdateTime(updateTime);
        ding.setRemindCount(remindCount);
        ding.setNote(note);
        ding.setUseCasesJson(toJson(useCases));
        ding.setHistoryJson(toJson(history));
        ding.setCreateTime(LocalDateTime.now());
        dingMapper.insert(ding);
        return ding;
    }

    private SysDing requireOwned(Long userId, Long id) {
        SysDing ding = dingMapper.selectById(id);
        if (ding == null || !userId.equals(ding.getUserId())) {
            throw new BizException("DING 不存在");
        }
        return ding;
    }

    private void hydrate(SysDing ding) {
        if (ding.getDirection() == null || ding.getDirection().isBlank()) {
            ding.setDirection("received");
        }
        if (ding.getOwnerUserId() == null) {
            ding.setOwnerUserId(resolveUserIdByName(ding.getOwnerName()));
        }
        if (ding.getTargetUserId() == null) {
            ding.setTargetUserId(resolveUserIdByName(ding.getTargetName()));
        }
        ding.setOwner(ding.getOwnerName());
        ding.setTarget(ding.getTargetName());
        ding.setDeadline(formatRelativeTime(ding.getDeadlineTime()));
        ding.setSentAt(formatDingTime(ding.getSentTime()));
        ding.setUpdatedAt(formatDingTime(ding.getUpdateTime()));
        ding.setUseCases(readList(ding.getUseCasesJson(), new TypeReference<List<SysDing.UseCaseItem>>() {}));
        ding.setHistory(readList(ding.getHistoryJson(), new TypeReference<List<SysDing.HistoryItem>>() {}));
    }

    private void syncLinkedDings(SysDing source, LocalDateTime updateTime, String status, Integer remindCount,
                                 String historyForSender, String historyForReceiver) {
        if (source.getLinkKey() == null || source.getLinkKey().isBlank()) {
            return;
        }
        List<SysDing> peers = dingMapper.selectList(
                new LambdaQueryWrapper<SysDing>()
                        .eq(SysDing::getLinkKey, source.getLinkKey())
                        .ne(SysDing::getId, source.getId())
        );
        for (SysDing peer : peers) {
            if (status != null) {
                peer.setStatus(status);
            }
            if (remindCount != null) {
                peer.setRemindCount(remindCount);
            }
            peer.setUpdateTime(updateTime);
            String historyText = "sent".equals(resolveDirection(peer)) ? historyForSender : historyForReceiver;
            if (historyText != null && !historyText.isBlank()) {
                prependHistory(peer, historyText);
            }
            dingMapper.updateById(peer);
        }
    }

    private void prependHistory(SysDing ding, String text) {
        List<SysDing.HistoryItem> history = readList(ding.getHistoryJson(), new TypeReference<List<SysDing.HistoryItem>>() {});
        history.add(0, new SysDing.HistoryItem(currentTimeLabel(), text));
        ding.setHistoryJson(toJson(history));
    }

    private String requireText(String text, String errorMessage) {
        if (text == null || text.trim().isEmpty()) {
            throw new BizException(errorMessage);
        }
        return text.trim();
    }

    private String normalizeType(String type) {
        return switch (type) {
            case "review", "task", "feedback", "approval" -> type;
            default -> "task";
        };
    }

    private String normalizeScene(String scene) {
        return scene == null || scene.trim().isEmpty() ? "协同跟进" : scene.trim();
    }

    private String normalizePriority(String priority) {
        return switch (priority) {
            case "high", "medium", "low" -> priority;
            default -> "medium";
        };
    }

    private String resolveDirection(SysDing ding) {
        return ding.getDirection() == null || ding.getDirection().isBlank() ? "received" : ding.getDirection();
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    private Long resolveUserIdByName(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return null;
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getNickname, nickname).last("limit 1"));
        return user != null ? user.getId() : null;
    }

    private String displayName(SysUser user) {
        if (user == null) {
            return "当前用户";
        }
        return user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname() : user.getUsername();
    }

    private String currentUserName(Long userId) {
        return displayName(userMapper.selectById(userId));
    }

    private String formatRelativeTime(LocalDateTime time) {
        if (time == null) return "";
        if (time.toLocalDate().equals(LocalDate.now())) {
            return "今天 " + time.format(HM_FORMATTER);
        }
        if (time.toLocalDate().equals(LocalDate.now().plusDays(1))) {
            return "明天 " + time.format(HM_FORMATTER);
        }
        return time.format(MDHM_FORMATTER);
    }

    private String formatDingTime(LocalDateTime time) {
        if (time == null) return "";
        return time.toLocalDate().equals(LocalDate.now())
                ? "今天 " + time.format(HM_FORMATTER)
                : time.format(MDHM_FORMATTER);
    }

    private String currentTimeLabel() {
        return "今天 " + LocalDateTime.now().format(HM_FORMATTER);
    }

    private String shorten(String text, int limit) {
        return text.length() > limit ? text.substring(0, limit) + "..." : text;
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<T> list = OBJECT_MAPPER.readValue(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            throw new BizException("DING 数据解析失败");
        }
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BizException("DING 数据序列化失败");
        }
    }
}
