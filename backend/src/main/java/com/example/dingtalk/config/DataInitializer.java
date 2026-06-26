package com.example.dingtalk.config;

import com.example.dingtalk.entity.*;
import com.example.dingtalk.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 启动时初始化种子数据 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final ChatSessionMapper sessionMapper;
    private final ChatSessionMemberMapper memberMapper;
    private final ChatMessageMapper messageMapper;
    private final NoticeMapper noticeMapper;
    private final TodoMapper todoMapper;
    private final ApprovalMapper approvalMapper;
    private final FileMapper fileMapper;
    private final FavoriteMapper favoriteMapper;
    private final ScheduleMapper scheduleMapper;
    private final MessageReadMapper messageReadMapper;
    private final PasswordEncoder passwordEncoder;

    private final List<Long> allMenuIds = new ArrayList<>();

    public DataInitializer(UserMapper userMapper, DeptMapper deptMapper, RoleMapper roleMapper,
                           MenuMapper menuMapper, UserRoleMapper userRoleMapper, RoleMenuMapper roleMenuMapper,
                           ChatSessionMapper sessionMapper, ChatSessionMemberMapper memberMapper,
                           ChatMessageMapper messageMapper,
                           NoticeMapper noticeMapper, TodoMapper todoMapper, ApprovalMapper approvalMapper,
                           FileMapper fileMapper, FavoriteMapper favoriteMapper, ScheduleMapper scheduleMapper,
                           MessageReadMapper messageReadMapper,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.sessionMapper = sessionMapper;
        this.memberMapper = memberMapper;
        this.messageMapper = messageMapper;
        this.noticeMapper = noticeMapper;
        this.todoMapper = todoMapper;
        this.approvalMapper = approvalMapper;
        this.fileMapper = fileMapper;
        this.favoriteMapper = favoriteMapper;
        this.scheduleMapper = scheduleMapper;
        this.messageReadMapper = messageReadMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 数据库已有数据则跳过，避免覆盖文件库或 MySQL 中的持久化数据
        if (userMapper.selectCount(null) > 0) {
            log.info(">>> 数据库已有数据, 跳过种子初始化");
            return;
        }

        // 1. 部门 (更真实的组织架构)
        Long root = createDept(0L, "0", "杭州星辰科技有限公司", 0);
        Long tech = createDept(root, "0," + root, "技术研发部", 1);
        Long product = createDept(root, "0," + root, "产品运营部", 2);
        Long design = createDept(root, "0," + root, "设计创意部", 3);
        Long hr = createDept(root, "0," + root, "人力行政部", 4);
        Long fe = createDept(tech, "0," + root + "," + tech, "前端组", 1);
        Long be = createDept(tech, "0," + root + "," + tech, "后端组", 2);

        // 2. 菜单 (含按钮权限)
        Long mSystem = createMenu(0L, "系统管理", 1, "/admin", "Setting", null, 1);
        Long mUser = createMenu(mSystem, "用户管理", 2, "/admin/user", "User", "system:user:list", 1);
        createMenu(mUser, "新增用户", 3, null, null, "system:user:add", 1);
        createMenu(mUser, "编辑用户", 3, null, null, "system:user:edit", 2);
        createMenu(mUser, "删除用户", 3, null, null, "system:user:remove", 3);
        createMenu(mUser, "重置密码", 3, null, null, "system:user:resetPwd", 4);
        Long mRole = createMenu(mSystem, "角色管理", 2, "/admin/role", "Avatar", "system:role:list", 2);
        createMenu(mRole, "新增角色", 3, null, null, "system:role:add", 1);
        createMenu(mRole, "编辑角色", 3, null, null, "system:role:edit", 2);
        createMenu(mRole, "删除角色", 3, null, null, "system:role:remove", 3);
        Long mMenu = createMenu(mSystem, "菜单管理", 2, "/admin/menu", "Menu", "system:menu:list", 3);
        createMenu(mMenu, "新增菜单", 3, null, null, "system:menu:add", 1);
        createMenu(mMenu, "编辑菜单", 3, null, null, "system:menu:edit", 2);
        createMenu(mMenu, "删除菜单", 3, null, null, "system:menu:remove", 3);
        Long mDept = createMenu(mSystem, "部门管理", 2, "/admin/dept", "OfficeBuilding", "system:dept:list", 4);
        createMenu(mDept, "新增部门", 3, null, null, "system:dept:add", 1);
        createMenu(mDept, "编辑部门", 3, null, null, "system:dept:edit", 2);
        createMenu(mDept, "删除部门", 3, null, null, "system:dept:remove", 3);
                Long mNotice = createMenu(0L,"公告管理",1,"/admin/notice","InfoFilled","notice:list",1);
        createMenu(mNotice,"发布公告",3,null,null,"notice:add",1);
        createMenu(mNotice,"编辑公告",3,null,null,"notice:edit",2);
        createMenu(mNotice,"删除公告",3,null,null,"notice:remove",3);
        Long mDashboard = createMenu(0L,"数据看板",1,"/admin/dashboard","DataBoard","dashboard:view",1);

        // 3. 角色
        Long roleAdmin = createRole("超级管理员", "admin", 1, "拥有所有权限");
        Long roleStaff = createRole("普通员工", "staff", 2, "仅协作功能, 无后台管理");
        for (Long mid : allMenuIds) roleMenuMapper.insert(roleAdmin, mid);

        // 4. 用户 (真实姓名)
        String pwd = passwordEncoder.encode("Boz@2026");
        Long uChenxin  = createUser("admin",    pwd, "洪辰新", "技术总监",   tech,    "技术研发部", 1);
        Long uYiwei    = createUser("chenyuqi",   pwd, "陈豫琪", "产品经理",   product, "产品运营部", 2);
        Long uHaoyu    = createUser("zhouleheng", pwd, "周乐恒", "前端工程师", fe,      "前端组",     1);
        Long uMeilin   = createUser("liangqinwei", pwd, "梁秦玮", "UI设计师",   design,  "设计创意部", 2);
        Long uJianfeng = createUser("liuyulin",   pwd, "刘玉林", "后端工程师", be,      "后端组",     1);
        Long uXiaotong = createUser("jiangzezhi", pwd, "姜泽之", "HR主管",     hr,      "人力行政部", 2);
        // 分配角色
        userRoleMapper.insert(uChenxin, roleAdmin);
        userRoleMapper.insert(uYiwei, roleStaff);
        userRoleMapper.insert(uHaoyu, roleStaff);
        userRoleMapper.insert(uMeilin, roleStaff);
        userRoleMapper.insert(uJianfeng, roleStaff);
        userRoleMapper.insert(uXiaotong, roleStaff);

        LocalDateTime base = LocalDateTime.now().minusHours(3);

        // ====== 5. 群聊 ======

        // 群1: 星辰科技-全员群
        Long g1 = createSession(2, "星辰科技-全员群", uChenxin, "公司制度: 上班9:00, 下班18:00, 午休12:00-13:30");
        addMember(g1, uChenxin, 1); addMember(g1, uYiwei, 2); addMember(g1, uHaoyu, 2);
        addMember(g1, uMeilin, 2); addMember(g1, uJianfeng, 2); addMember(g1, uXiaotong, 2);
        addMessage(g1, uXiaotong, "提醒大家: 本月社保基数调整, 请在OA上确认个人信息", base);
        addMessage(g1, uChenxin, "收到, 另外通知下大家, 下周三下午2点公司季度复盘会", base.plusMinutes(5));
        addMessage(g1, uYiwei, "好的, 各部门记得准备Q2总结PPT", base.plusMinutes(8));
        addMessage(g1, uHaoyu, "收到~", base.plusMinutes(10));
        updateSessionLast(g1, "收到~", base.plusMinutes(10));

        // 群2: 产品研发协作群
        Long g2 = createSession(2, "产品研发协作群", uYiwei, "迭代排期: 6/16-6/27 Sprint 12");
        addMember(g2, uYiwei, 1); addMember(g2, uChenxin, 2); addMember(g2, uHaoyu, 2);
        addMember(g2, uMeilin, 2); addMember(g2, uJianfeng, 2);
        Long mentionAdminMsgId = addMessage(g2, uYiwei, "@洪辰新 Sprint 12需求已拆分完, 明天10:30一起确认接口优先级, 有问题群里反馈", base.plusMinutes(20), String.valueOf(uChenxin));
        addMessage(g2, uJianfeng, "用户模块接口文档更新了, 在语雀上", base.plusMinutes(25));
        addMessage(g2, uHaoyu, "我这边首页改版进度60%, 预计明天能提测", base.plusMinutes(30));
        Long mentionHaoyuMsgId = addMessage(g2, uMeilin, "首页的设计稿我刚传到蓝湖了, @周乐恒 看下标注有没有问题", base.plusMinutes(35), String.valueOf(uHaoyu));
        addMessage(g2, uHaoyu, "收到, 我去看看", base.plusMinutes(36));
        addMessage(g2, uChenxin, "辛苦大家, 这个迭代时间比较紧, 有阻塞的及时同步", base.plusMinutes(40));
        updateSessionLast(g2, "辛苦大家, 这个迭代时间比较紧, 有阻塞的及时同步", base.plusMinutes(40));

        // 群3: 技术吹水群
        Long g3 = createSession(2, "技术吹水群 \uD83C\uDF1F", uHaoyu, null);
        addMember(g3, uHaoyu, 1); addMember(g3, uJianfeng, 2); addMember(g3, uChenxin, 2);
        addMessage(g3, uHaoyu, "有人用过 Bun 吗? 比 Node 快好多", base.plusMinutes(45));
        addMessage(g3, uJianfeng, "我在新项目试了下, 确实启动快很多, 但生态还差点", base.plusMinutes(48));
        addMessage(g3, uChenxin, "正式项目先别上, 等稳定了再说", base.plusMinutes(50));
        addMessage(g3, uHaoyu, "哈哈好的, 我自己玩玩 \uD83D\uDE02", base.plusMinutes(51));
        updateSessionLast(g3, "哈哈好的, 我自己玩玩", base.plusMinutes(51));

        // 群4: 设计评审群
        Long g4 = createSession(2, "UI设计评审", uMeilin, "每周三下午3点设计评审");
        addMember(g4, uMeilin, 1); addMember(g4, uYiwei, 2); addMember(g4, uHaoyu, 2);
        addMessage(g4, uMeilin, "V2.0首页视觉稿已更新, 主要改了配色和间距", base.plusMinutes(55));
        addMessage(g4, uYiwei, "看了, 整体感觉不错, 导航栏的icon能再大一点吗", base.plusMinutes(58));
        addMessage(g4, uMeilin, "可以, 我调一版", base.plusMinutes(59));
        updateSessionLast(g4, "可以, 我调一版", base.plusMinutes(59));

        // ====== 6. 单聊 ======

        // 单聊: admin <-> 陈豫琪
        Long s1 = createSession(1, null, null, null);
        addMember(s1, uChenxin, 2); addMember(s1, uYiwei, 2);
        addMessage(s1, uYiwei, "辰新, Q3的OKR你定了吗?", base.plusMinutes(60));
        addMessage(s1, uChenxin, "还在写, 技术侧主要围绕性能优化和架构升级", base.plusMinutes(62));
        addMessage(s1, uYiwei, "好的, 产品侧我这边重点是用户增长和留存, 到时候对齐下", base.plusMinutes(65));
        Long okrMsgId = addMessage(s1, uChenxin, "行, 周三下午我们约个会议室聊", base.plusMinutes(66));
        updateSessionLast(s1, "行, 周三下午我们约个会议室聊", base.plusMinutes(66));

        // 单聊: admin <-> 周乐恒
        Long s2 = createSession(1, null, null, null);
        addMember(s2, uChenxin, 2); addMember(s2, uHaoyu, 2);
        addMessage(s2, uHaoyu, "洪总, 首页那个性能问题我定位到了, 是列表没做虚拟滚动", base.plusMinutes(70));
        addMessage(s2, uChenxin, "嗯, 用react-virtualized还是自己写?", base.plusMinutes(72));
        Long reactWindowMsgId = addMessage(s2, uHaoyu, "打算用react-window, 轻量一点", base.plusMinutes(73));
        addMessage(s2, uChenxin, "可以, 注意兼容移动端", base.plusMinutes(74));
        addMessage(s2, uHaoyu, "好的\uD83D\uDC4D", base.plusMinutes(75));
        updateSessionLast(s2, "好的\uD83D\uDC4D", base.plusMinutes(75));

        // 单聊: admin <-> 刘玉林
        Long s3 = createSession(1, null, null, null);
        addMember(s3, uChenxin, 2); addMember(s3, uJianfeng, 2);
        addMessage(s3, uJianfeng, "洪总, 数据库连接池的配置要调一下, 现在高峰期偶尔超时", base.plusMinutes(80));
        addMessage(s3, uChenxin, "最大连接数是多少?", base.plusMinutes(81));
        addMessage(s3, uJianfeng, "20, 我准备调到50, 再加个Druid监控", base.plusMinutes(82));
        addMessage(s3, uChenxin, "可以, 调完灰度观察一天", base.plusMinutes(83));
        updateSessionLast(s3, "可以, 调完灰度观察一天", base.plusMinutes(83));

        // 单聊: admin <-> 梁秦玮
        Long s4 = createSession(1, null, null, null);
        addMember(s4, uChenxin, 2); addMember(s4, uMeilin, 2);
        addMessage(s4, uMeilin, "辰新, 新的品牌色方案出了, 发你看看", base.plusMinutes(90));
        addMessage(s4, uChenxin, "好的发过来", base.plusMinutes(91));
        Long brandColorMsgId = addMessage(s4, uMeilin, "主色改成 #1677FF, 辅色用 #52C41A, 你觉得怎么样?", base.plusMinutes(92));
        addMessage(s4, uChenxin, "不错, 挺清爽的, 就用这套吧", base.plusMinutes(95));
        updateSessionLast(s4, "不错, 挺清爽的, 就用这套吧", base.plusMinutes(95));

        // 单聊: admin <-> 姜泽之
        Long s5 = createSession(1, null, null, null);
        addMember(s5, uChenxin, 2); addMember(s5, uXiaotong, 2);
        addMessage(s5, uXiaotong, "辰新, 技术部还有两个HC, 你看什么时候启动招聘?", base.plusMinutes(100));
        addMessage(s5, uChenxin, "这周就可以发JD了, 要一个Java后端和一个前端", base.plusMinutes(102));
        addMessage(s5, uXiaotong, "好的, 我先在BOSS和拉勾发出去", base.plusMinutes(103));
        updateSessionLast(s5, "好的, 我先在BOSS和拉勾发出去", base.plusMinutes(103));

        // 6.5 为所有历史消息生成已读回执 (让单聊显示"已读", 群聊显示正确已读人数)
        markAllMessagesRead();

        // 6.6 文档 / 收藏 / 日程示例
        createFile("Q2-研发周报.pdf", "/uploads/demo/q2-rd-weekly.pdf", 186432L, "pdf", uChenxin,
                "周报同步", "用于管理层例会同步研发进展、风险和资源诉求。", "已共享",
                LocalDateTime.now().minusDays(2));
        createFile("产品迭代排期.docx", "/uploads/demo/product-roadmap.docx", 96812L, "docx", uChenxin,
                "排期评审", "需求、前后端和测试排期统一确认用文档。", "待评审",
                LocalDateTime.now().minusDays(1).minusHours(3));
        createFile("燃尽图数据.csv", "/uploads/demo/sprint-burndown.csv", 24576L, "csv", uChenxin,
                "数据回传", "燃尽趋势和任务完成率导出，适合二次分析。", "待回收",
                LocalDateTime.now().minusHours(18));
        createFile("客户复盘简报.pptx", "/uploads/demo/customer-review.pptx", 328704L, "pptx", uChenxin,
                "客户复盘", "客户季度复盘演示稿，包含目标回顾、稳定性和下阶段计划。", "已锁版",
                LocalDateTime.now().minusHours(8));
        createFile("品牌素材包.zip", "/uploads/demo/brand-assets.zip", 512064L, "zip", uChenxin,
                "资源交付", "品牌视觉资源、Logo、Banner 和色板 token 打包交付。", "可下载",
                LocalDateTime.now().minusHours(4));

        createFavorite(uChenxin, mentionAdminMsgId, g2, "@洪辰新 Sprint 12需求已拆分完, 明天10:30一起确认接口优先级, 有问题群里反馈", "陈豫琪", LocalDateTime.now().minusHours(3));
        createFavorite(uChenxin, reactWindowMsgId, s2, "打算用react-window, 轻量一点", "周乐恒", LocalDateTime.now().minusHours(2));
        createFavorite(uChenxin, brandColorMsgId, s4, "主色改成 #1677FF, 辅色用 #52C41A, 你觉得怎么样?", "梁秦玮", LocalDateTime.now().minusMinutes(80));

        createSchedule("产品研发周会", "三号会议室，确认 Sprint 12 风险和资源安排", uChenxin,
                LocalDateTime.now().withHour(10).withMinute(30).withSecond(0).withNano(0).plusDays(1),
                LocalDateTime.now().withHour(11).withMinute(30).withSecond(0).withNano(0).plusDays(1),
                0, "#1677ff");
        createSchedule("供应商合同评审", "和行政、财务一起过云服务续费条款", uChenxin,
                LocalDateTime.now().withHour(15).withMinute(0).withSecond(0).withNano(0).plusDays(2),
                LocalDateTime.now().withHour(16).withMinute(0).withSecond(0).withNano(0).plusDays(2),
                0, "#fa8c16");
        createSchedule("季度复盘材料准备", "补齐数据页、风险页、下季度目标页", uChenxin,
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).plusDays(3),
                LocalDateTime.now().withHour(23).withMinute(59).withSecond(0).withNano(0).plusDays(3),
                1, "#52c41a");

        // 7. 种子公告
        SysNotice n1 = new SysNotice();
        n1.setTitle("关于公司办公区域调整的通知");
        n1.setContent("各位同事：\n\n因公司业务发展, 技术研发部将于下周一搬至6楼, 请相关同事提前整理个人物品。\n\n新工位安排已在OA系统公示, 如有疑问请联系行政部姜泽之。\n\n人力行政部\n2026年6月15日");
        n1.setType(1); n1.setPriority(1); n1.setPublisherId(uXiaotong); n1.setStatus(1); n1.setCreateTime(LocalDateTime.now().minusDays(1));
        noticeMapper.insert(n1);
        SysNotice n2 = new SysNotice();
        n2.setTitle("Sprint 12 迭代排期确认");
        n2.setContent("Sprint 12 排期: 6月16日-6月27日\n\n核心目标:\n1. 首页改版上线 (前端: 周乐恒, 设计: 梁秦玮)\n2. 用户中心重构 (后端: 刘玉林)\n3. 消息推送优化 (前端+后端联调)\n\n6/19 中期check, 6/26 提测, 6/27 产品验收\n\n如有排期风险请提前反馈。");
        n2.setType(2); n2.setPriority(2); n2.setPublisherId(uYiwei); n2.setStatus(1); n2.setCreateTime(LocalDateTime.now().minusHours(3));
        noticeMapper.insert(n2);
        SysNotice n3 = new SysNotice();
        n3.setTitle("端午节放假通知");
        n3.setContent("各位同事：\n\n根据国务院办公厅通知, 端午节放假安排如下:\n6月28日(星期六)至6月30日(星期一)放假调休, 共3天。\n\n请大家提前安排好工作。祝大家端午安康！\n\n人力行政部");
        n3.setType(1); n3.setPriority(2); n3.setPublisherId(uXiaotong); n3.setStatus(1); n3.setCreateTime(LocalDateTime.now().minusHours(6));
        noticeMapper.insert(n3);

        // 8. 种子待办
        createTodo("首页改版前端开发", "按V2.0设计稿实现, 注意响应式适配", uYiwei, uHaoyu, 1, 1);
        createTodo("用户中心接口重构", "拆分用户信息和账户设置接口, 补充单元测试", uChenxin, uJianfeng, 1, 1);
        createTodo("品牌色方案确认", "确认主色/辅色/渐变方案, 更新设计规范文档", uYiwei, uMeilin, 2, 2);
        createTodo("Q3 OKR 撰写", "包含技术侧OKR和个人成长目标", uChenxin, uChenxin, 2, 0);
        createTodo("招聘JD撰写", "Java后端+前端各一个岗位的JD", uChenxin, uXiaotong, 2, 0);
        createTodo("消息推送功能联调", "WebSocket推送+离线消息补偿", uYiwei, uHaoyu, 1, 0);

        // 9. 种子审批
        SysApproval a1 = new SysApproval();
        a1.setType("leave"); a1.setTitle("年假申请 - 陈豫琪");
        a1.setContent("{\"reason\":\"家里有事需要处理\",\"days\":2,\"start\":\"2026-06-23\",\"end\":\"2026-06-24\"}");
        a1.setApplicantId(uYiwei); a1.setApproverId(uChenxin); a1.setStatus(0); a1.setCreateTime(LocalDateTime.now().minusHours(1));
        approvalMapper.insert(a1);
        SysApproval a2 = new SysApproval();
        a2.setType("expense"); a2.setTitle("差旅报销 - 刘玉林");
        a2.setContent("{\"reason\":\"深圳客户现场技术支持, 交通+住宿\",\"amount\":3680}");
        a2.setApplicantId(uJianfeng); a2.setApproverId(uChenxin); a2.setStatus(0); a2.setCreateTime(LocalDateTime.now().minusMinutes(30));
        approvalMapper.insert(a2);
        SysApproval a3 = new SysApproval();
        a3.setType("leave"); a3.setTitle("病假申请 - 周乐恒");
        a3.setContent("{\"reason\":\"感冒发烧需要休息\",\"days\":1,\"start\":\"2026-06-17\",\"end\":\"2026-06-17\"}");
        a3.setApplicantId(uHaoyu); a3.setApproverId(uChenxin); a3.setStatus(1); a3.setRemark("注意休息, 早日康复"); a3.setApproveTime(LocalDateTime.now().minusHours(2));
        a3.setCreateTime(LocalDateTime.now().minusHours(4));
        approvalMapper.insert(a3);
        SysApproval a4 = new SysApproval();
        a4.setType("expense"); a4.setTitle("云服务续费申请 - 洪辰新");
        a4.setContent("{\"reason\":\"Q3 环境扩容和监控续费\",\"amount\":12800}");
        a4.setApplicantId(uChenxin); a4.setApproverId(uXiaotong); a4.setStatus(0); a4.setCreateTime(LocalDateTime.now().minusMinutes(45));
        approvalMapper.insert(a4);
        SysApproval a5 = new SysApproval();
        a5.setType("leave"); a5.setTitle("外出参会申请 - 洪辰新");
        a5.setContent("{\"reason\":\"参加杭州 AI 技术闭门会\",\"days\":1,\"start\":\"2026-06-20\",\"end\":\"2026-06-20\"}");
        a5.setApplicantId(uChenxin); a5.setApproverId(uXiaotong); a5.setStatus(1); a5.setRemark("已登记外出行程"); a5.setApproveTime(LocalDateTime.now().minusHours(6));
        a5.setCreateTime(LocalDateTime.now().minusHours(10));
        approvalMapper.insert(a5);

        log.info(">>> 种子数据初始化完成: 6部门, {}个菜单, 2角色, 6用户(密码 Boz@2026), 4群聊, 5单聊, 5文件, 3收藏, 3日程, 3公告, 6待办, 5审批", allMenuIds.size());
    }

    private Long createDept(Long parentId, String ancestors, String name, int sort) {
        SysDept d = new SysDept();
        d.setParentId(parentId);
        d.setAncestors(ancestors);
        d.setName(name);
        d.setSort(sort);
        d.setStatus(1);
        d.setCreateTime(LocalDateTime.now());
        deptMapper.insert(d);
        return d.getId();
    }

    private Long createMenu(Long parentId, String name, int type, String path,
                            String icon, String perms, int sort) {
        SysMenu m = new SysMenu();
        m.setParentId(parentId);
        m.setName(name);
        m.setType(type);
        m.setPath(path);
        m.setIcon(icon);
        m.setPerms(perms);
        m.setSort(sort);
        m.setVisible(1);
        menuMapper.insert(m);
        allMenuIds.add(m.getId());
        return m.getId();
    }

    private Long createRole(String name, String key, int sort, String remark) {
        SysRole r = new SysRole();
        r.setName(name);
        r.setRoleKey(key);
        r.setSort(sort);
        r.setStatus(1);
        r.setRemark(remark);
        r.setCreateTime(LocalDateTime.now());
        roleMapper.insert(r);
        return r.getId();
    }

    private Long createUser(String username, String pwd, String nickname, String job,
                            Long deptId, String deptName, int gender) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(pwd);
        u.setNickname(nickname);
        u.setJobTitle(job);
        u.setDeptId(deptId);
        u.setDeptName(deptName);
        u.setGender(gender);
        u.setStatus(1);
        u.setChatStatus(1);
        u.setCreateTime(LocalDateTime.now());
        userMapper.insert(u);
        return u.getId();
    }

    private Long createSession(int type, String name, Long ownerId, String notice) {
        ChatSession s = new ChatSession();
        s.setType(type);
        s.setName(name);
        s.setOwnerId(ownerId);
        s.setNotice(notice);
        s.setCreateTime(LocalDateTime.now());
        s.setLastTime(LocalDateTime.now());
        sessionMapper.insert(s);
        return s.getId();
    }

    private void addMember(Long sessionId, Long userId, int role) {
        ChatSessionMember m = new ChatSessionMember();
        m.setSessionId(sessionId);
        m.setUserId(userId);
        m.setMemberRole(role);
        m.setUnread(0);
        m.setJoinTime(LocalDateTime.now());
        memberMapper.insert(m);
    }

    private Long addMessage(Long sessionId, Long senderId, String content, LocalDateTime time) {
        return addMessage(sessionId, senderId, content, time, null);
    }

    private Long addMessage(Long sessionId, Long senderId, String content, LocalDateTime time, String atUserIds) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setSenderId(senderId);
        msg.setContentType(1);
        msg.setContent(content);
        msg.setAtUserIds(atUserIds);
        msg.setStatus(1);
        msg.setCreateTime(time);
        messageMapper.insert(msg);
        return msg.getId();
    }

    /** 为所有已有消息生成已读回执: 每个会话的成员都"读过"该会话所有非自己发的消息 */
    private void markAllMessagesRead() {
        var allMembers = memberMapper.selectList(null);
        var allMessages = messageMapper.selectList(null);
        for (var msg : allMessages) {
            if (msg.getSenderId() == null || msg.getSenderId() == 0L) continue;
            for (var m : allMembers) {
                if (!m.getSessionId().equals(msg.getSessionId())) continue;
                if (m.getUserId().equals(msg.getSenderId())) continue; // 不给自己标已读
                ChatMessageRead r = new ChatMessageRead();
                r.setMessageId(msg.getId());
                r.setSessionId(msg.getSessionId());
                r.setUserId(m.getUserId());
                r.setReadTime(msg.getCreateTime().plusMinutes(1));
                messageReadMapper.insert(r);
            }
        }
    }

    private void createTodo(String title, String content, Long creatorId, Long assigneeId, int priority, int status) {
        SysTodo t = new SysTodo();
        t.setTitle(title); t.setContent(content);
        t.setCreatorId(creatorId); t.setAssigneeId(assigneeId);
        t.setPriority(priority); t.setStatus(status);
        t.setCreateTime(LocalDateTime.now());
        todoMapper.insert(t);
    }

    private void createFile(String name, String url, Long size, String type, Long uploaderId,
                            String scene, String description, String status, LocalDateTime createTime) {
        SysFile file = new SysFile();
        file.setName(name);
        file.setUrl(url);
        file.setSize(size);
        file.setType(type);
        file.setUploaderId(uploaderId);
        file.setScene(scene);
        file.setDescription(description);
        file.setStatus(status);
        file.setShareCount("已共享".equals(status) ? 3 : ("可下载".equals(status) ? 1 : 0));
        file.setHistoryJson(initialFileHistory(status, createTime));
        file.setCreateTime(createTime);
        file.setUpdateTime(createTime);
        fileMapper.insert(file);
    }

    private String initialFileHistory(String status, LocalDateTime createTime) {
        List<SysFile.HistoryItem> history = new ArrayList<>();
        if ("已共享".equals(status)) {
            history.add(new SysFile.HistoryItem(formatHistoryTime(createTime.plusHours(2)), "已设为共享文档"));
        } else if ("待评审".equals(status)) {
            history.add(new SysFile.HistoryItem(formatHistoryTime(createTime.plusHours(1)), "已提交评审"));
        } else if ("待回收".equals(status)) {
            history.add(new SysFile.HistoryItem(formatHistoryTime(createTime.plusHours(3)), "已标记待回收"));
        } else if ("已锁版".equals(status)) {
            history.add(new SysFile.HistoryItem(formatHistoryTime(createTime.plusHours(4)), "已锁定最终版本"));
        } else if ("可下载".equals(status)) {
            history.add(new SysFile.HistoryItem(formatHistoryTime(createTime.plusHours(2)), "已生成可分发资源包"));
        }
        history.add(new SysFile.HistoryItem(formatHistoryTime(createTime), "已上传到文档中心"));
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < history.size(); i++) {
            SysFile.HistoryItem item = history.get(i);
            if (i > 0) builder.append(',');
            builder.append("{\"time\":\"")
                    .append(item.getTime())
                    .append("\",\"text\":\"")
                    .append(item.getText())
                    .append("\"}");
        }
        builder.append(']');
        return builder.toString();
    }

    private String formatHistoryTime(LocalDateTime time) {
        return String.format("%02d-%02d %02d:%02d",
                time.getMonthValue(),
                time.getDayOfMonth(),
                time.getHour(),
                time.getMinute());
    }

    private void createFavorite(Long userId, Long messageId, Long sessionId, String content, String senderName, LocalDateTime createTime) {
        ChatFavorite favorite = new ChatFavorite();
        favorite.setUserId(userId);
        favorite.setMessageId(messageId);
        favorite.setSessionId(sessionId);
        favorite.setContent(content);
        favorite.setSenderName(senderName);
        favorite.setCreateTime(createTime);
        favoriteMapper.insert(favorite);
    }

    private void createSchedule(String title, String content, Long userId, LocalDateTime startTime,
                                LocalDateTime endTime, int allDay, String color) {
        SysSchedule schedule = new SysSchedule();
        schedule.setTitle(title);
        schedule.setContent(content);
        schedule.setUserId(userId);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setAllDay(allDay);
        schedule.setColor(color);
        schedule.setCreateTime(LocalDateTime.now());
        scheduleMapper.insert(schedule);
    }

    private void updateSessionLast(Long sessionId, String lastMsg, LocalDateTime time) {
        ChatSession s = sessionMapper.selectById(sessionId);
        s.setLastMsg(lastMsg);
        s.setLastTime(time);
        sessionMapper.updateById(s);
    }
}

