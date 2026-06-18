package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dingtalk.entity.SysNotice;
import com.example.dingtalk.entity.SysNoticeRead;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.NoticeMapper;
import com.example.dingtalk.mapper.NoticeReadMapper;
import com.example.dingtalk.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final NoticeReadMapper readMapper;
    private final UserMapper userMapper;

    public NoticeService(NoticeMapper noticeMapper, NoticeReadMapper readMapper, UserMapper userMapper) {
        this.noticeMapper = noticeMapper;
        this.readMapper = readMapper;
        this.userMapper = userMapper;
    }

    /** 公告列表 (已发布) */
    public IPage<SysNotice> page(int pageNum, int pageSize, Long currentUserId) {
        IPage<SysNotice> page = noticeMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysNotice>()
                        .eq(SysNotice::getStatus, 1)
                        .orderByDesc(SysNotice::getCreateTime));
        int totalUsers = (int) (long) userMapper.selectCount(null);
        for (SysNotice n : page.getRecords()) {
            fillExtra(n, currentUserId, totalUsers);
        }
        return page;
    }

    /** 管理端: 所有公告(含草稿) */
    public IPage<SysNotice> adminPage(int pageNum, int pageSize) {
        IPage<SysNotice> page = noticeMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysNotice>().orderByDesc(SysNotice::getCreateTime));
        for (SysNotice n : page.getRecords()) {
            SysUser u = userMapper.selectById(n.getPublisherId());
            if (u != null) n.setPublisherName(u.getNickname());
            long readCnt = readMapper.selectCount(new LambdaQueryWrapper<SysNoticeRead>()
                    .eq(SysNoticeRead::getNoticeId, n.getId()));
            n.setReadCount((int) readCnt);
            n.setTotalCount((int) (long) userMapper.selectCount(null));
        }
        return page;
    }

    public SysNotice getById(Long id, Long currentUserId) {
        SysNotice n = noticeMapper.selectById(id);
        if (n == null) return null;
        fillExtra(n, currentUserId, (int) (long) userMapper.selectCount(null));
        // 标记已读
        markRead(id, currentUserId);
        return n;
    }

    public void save(SysNotice notice) {
        if (notice.getStatus() == null) notice.setStatus(1);
        if (notice.getPriority() == null) notice.setPriority(2);
        if (notice.getType() == null) notice.setType(1);
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);
    }

    public void update(SysNotice notice) {
        noticeMapper.updateById(notice);
    }

    public void delete(Long id) {
        noticeMapper.deleteById(id);
        readMapper.delete(new LambdaQueryWrapper<SysNoticeRead>().eq(SysNoticeRead::getNoticeId, id));
    }

    public void markRead(Long noticeId, Long userId) {
        Long cnt = readMapper.selectCount(new LambdaQueryWrapper<SysNoticeRead>()
                .eq(SysNoticeRead::getNoticeId, noticeId)
                .eq(SysNoticeRead::getUserId, userId));
        if (cnt == 0) {
            SysNoticeRead r = new SysNoticeRead();
            r.setNoticeId(noticeId);
            r.setUserId(userId);
            r.setReadTime(LocalDateTime.now());
            readMapper.insert(r);
        }
    }

    private void fillExtra(SysNotice n, Long currentUserId, int totalUsers) {
        SysUser u = userMapper.selectById(n.getPublisherId());
        if (u != null) n.setPublisherName(u.getNickname());
        long readCnt = readMapper.selectCount(new LambdaQueryWrapper<SysNoticeRead>()
                .eq(SysNoticeRead::getNoticeId, n.getId()));
        n.setReadCount((int) readCnt);
        n.setTotalCount((int) totalUsers);
        long myRead = readMapper.selectCount(new LambdaQueryWrapper<SysNoticeRead>()
                .eq(SysNoticeRead::getNoticeId, n.getId())
                .eq(SysNoticeRead::getUserId, currentUserId));
        n.setHasRead(myRead > 0);
    }
}
