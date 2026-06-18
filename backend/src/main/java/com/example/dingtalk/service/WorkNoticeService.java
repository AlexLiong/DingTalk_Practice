package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.entity.SysDing;
import com.example.dingtalk.entity.SysMailboxMail;
import com.example.dingtalk.mapper.DingMapper;
import com.example.dingtalk.mapper.MailboxMailMapper;
import com.example.dingtalk.vo.WorkNoticeVO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WorkNoticeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final MailboxMailMapper mailboxMailMapper;
    private final DingMapper dingMapper;

    public WorkNoticeService(SimpMessagingTemplate messagingTemplate,
                             MailboxMailMapper mailboxMailMapper,
                             DingMapper dingMapper) {
        this.messagingTemplate = messagingTemplate;
        this.mailboxMailMapper = mailboxMailMapper;
        this.dingMapper = dingMapper;
    }

    public void notifyUser(Long userId, String category, String action, String level,
                           String title, String summary, String route, Long refId) {
        if (userId == null) {
            return;
        }
        WorkNoticeVO notice = new WorkNoticeVO();
        notice.setCategory(category);
        notice.setAction(action);
        notice.setLevel(level);
        notice.setTitle(title);
        notice.setSummary(summary);
        notice.setRoute(route);
        notice.setRefId(refId);
        notice.setMailboxUnread(mailboxUnreadCount(userId));
        notice.setDingPending(dingPendingCount(userId));
        notice.setDingUrgent(dingUrgentCount(userId));
        notice.setCreateTime(LocalDateTime.now());
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/work-notice", notice);
    }

    private long mailboxUnreadCount(Long userId) {
        return mailboxMailMapper.selectCount(
                new LambdaQueryWrapper<SysMailboxMail>()
                        .eq(SysMailboxMail::getUserId, userId)
                        .eq(SysMailboxMail::getDirection, "received")
                        .eq(SysMailboxMail::getArchived, 0)
                        .eq(SysMailboxMail::getUnread, 1)
        );
    }

    private long dingPendingCount(Long userId) {
        return dingMapper.selectCount(
                new LambdaQueryWrapper<SysDing>()
                        .eq(SysDing::getUserId, userId)
                        .eq(SysDing::getDirection, "received")
                        .ne(SysDing::getStatus, "done")
        );
    }

    private long dingUrgentCount(Long userId) {
        return dingMapper.selectCount(
                new LambdaQueryWrapper<SysDing>()
                        .eq(SysDing::getUserId, userId)
                        .eq(SysDing::getDirection, "received")
                        .eq(SysDing::getPriority, "high")
                        .ne(SysDing::getStatus, "done")
        );
    }
}
