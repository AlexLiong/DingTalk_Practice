package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.dto.SendMailDTO;
import com.example.dingtalk.entity.SysFile;
import com.example.dingtalk.entity.SysMailboxMail;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.FileMapper;
import com.example.dingtalk.mapper.MailboxMailMapper;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class MailboxService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter HM_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MDHM_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final MailboxMailMapper mailboxMailMapper;
    private final UserMapper userMapper;
    private final FileMapper fileMapper;
    private final WorkNoticeService workNoticeService;

    public MailboxService(MailboxMailMapper mailboxMailMapper, UserMapper userMapper,
                          FileMapper fileMapper, WorkNoticeService workNoticeService) {
        this.mailboxMailMapper = mailboxMailMapper;
        this.userMapper = userMapper;
        this.fileMapper = fileMapper;
        this.workNoticeService = workNoticeService;
    }

    public List<SysMailboxMail> list(Long userId) {
        ensureSeeded(userId);
        List<SysMailboxMail> list = mailboxMailMapper.selectList(
                new LambdaQueryWrapper<SysMailboxMail>()
                        .eq(SysMailboxMail::getUserId, userId)
                        .orderByDesc(SysMailboxMail::getSendTime)
                        .orderByDesc(SysMailboxMail::getCreateTime)
        );
        list.forEach(this::hydrate);
        return list;
    }

    public void resetSamples(Long userId) {
        mailboxMailMapper.delete(new LambdaQueryWrapper<SysMailboxMail>().eq(SysMailboxMail::getUserId, userId));
        seedSamples(userId);
    }

    @Transactional
    public void send(Long userId, SendMailDTO dto) {
        SysUser sender = requireUser(userId);
        List<Long> recipientIds = sanitizeUserIds(dto.getRecipientIds(), userId);
        if (recipientIds.isEmpty()) {
            throw new BizException("请至少选择一位收件人");
        }
        List<Long> ccIds = sanitizeUserIds(dto.getCcIds(), userId);
        ccIds.removeAll(recipientIds);

        String subject = requireText(dto.getSubject(), "请输入邮件主题");
        String content = requireText(dto.getContent(), "请输入邮件正文");
        String priority = normalizePriority(dto.getPriority());
        String tag = normalizeTag(dto.getTag());
        List<SysMailboxMail.AttachmentItem> attachments = loadAttachments(dto.getAttachmentIds(), userId);

        ensureSeeded(userId);
        List<SysUser> recipients = loadUsers(recipientIds);
        List<SysUser> ccUsers = loadUsers(ccIds);
        recipients.forEach(user -> ensureSeeded(user.getId()));
        ccUsers.forEach(user -> ensureSeeded(user.getId()));

        List<String> recipientNames = toUserNames(recipients);
        List<String> ccNames = toUserNames(ccUsers);
        LocalDateTime now = LocalDateTime.now();
        String preview = buildPreview(content);
        String senderName = displayName(sender);
        String senderRole = displayRole(sender);

        SysMailboxMail sentMail = insertMailRecord(userId, userId, "sent", senderName, senderRole, subject, preview, content, now,
                0, 0, 0, tag, priority, recipientNames, ccNames,
                attachments, "", List.of(),
                List.of(new SysMailboxMail.HistoryItem(currentTimeLabel(), buildSendHistory(recipientNames, ccNames))));
        workNoticeService.notifyUser(
                userId, "mailbox", "sent", "info", subject,
                buildSendNoticeSummary(recipientNames, ccNames, attachments.size()),
                "/mailbox", sentMail.getId());

        for (SysUser recipient : recipients) {
            SysMailboxMail recipientMail = insertMailRecord(recipient.getId(), userId, "received", senderName, senderRole, subject, preview, content, now,
                    1, 0, 0, tag, priority, recipientNames, ccNames,
                    attachments, "", List.of(),
                    List.of(new SysMailboxMail.HistoryItem(currentTimeLabel(), "收到来自 " + senderName + " 的新邮件")));
            workNoticeService.notifyUser(
                    recipient.getId(), "mailbox", "received", "warning", subject,
                    senderName + " 发来一封新邮件" + attachmentSuffix(attachments.size()),
                    "/mailbox", recipientMail.getId());
        }
        for (SysUser ccUser : ccUsers) {
            SysMailboxMail ccMail = insertMailRecord(ccUser.getId(), userId, "received", senderName, senderRole, subject, preview, content, now,
                    1, 0, 0, tag, priority, recipientNames, ccNames,
                    attachments, "", List.of(),
                    List.of(new SysMailboxMail.HistoryItem(currentTimeLabel(), "收到来自 " + senderName + " 的抄送邮件")));
            workNoticeService.notifyUser(
                    ccUser.getId(), "mailbox", "cc", "info", subject,
                    senderName + " 抄送给你一封邮件" + attachmentSuffix(attachments.size()),
                    "/mailbox", ccMail.getId());
        }
    }

    public void open(Long userId, Long id) {
        SysMailboxMail mail = requireOwned(userId, id);
        if (mail.getUnread() != null && mail.getUnread() == 1) {
            mail.setUnread(0);
            mailboxMailMapper.updateById(mail);
        }
    }

    public void updateRead(Long userId, Long id, boolean unread) {
        SysMailboxMail mail = requireOwned(userId, id);
        mail.setUnread(unread ? 1 : 0);
        prependHistory(mail, unread ? "重新标记为未读" : "已标记为已读");
        mailboxMailMapper.updateById(mail);
    }

    public void updateStar(Long userId, Long id, boolean starred) {
        SysMailboxMail mail = requireOwned(userId, id);
        mail.setStarred(starred ? 1 : 0);
        prependHistory(mail, starred ? "已设为星标" : "已取消星标");
        mailboxMailMapper.updateById(mail);
    }

    public void archive(Long userId, Long id) {
        SysMailboxMail mail = requireOwned(userId, id);
        mail.setArchived(1);
        prependHistory(mail, "邮件已归档");
        mailboxMailMapper.updateById(mail);
    }

    public void saveDraft(Long userId, Long id, String draft) {
        SysMailboxMail mail = requireOwned(userId, id);
        mail.setDraft(draft == null ? "" : draft);
        prependHistory(mail, "已保存回复草稿");
        mailboxMailMapper.updateById(mail);
    }

    @Transactional
    public void sendReply(Long userId, Long id, String content) {
        String trimmed = requireText(content, "请输入回复内容");
        SysMailboxMail mail = requireOwned(userId, id);
        if ("sent".equals(mail.getDirection())) {
            throw new BizException("已发出的邮件不支持在这里直接回复");
        }

        Long targetUserId = resolveReplyTargetUserId(mail);
        if (targetUserId == null || userId.equals(targetUserId)) {
            throw new BizException("这封邮件暂不支持直接回复");
        }

        SysUser sender = requireUser(userId);
        SysUser target = requireUser(targetUserId);
        ensureSeeded(userId);
        ensureSeeded(targetUserId);

        String subject = buildReplySubject(mail.getSubject());
        String preview = buildPreview(trimmed);
        String senderName = displayName(sender);
        String senderRole = displayRole(sender);
        List<String> recipientNames = List.of(displayName(target));
        LocalDateTime now = LocalDateTime.now();

        mail.setDraft("");
        mail.setUnread(0);
        prependHistory(mail, "已发送回复：" + shorten(trimmed, 28));
        mailboxMailMapper.updateById(mail);

        SysMailboxMail sentReply = insertMailRecord(userId, userId, "sent", senderName, senderRole, subject, preview, trimmed, now,
                0, 0, 0, normalizeTag(mail.getTagName()), normalizePriority(mail.getPriority()), recipientNames, List.of(),
                List.of(), "", List.of(),
                List.of(new SysMailboxMail.HistoryItem(currentTimeLabel(), "已回复给 " + displayName(target))));
        workNoticeService.notifyUser(
                userId, "mailbox", "reply_sent", "info", subject,
                "回复已发给 " + displayName(target), "/mailbox", sentReply.getId());

        SysMailboxMail receivedReply = insertMailRecord(targetUserId, userId, "received", senderName, senderRole, subject, preview, trimmed, now,
                1, 0, 0, normalizeTag(mail.getTagName()), normalizePriority(mail.getPriority()), recipientNames, List.of(),
                List.of(), "", List.of(),
                List.of(new SysMailboxMail.HistoryItem(currentTimeLabel(), "收到 " + senderName + " 的回复")));
        workNoticeService.notifyUser(
                targetUserId, "mailbox", "reply_received", "warning", subject,
                senderName + " 回复了你的邮件", "/mailbox", receivedReply.getId());
    }

    private void ensureSeeded(Long userId) {
        Long count = mailboxMailMapper.selectCount(new LambdaQueryWrapper<SysMailboxMail>().eq(SysMailboxMail::getUserId, userId));
        if (count == 0) {
            seedSamples(userId);
        }
    }

    private void seedSamples(Long userId) {
        String me = currentUserName(userId);
        LocalDateTime now = LocalDateTime.now();

        insertSample(userId, "陈豫琪", "产品经理", "Sprint 12 排期确认",
                "已把产品、研发、测试的时间点重新对齐，麻烦今天内确认。",
                "辰新，Sprint 12 的排期我重新过了一版，前端提测调整到周四下午，后端联调放到周五上午。\n\n你这边如果没有额外风险，我就按这个版本同步到项目群，并给测试补一版提测节奏说明。",
                now.withHour(9).withMinute(18).withSecond(0).withNano(0),
                1, 1, 0, "项目", "high",
                List.of(me), List.of("周乐恒", "刘玉林"),
                List.of(demoAttachment("排期确认表.xlsx", "98 KB")),
                "",
                List.of(
                        new SysMailboxMail.UseCaseItem("直接确认排期", "适合没有额外风险，允许产品直接同步到群里的场景。", "确认", "排期我确认过了，按这个版本同步即可。测试节奏保持你邮件里的安排。"),
                        new SysMailboxMail.UseCaseItem("补充一个风险提醒", "适合还需要提醒接口联调或提测窗口的场景。", "补充风险", "整体排期可以，唯一要注意的是周五上午的联调窗口别再压缩了，不然测试时间会太紧。")
                ),
                List.of(
                        new SysMailboxMail.HistoryItem(formatMailTime(now.withHour(9).withMinute(18)), "收到排期确认邮件"),
                        new SysMailboxMail.HistoryItem(formatMailTime(now.withHour(9).withMinute(20)), "已自动加入项目分类")
                )
        );

        insertSample(userId, "钉邮系统", "系统通知", "本周会议室预定汇总",
                "你本周共有 4 个会议预定，其中 1 个冲突需要处理。",
                "系统提醒：你本周在 3 号会议室、6 号会议室共有 4 个预定，其中周四 15:00 的两个预定发生冲突。\n\n如果不处理，系统会在明天 10:00 自动保留先创建的那一个。",
                now.withHour(8).withMinute(42).withSecond(0).withNano(0),
                1, 0, 0, "系统", "medium",
                List.of(me), List.of(),
                List.of(),
                "",
                List.of(
                        new SysMailboxMail.UseCaseItem("转发给行政协助调整", "适合需要行政帮忙挪会场的情况。", "转发", "麻烦帮我看下周四 15:00 的会议室冲突，优先保留客户沟通那场，内部讨论可以往后挪 30 分钟。"),
                        new SysMailboxMail.UseCaseItem("内部提醒相关同事", "适合同步给参会人调整时间。", "提醒", "周四 15:00 的会议室有冲突，我这边在调整时间，先别按原计划过去，稍后重新发日历。")
                ),
                List.of(new SysMailboxMail.HistoryItem(formatMailTime(now.withHour(8).withMinute(42)), "系统邮件到达收件箱"))
        );

        insertSample(userId, "刘玉林", "后端工程师", "接口压测结果已发你",
                "压测报告里把连接池和缓存命中率问题都标出来了。",
                "我把昨晚的接口压测结果发过来了，结论是连接池峰值确实偏高，另外热点接口的缓存命中率还有提升空间。\n\n你先看下，如果要我下午同步，我这边可以再补一页结论。",
                now.minusDays(2).withHour(20).withMinute(16).withSecond(0).withNano(0),
                0, 1, 0, "研发", "high",
                List.of(me), List.of("周乐恒"),
                List.of(demoAttachment("压测报告.pdf", "186 KB")),
                "下午 3 点前把结论页补完，重点写高峰超时和缓存命中率。",
                List.of(
                        new SysMailboxMail.UseCaseItem("要求补一页结论", "适合你需要直接带进复盘会的情况。", "补充", "麻烦再补一页结论，重点写连接池高峰超时、缓存命中率以及建议的改动顺序。"),
                        new SysMailboxMail.UseCaseItem("安排当面同步", "适合问题影响面较大，需要口头过一遍。", "约会", "报告我先看到了，下午 4 点你来我工位过一遍，顺手把灰度方案也带上。")
                ),
                List.of(
                        new SysMailboxMail.HistoryItem(formatMailTime(now.minusDays(2).withHour(20).withMinute(16)), "收到压测报告"),
                        new SysMailboxMail.HistoryItem(formatMailTime(now.minusDays(2).withHour(20).withMinute(30)), "已设为星标待跟进")
                )
        );

        insertSample(userId, "姜泽之", "HR 主管", "下周面试安排",
                "Java 后端和前端岗位各安排了两轮面试，请确认时间。",
                "下周一和周二分别安排了 Java 后端、前端岗位的两轮面试，技术面和 HR 面时间我都排好了。\n\n你如果有时间冲突，今晚前回我，我明早统一改。",
                now.minusDays(2).withHour(17).withMinute(3).withSecond(0).withNano(0),
                0, 0, 0, "HR", "medium",
                List.of(me), List.of(),
                List.of(demoAttachment("候选人安排表.docx", "54 KB")),
                "",
                List.of(
                        new SysMailboxMail.UseCaseItem("直接确认可参加", "适合时间没有冲突，允许 HR 直接发邀请的情况。", "确认", "时间我看过了，没有冲突，你直接按这个安排给候选人发面试通知即可。"),
                        new SysMailboxMail.UseCaseItem("只调整一场时间", "适合有单个时间点撞会的情况。", "改单", "周二下午那场我可能会撞客户会，麻烦改到 16:30 之后，其它场次保持不变。")
                ),
                List.of(new SysMailboxMail.HistoryItem(formatMailTime(now.minusDays(2).withHour(17).withMinute(3)), "收到面试安排"))
        );

        insertSample(userId, "客户成功组", "客户成功", "客户复盘材料待确认",
                "周五要给客户做季度复盘，数据页和计划页还差最终确认。",
                "本周五要给客户做季度复盘，材料已经合成一版。\n\n数据页和下季度计划页还需要你过一下，尤其是稳定性和资源投入那部分，避免口径不一致。",
                now.minusDays(3).withHour(14).withMinute(27).withSecond(0).withNano(0),
                0, 0, 0, "客户", "high",
                List.of(me), List.of("陈豫琪"),
                List.of(demoAttachment("客户复盘简报.pptx", "321 KB")),
                "",
                List.of(
                        new SysMailboxMail.UseCaseItem("确认可对外发送", "适合口径已经统一，只差最终拍板。", "对外", "我这边确认过了，可以按当前版本对外发送，稳定性页和下季度计划页口径没问题。"),
                        new SysMailboxMail.UseCaseItem("要求补资源说明", "适合还需要把投入和预期讲清楚。", "补内容", "先别发，资源投入那一页还不够完整，补一句扩容原因和预期收益，再给我过一版。")
                ),
                List.of(new SysMailboxMail.HistoryItem(formatMailTime(now.minusDays(3).withHour(14).withMinute(27)), "收到客户复盘材料确认请求"))
        );
    }

    private void insertSample(Long userId, String sender, String senderRole, String subject, String preview,
                              String content, LocalDateTime sendTime, int unread, int starred, int archived,
                              String tag, String priority, List<String> recipients, List<String> cc,
                              List<SysMailboxMail.AttachmentItem> attachments, String draft,
                              List<SysMailboxMail.UseCaseItem> useCases, List<SysMailboxMail.HistoryItem> history) {
        insertMailRecord(userId, resolveUserIdByName(sender), "received", sender, senderRole, subject, preview, content, sendTime,
                unread, starred, archived, tag, priority, recipients, cc, attachments, draft, useCases, history);
    }

    private SysMailboxMail insertMailRecord(Long ownerUserId, Long senderId, String direction, String sender, String senderRole,
                                            String subject, String preview, String content, LocalDateTime sendTime,
                                            int unread, int starred, int archived, String tag, String priority,
                                            List<String> recipients, List<String> cc, List<SysMailboxMail.AttachmentItem> attachments,
                                            String draft, List<SysMailboxMail.UseCaseItem> useCases,
                                            List<SysMailboxMail.HistoryItem> history) {
        SysMailboxMail mail = new SysMailboxMail();
        mail.setUserId(ownerUserId);
        mail.setSenderId(senderId);
        mail.setDirection(direction);
        mail.setSender(sender);
        mail.setSenderRole(senderRole);
        mail.setSubject(subject);
        mail.setPreview(preview);
        mail.setContent(content);
        mail.setSendTime(sendTime);
        mail.setUnread(unread);
        mail.setStarred(starred);
        mail.setArchived(archived);
        mail.setTagName(tag);
        mail.setPriority(priority);
        mail.setRecipientsJson(toJson(recipients));
        mail.setCcJson(toJson(cc));
        mail.setAttachmentsJson(toJson(attachments));
        mail.setDraft(draft);
        mail.setUseCasesJson(toJson(useCases));
        mail.setHistoryJson(toJson(history));
        mail.setCreateTime(LocalDateTime.now());
        mailboxMailMapper.insert(mail);
        return mail;
    }

    private SysMailboxMail requireOwned(Long userId, Long id) {
        SysMailboxMail mail = mailboxMailMapper.selectById(id);
        if (mail == null || !userId.equals(mail.getUserId())) {
            throw new BizException("邮件不存在");
        }
        return mail;
    }

    private void hydrate(SysMailboxMail mail) {
        if (mail.getDirection() == null || mail.getDirection().isBlank()) {
            mail.setDirection("received");
        }
        if (mail.getSenderId() == null) {
            mail.setSenderId(resolveUserIdByName(mail.getSender()));
        }
        mail.setTag(mail.getTagName());
        mail.setTime(formatMailTime(mail.getSendTime()));
        mail.setRecipients(readStringList(mail.getRecipientsJson()));
        mail.setCc(readStringList(mail.getCcJson()));
        mail.setAttachments(enrichAttachments(readList(mail.getAttachmentsJson(), new TypeReference<List<SysMailboxMail.AttachmentItem>>() {})));
        mail.setUseCases(readList(mail.getUseCasesJson(), new TypeReference<List<SysMailboxMail.UseCaseItem>>() {}));
        mail.setHistory(readList(mail.getHistoryJson(), new TypeReference<List<SysMailboxMail.HistoryItem>>() {}));
    }

    private void prependHistory(SysMailboxMail mail, String text) {
        List<SysMailboxMail.HistoryItem> history = readList(mail.getHistoryJson(), new TypeReference<List<SysMailboxMail.HistoryItem>>() {});
        history.add(0, new SysMailboxMail.HistoryItem(currentTimeLabel(), text));
        mail.setHistoryJson(toJson(history));
    }

    private String buildSendHistory(List<String> recipients, List<String> cc) {
        String text = "已发给 " + String.join("、", recipients);
        if (!cc.isEmpty()) {
            text += "，抄送 " + String.join("、", cc);
        }
        return text;
    }

    private Long resolveReplyTargetUserId(SysMailboxMail mail) {
        if (mail.getSenderId() != null) {
            return mail.getSenderId();
        }
        return resolveUserIdByName(mail.getSender());
    }

    private String buildReplySubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return "回复邮件";
        }
        return subject.startsWith("Re: ") ? subject : "Re: " + subject;
    }

    private String buildPreview(String content) {
        return shorten(content.replace('\n', ' ').trim(), 36);
    }

    private List<SysMailboxMail.AttachmentItem> loadAttachments(List<Long> attachmentIds, Long uploaderId) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<Long> orderedIds = new LinkedHashSet<>();
        for (Long attachmentId : attachmentIds) {
            if (attachmentId != null && attachmentId > 0) {
                orderedIds.add(attachmentId);
            }
        }
        if (orderedIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<SysFile> files = fileMapper.selectBatchIds(orderedIds);
        if (files.size() != orderedIds.size()) {
            throw new BizException("附件不存在或已失效");
        }
        Map<Long, SysFile> fileMap = new LinkedHashMap<>();
        for (SysFile file : files) {
            if (!uploaderId.equals(file.getUploaderId())) {
                throw new BizException("只能发送自己上传的附件");
            }
            fileMap.put(file.getId(), file);
        }
        List<SysMailboxMail.AttachmentItem> attachments = new ArrayList<>();
        for (Long attachmentId : orderedIds) {
            SysFile file = fileMap.get(attachmentId);
            if (file == null) {
                throw new BizException("附件不存在或已失效");
            }
            attachments.add(new SysMailboxMail.AttachmentItem(
                    file.getId(),
                    file.getName(),
                    formatFileSize(file.getSize()),
                    "",
                    file.getUrl(),
                    file.getType()
            ));
        }
        return attachments;
    }

    private List<SysMailboxMail.AttachmentItem> enrichAttachments(List<SysMailboxMail.AttachmentItem> attachments) {
        List<SysMailboxMail.AttachmentItem> enriched = new ArrayList<>();
        for (SysMailboxMail.AttachmentItem attachment : attachments) {
            if (attachment == null) {
                continue;
            }
            if (attachment.getUrl() != null && !attachment.getUrl().isBlank()) {
                enriched.add(attachment);
                continue;
            }
            String demoUrl = demoAttachmentUrl(attachment.getName());
            if (demoUrl == null) {
                enriched.add(attachment);
                continue;
            }
            enriched.add(new SysMailboxMail.AttachmentItem(
                    attachment.getFileId(),
                    attachment.getName(),
                    attachment.getSize(),
                    attachment.getContent(),
                    demoUrl,
                    guessAttachmentType(attachment.getName())
            ));
        }
        return enriched;
    }

    private SysMailboxMail.AttachmentItem demoAttachment(String name, String size) {
        return new SysMailboxMail.AttachmentItem(
                null,
                name,
                size,
                "",
                demoAttachmentUrl(name),
                guessAttachmentType(name)
        );
    }

    private String demoAttachmentUrl(String name) {
        if (name == null) {
            return null;
        }
        return switch (name) {
            case "排期确认表.xlsx" -> "/uploads/demo/schedule-confirm.xlsx";
            case "压测报告.pdf" -> "/uploads/demo/pressure-report.pdf";
            case "候选人安排表.docx" -> "/uploads/demo/candidate-schedule.docx";
            case "客户复盘简报.pptx" -> "/uploads/demo/customer-review.pptx";
            default -> null;
        };
    }

    private String guessAttachmentType(String name) {
        String ext = extensionOf(name);
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default -> "application/octet-stream";
        };
    }

    private String extensionOf(String name) {
        if (name == null) {
            return "";
        }
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return "";
        }
        return name.substring(index + 1).toLowerCase();
    }

    private String requireText(String text, String errorMessage) {
        if (text == null || text.trim().isEmpty()) {
            throw new BizException(errorMessage);
        }
        return text.trim();
    }

    private String normalizePriority(String priority) {
        return switch (priority) {
            case "high", "medium", "low" -> priority;
            default -> "medium";
        };
    }

    private String normalizeTag(String tag) {
        return tag == null || tag.trim().isEmpty() ? "协作" : tag.trim();
    }

    private String buildSendNoticeSummary(List<String> recipients, List<String> cc, int attachmentCount) {
        StringBuilder builder = new StringBuilder("已发给 ").append(String.join("、", recipients));
        if (!cc.isEmpty()) {
            builder.append("，抄送 ").append(String.join("、", cc));
        }
        builder.append(attachmentSuffix(attachmentCount));
        return builder.toString();
    }

    private String attachmentSuffix(int attachmentCount) {
        return attachmentCount > 0 ? "，含 " + attachmentCount + " 个附件" : "";
    }

    private List<Long> sanitizeUserIds(List<Long> ids, Long selfId) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<Long> values = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && !id.equals(selfId)) {
                values.add(id);
            }
        }
        return new ArrayList<>(values);
    }

    private List<SysUser> loadUsers(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<SysUser> users = userMapper.selectBatchIds(userIds);
        if (users.size() != userIds.size()) {
            throw new BizException("收件人不存在");
        }
        Map<Long, SysUser> mapping = new LinkedHashMap<>();
        for (SysUser user : users) {
            mapping.put(user.getId(), user);
        }
        List<SysUser> ordered = new ArrayList<>();
        for (Long id : userIds) {
            SysUser user = mapping.get(id);
            if (user == null) {
                throw new BizException("收件人不存在");
            }
            ordered.add(user);
        }
        return ordered;
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

    private List<String> toUserNames(List<SysUser> users) {
        List<String> names = new ArrayList<>();
        for (SysUser user : users) {
            names.add(displayName(user));
        }
        return names;
    }

    private String displayName(SysUser user) {
        if (user == null) {
            return "当前用户";
        }
        return user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname() : user.getUsername();
    }

    private String displayRole(SysUser user) {
        if (user == null || user.getJobTitle() == null || user.getJobTitle().isBlank()) {
            return "成员";
        }
        return user.getJobTitle();
    }

    private String currentUserName(Long userId) {
        return displayName(userMapper.selectById(userId));
    }

    private String formatMailTime(LocalDateTime time) {
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

    private String formatFileSize(Long size) {
        if (size == null || size <= 0) {
            return "";
        }
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        }
        return String.format("%.1f MB", size / 1024.0 / 1024.0);
    }

    private List<String> readStringList(String json) {
        return readList(json, new TypeReference<List<String>>() {});
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<T> list = OBJECT_MAPPER.readValue(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            throw new BizException("邮件数据解析失败");
        }
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BizException("邮件数据序列化失败");
        }
    }
}
