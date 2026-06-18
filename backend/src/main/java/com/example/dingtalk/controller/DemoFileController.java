package com.example.dingtalk.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
public class DemoFileController {

    private static final Map<String, DemoFile> DEMO_FILES = Map.of(
            "q2-rd-weekly.pdf",
            new DemoFile(
                    "Q2-研发周报.pdf",
                    MediaType.APPLICATION_PDF,
                    """
                    Q2 研发周报示例
                    - 本周完成：文档中心、邮箱阅读区、DING 状态样式细化
                    - 当前风险：客户复盘材料今晚锁版，压测优化待确认
                    - 下周计划：完善添加弹层和聊天会话联动
                    """
            ),
            "product-roadmap.docx",
            new DemoFile(
                    "产品迭代排期.docx",
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                    """
                    产品迭代排期示例
                    1. 周三上午完成需求评审
                    2. 周四下午前端提测
                    3. 周五上午后端联调
                    4. 下周一回归并准备上线
                    """
            ),
            "sprint-burndown.csv",
            new DemoFile(
                    "燃尽图数据.csv",
                    new MediaType("text", "csv", StandardCharsets.UTF_8),
                    """
                    日期,计划剩余,实际剩余
                    06-12,40,40
                    06-13,32,34
                    06-14,24,26
                    06-15,16,18
                    06-16,8,10
                    """
            ),
            "schedule-confirm.xlsx",
            new DemoFile(
                    "排期确认表.xlsx",
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                    """
                    模块,负责人,开始时间,结束时间,备注
                    首页改版,周乐恒,06-17,06-20,周四下午提测
                    用户中心接口,刘玉林,06-17,06-21,周五上午联调
                    回归验证,测试组,06-24,06-26,周五验收
                    """
            ),
            "pressure-report.pdf",
            new DemoFile(
                    "压测报告.pdf",
                    MediaType.APPLICATION_PDF,
                    """
                    压测报告示例
                    - 连接池峰值偏高，建议扩容并收敛慢查询
                    - 热点接口缓存命中率 71%，仍有提升空间
                    - 高峰时段接口超时主要出现在 14:00-14:20
                    """
            ),
            "candidate-schedule.docx",
            new DemoFile(
                    "候选人安排表.docx",
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                    """
                    候选人安排表
                    1. 周一 10:00 Java 后端一面
                    2. 周一 15:30 Java 后端二面
                    3. 周二 14:00 前端一面
                    4. 周二 16:30 前端二面
                    """
            ),
            "customer-review.pptx",
            new DemoFile(
                    "客户复盘简报.pptx",
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
                    """
                    客户复盘简报示例
                    - 目标回顾：交付稳定性提升 12%
                    - 关键问题：高峰时段接口超时
                    - 下阶段计划：扩容、缓存命中率优化、告警收敛
                    """
            ),
            "brand-assets.zip",
            new DemoFile(
                    "品牌素材包.zip",
                    MediaType.parseMediaType("application/zip"),
                    """
                    品牌素材包示例
                    /logo
                    /banner
                    /token/colors.json
                    /readme.md
                    """
            )
    );

    @GetMapping("/uploads/demo/{filename}")
    public ResponseEntity<byte[]> downloadDemo(@PathVariable String filename) {
        DemoFile demoFile = DEMO_FILES.get(filename);
        if (demoFile == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] content = demoFile.content().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(demoFile.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(demoFile.downloadName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(content);
    }

    private record DemoFile(String downloadName, MediaType mediaType, String content) {
    }
}
