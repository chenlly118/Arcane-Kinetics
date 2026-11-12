package dev.createspellbook.client.ponder;

import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;

/**
 * One stable key per tutorial line. Ponder's shared text registry resolves these
 * through {@code create_spellbook.ponder.shared.<key>}, so scenes never depend on
 * the order in which text elements happen to be registered.
 */
public final class PonderTexts {
    // Black Hole Reactor - assembly
    public static final String REACTOR_ASSEMBLY_INTRO = "reactor_assembly_intro";
    public static final String REACTOR_ASSEMBLY_FLOOR = "reactor_assembly_floor";
    public static final String REACTOR_ASSEMBLY_ENERGY_CORE = "reactor_assembly_energy_core";
    public static final String REACTOR_ASSEMBLY_WALLS = "reactor_assembly_walls";
    public static final String REACTOR_ASSEMBLY_CONTROLLER = "reactor_assembly_controller";
    public static final String REACTOR_ASSEMBLY_MANA = "reactor_assembly_mana";
    public static final String REACTOR_ASSEMBLY_STABILIZER = "reactor_assembly_stabilizer";
    public static final String REACTOR_ASSEMBLY_OUTPUT = "reactor_assembly_output";
    public static final String REACTOR_ASSEMBLY_ROOF = "reactor_assembly_roof";
    public static final String REACTOR_ASSEMBLY_REACTION_CORE = "reactor_assembly_reaction_core";
    public static final String REACTOR_ASSEMBLY_SHAFT = "reactor_assembly_shaft";
    public static final String REACTOR_ASSEMBLY_CHECK = "reactor_assembly_check";

    // Black Hole Reactor - operation
    public static final String REACTOR_OPERATION_PANEL = "reactor_operation_panel";
    public static final String REACTOR_OPERATION_MANA = "reactor_operation_mana";
    public static final String REACTOR_OPERATION_PAIRS = "reactor_operation_pairs";
    public static final String REACTOR_OPERATION_STABILIZERS = "reactor_operation_stabilizers";
    public static final String REACTOR_OPERATION_START = "reactor_operation_start";
    public static final String REACTOR_OPERATION_STRESS = "reactor_operation_stress";
    public static final String REACTOR_OPERATION_OUTPUT = "reactor_operation_output";
    public static final String REACTOR_OPERATION_NO_MANA = "reactor_operation_no_mana";
    public static final String REACTOR_OPERATION_STOP = "reactor_operation_stop";

    // Black Hole Reactor - instability
    public static final String REACTOR_SAFETY_TRIGGER = "reactor_safety_trigger";
    public static final String REACTOR_SAFETY_GROWTH = "reactor_safety_growth";
    public static final String REACTOR_SAFETY_THRESHOLD = "reactor_safety_threshold";
    public static final String REACTOR_SAFETY_TIME = "reactor_safety_time";
    public static final String REACTOR_SAFETY_REPAIR = "reactor_safety_repair";
    public static final String REACTOR_SAFETY_RUNAWAY = "reactor_safety_runaway";
    public static final String REACTOR_SAFETY_ADVICE = "reactor_safety_advice";

    // Arcane Casting Arm - setup
    public static final String CASTING_SETUP_POWER = "casting_setup_power";
    public static final String CASTING_SETUP_STRESS = "casting_setup_stress";
    public static final String CASTING_SETUP_MANA = "casting_setup_mana";
    public static final String CASTING_SETUP_SCROLL = "casting_setup_scroll";
    public static final String CASTING_SETUP_TEMPLATE = "casting_setup_template";
    public static final String CASTING_SETUP_MODE = "casting_setup_mode";
    public static final String CASTING_SETUP_PULSE = "casting_setup_pulse";
    public static final String CASTING_SETUP_COST = "casting_setup_cost";
    public static final String CASTING_SETUP_COOLDOWN = "casting_setup_cooldown";

    // Arcane Casting Arm - modes
    public static final String CASTING_MODES_CYCLE = "casting_modes_cycle";
    public static final String CASTING_MODES_PULSE = "casting_modes_pulse";
    public static final String CASTING_MODES_TURRET = "casting_modes_turret";
    public static final String CASTING_MODES_PROCESSING = "casting_modes_processing";
    public static final String CASTING_MODES_RECIPE = "casting_modes_recipe";
    public static final String CASTING_MODES_RESULT = "casting_modes_result";
    public static final String CASTING_MODES_BELT = "casting_modes_belt";
    public static final String CASTING_MODES_LIMITS = "casting_modes_limits";

    // Focusing Basin
    public static final String BASIN_INTRO = "basin_intro";
    public static final String BASIN_DAMAGE = "basin_damage";
    public static final String BASIN_RATE = "basin_rate";
    public static final String BASIN_CAPACITY = "basin_capacity";
    public static final String BASIN_PIPE = "basin_pipe";
    public static final String BASIN_STORAGE = "basin_storage";
    public static final String BASIN_READ = "basin_read";
    public static final String BASIN_NOTE = "basin_note";

    // Arcane Depot
    public static final String DEPOT_INTRO = "depot_intro";
    public static final String DEPOT_PLACE = "depot_place";
    public static final String DEPOT_ARM = "depot_arm";
    public static final String DEPOT_RECIPE = "depot_recipe";
    public static final String DEPOT_EXAMPLE = "depot_example";
    public static final String DEPOT_RESULT = "depot_result";
    public static final String DEPOT_AUTOMATION = "depot_automation";
    public static final String DEPOT_VANILLA = "depot_vanilla";

    // Scroll Printer
    public static final String PRINTER_SETUP = "printer_setup";
    public static final String PRINTER_NO_POWER = "printer_no_power";
    public static final String PRINTER_TEMPLATE = "printer_template";
    public static final String PRINTER_FOCUS = "printer_focus";
    public static final String PRINTER_INK = "printer_ink";
    public static final String PRINTER_PAPER = "printer_paper";
    public static final String PRINTER_CYCLE = "printer_cycle";
    public static final String PRINTER_FILTER = "printer_filter";
    public static final String PRINTER_NOTE = "printer_note";

    // Arcane Auto Caster
    public static final String CASTER_NO_POWER = "caster_no_power";
    public static final String CASTER_SCROLL = "caster_scroll";
    public static final String CASTER_MANA = "caster_mana";
    public static final String CASTER_REGEN = "caster_regen";
    public static final String CASTER_AUTO = "caster_auto";
    public static final String CASTER_COMBAT = "caster_combat";
    public static final String CASTER_LIMITS = "caster_limits";
    public static final String CASTER_NOTE = "caster_note";

    // Reactor accident demonstration
    public static final String REACTOR_ACCIDENT_SETUP = "reactor_accident_setup";
    public static final String REACTOR_ACCIDENT_GROWTH = "reactor_accident_growth";
    public static final String REACTOR_ACCIDENT_ESCAPE = "reactor_accident_escape";
    public static final String REACTOR_ACCIDENT_EAT = "reactor_accident_eat";
    public static final String REACTOR_ACCIDENT_REPAIR = "reactor_accident_repair";
    public static final String REACTOR_ACCIDENT_DEMO = "reactor_accident_demo";
    public static final String REACTOR_ACCIDENT_ADVICE = "reactor_accident_advice";

    private PonderTexts() {}

    public static void register(SharedTextRegistrationHelper helper) {
        // Assembly
        add(helper, REACTOR_ASSEMBLY_INTRO, "黑洞反应器由奥术机壳围成立方体，边长可以是 5 到 19 格。");
        add(helper, REACTOR_ASSEMBLY_FLOOR, "先铺底层：十二条棱边使用奥术机壳，能量核心放在底面中央。");
        add(helper, REACTOR_ASSEMBLY_ENERGY_CORE, "能量核心放在底面中央，或紧贴底面内侧。");
        add(helper, REACTOR_ASSEMBLY_WALLS, "逐层围出四壁，非棱边可以换成强化玻璃。");
        add(helper, REACTOR_ASSEMBLY_CONTROLLER, "控制器装在侧面并朝外；同层留出法力、稳定质和输出接口。");
        add(helper, REACTOR_ASSEMBLY_MANA, "法力接口负责输入液态法力，多个接口共享同一储罐。");
        add(helper, REACTOR_ASSEMBLY_STABILIZER, "稳定质端口只接受黑洞稳定质，燃料会从这里补进反应器。");
        add(helper, REACTOR_ASSEMBLY_OUTPUT, "输出口只导出坍缩物质，接上漏斗或物流就能取走。");
        add(helper, REACTOR_ASSEMBLY_ROOF, "继续向上封顶。");
        add(helper, REACTOR_ASSEMBLY_REACTION_CORE, "封顶后，把反应核心装在顶面中央，与底面能量核心配对。");
        add(helper, REACTOR_ASSEMBLY_SHAFT, "侧面装一个反应轴，轴口朝外，用马达或动力网络驱动。");
        add(helper, REACTOR_ASSEMBLY_CHECK, "接好外部法力与动力后，空手右键控制器检测结构并打开面板。");

        // Operation
        add(helper, REACTOR_OPERATION_PANEL, "结构成立后，右键控制器打开面板。");
        add(helper, REACTOR_OPERATION_MANA, "液态法力从法力接口泵入，整座反应器共用一份储量。");
        add(helper, REACTOR_OPERATION_PAIRS, "每对核心每秒消耗 100 mB 法力和 1 点稳定质耐久。");
        add(helper, REACTOR_OPERATION_STABILIZERS, "每个核心对需要一件黑洞稳定质，每秒消耗 1 点耐久。");
        add(helper, REACTOR_OPERATION_START, "点“启动”，黑洞逐渐增长到配置的稳定尺寸，默认是 80%%。");
        add(helper, REACTOR_OPERATION_STRESS, "默认 80%% 时每对核心提供 327,680 SU，反应轴默认以 256 RPM 输出。");
        add(helper, REACTOR_OPERATION_OUTPUT, "坍缩物质从输出口导出，缓存上限为 1024 个。");
        add(helper, REACTOR_OPERATION_NO_MANA, "法力耗尽时，黑洞会平滑收缩。");
        add(helper, REACTOR_OPERATION_STOP, "点“停止供能”，黑洞平滑收缩；法力耗尽时同样收缩。");

        // Safety
        add(helper, REACTOR_SAFETY_TRIGGER, "稳定质耗尽、废料满仓或外壳破损时，黑洞开始失稳膨胀。");
        add(helper, REACTOR_SAFETY_GROWTH, "失稳后每 5 Tick 增长 0.1%%；在 100%% 前修复就能恢复。");
        add(helper, REACTOR_SAFETY_THRESHOLD, "100%% 仍在约束内；超过 100%% 才会真正失控。");
        add(helper, REACTOR_SAFETY_TIME, "从默认的 80%% 开始失稳，大约 50 秒后会超过 100%%。");
        add(helper, REACTOR_SAFETY_REPAIR, "修复结构、补足稳定质并清空废料，黑洞会停止膨胀。");
        add(helper, REACTOR_SAFETY_RUNAWAY, "一旦失控，黑洞会吞噬周围的方块和生物，反应器将失去控制。");
        add(helper, REACTOR_SAFETY_ADVICE, "把稳定质与废料空间纳入日常维护，让反应器长期稳定。");

        // Casting arm setup
        add(helper, CASTING_SETUP_POWER, "从底部接入旋转动力，并保持足够转速与应力。");
        add(helper, CASTING_SETUP_STRESS, "转速过低或应力超载会中断施法。");
        add(helper, CASTING_SETUP_MANA, "侧面输入液态法力，内部储罐默认 1000 mB。");
        add(helper, CASTING_SETUP_SCROLL, "手持法术卷轴右键装入，空手右键取回；扳手切换三种模式。");
        add(helper, CASTING_SETUP_TEMPLATE, "装入会消耗一张卷轴；之后的施法使用这张模板。");
        add(helper, CASTING_SETUP_MODE, "用 Create 扳手右键，可以在三种模式之间切换。");
        add(helper, CASTING_SETUP_PULSE, "脉冲模式只在红石信号上升沿施放一次。");
        add(helper, CASTING_SETUP_COST, "施法按卷轴魔耗扣除液态法力，默认 1 点魔耗对应 1 mB。");
        add(helper, CASTING_SETUP_COOLDOWN, "冷却按法术分别记录，换卷轴后仍要等待。");

        // Casting arm modes
        add(helper, CASTING_MODES_CYCLE, "扳手右键依次切换：红石脉冲、自动炮台、奥术加工。");
        add(helper, CASTING_MODES_PULSE, "脉冲模式朝正前方施放，适合直接触发。");
        add(helper, CASTING_MODES_TURRET, "炮台模式自动锁定 16 格内可见的敌对生物。");
        add(helper, CASTING_MODES_PROCESSING, "加工模式向正前方 2 到 16 格的置物台施法，物品匹配配方后产出成品。");
        add(helper, CASTING_MODES_RECIPE, "台上的物品必须满足这条法术对应的加工配方。");
        add(helper, CASTING_MODES_RESULT, "命中后输入被消耗，成品留在置物台的输出缓存里。");
        add(helper, CASTING_MODES_BELT, "传送带也能作为目标，但速度太快时法术会脱靶。");
        add(helper, CASTING_MODES_LIMITS, "三种模式都需要动力和法力；切换模式后冷却继续计时。");

        // Focusing basin
        add(helper, BASIN_INTRO, "聚魔盆会把命中它的攻击法术转化为液态法力。");
        add(helper, BASIN_DAMAGE, "只有造成伤害的法术才会产出法力。");
        add(helper, BASIN_RATE, "造成伤害的法术命中后，每点伤害转化为 10 mB 法力。");
        add(helper, BASIN_CAPACITY, "默认容量 2000 mB；装满后继续命中会溢出。");
        add(helper, BASIN_PIPE, "每一面都能接 Create 管道，用动力泵把法力送入储罐。");
        add(helper, BASIN_STORAGE, "用动力泵把法力送进储罐，或直接用桶取用。");
        add(helper, BASIN_READ, "空手右键可以查看当前储量和容量。");
        add(helper, BASIN_NOTE, "聚魔盆负责收集法力，目标由施法装置提供。");

        // Arcane depot
        add(helper, DEPOT_INTRO, "把待加工物品放在台面上，魔化置物台沿用 Create 置物台的物流行为。");
        add(helper, DEPOT_PLACE, "把要加工的原料放在台面上。");
        add(helper, DEPOT_ARM, "把奥术施法装置放在正前方 2 格以外，并保持视线。");
        add(helper, DEPOT_RECIPE, "法术与原料匹配一条法术加工配方。");
        add(helper, DEPOT_EXAMPLE, "例如紫水晶碎片被邪术冲击波命中，会变成回响碎片。");
        add(helper, DEPOT_RESULT, "命中后原料被消耗，成品进入置物台的输出缓存。");
        add(helper, DEPOT_AUTOMATION, "漏斗、机械臂或传送带可以继续取走成品。");
        add(helper, DEPOT_VANILLA, "普通 Create 置物台也能执行同样的法术加工。");

        // Scroll printer
        add(helper, PRINTER_SETUP, "点击置物台或传送带顶部安装印刷机，中间留一格供印刷头下压。");
        add(helper, PRINTER_NO_POWER, "它依靠墨水、模板和学派材料工作。");
        add(helper, PRINTER_TEMPLATE, "侧面装入法术卷轴作模板，另一侧放对应学派材料；模板可以重复使用。");
        add(helper, PRINTER_FOCUS, "另一侧放入对应学派的材料，例如烈焰棒对应火焰学派。");
        add(helper, PRINTER_INK, "顶部输入液态墨水，墨水品质决定印出的卷轴等级。");
        add(helper, PRINTER_PAPER, "把纸放在下方的置物台或传送带上。");
        add(helper, PRINTER_CYCLE, "印刷消耗纸和墨水。模板和学派材料可以重复使用。");
        add(helper, PRINTER_FILTER, "白名单过滤器可以放多张卷轴，印刷时轮流选择。");
        add(helper, PRINTER_NOTE, "印刷头下方必须留空；被方块挡住时会停止。");

        // Arcane auto caster
        add(helper, CASTER_NO_POWER, "奥术反应装置从大气中收集法力，内部储罐默认 1000 mB。");
        add(helper, CASTER_SCROLL, "持法术卷轴右键装入；空手右键取回。");
        add(helper, CASTER_MANA, "背面输入液态法力，内部储罐默认 1000 mB，并会缓慢回充。");
        add(helper, CASTER_REGEN, "已加载时每个 Tick 恢复 1 mB，装满后停止。");
        add(helper, CASTER_AUTO, "每 10 Tick 搜索 16 格内可见的敌对生物。");
        add(helper, CASTER_COMBAT, "找到目标后，按卷轴的魔耗、吟唱和冷却正常施法。");
        add(helper, CASTER_LIMITS, "装置专注攻击敌对生物，也接受管道补充的法力。");
        add(helper, CASTER_NOTE, "装置专注战斗，聚魔盆、加工台和感应器各自独立工作。");

        // Reactor accident demonstration
        add(helper, REACTOR_ACCIDENT_SETUP, "稳定质不足、废料满仓或结构破损时，反应器开始失稳。");
        add(helper, REACTOR_ACCIDENT_GROWTH, "黑洞尺寸每 5 Tick 增加 0.1%，应力也随尺寸上升。");
        add(helper, REACTOR_ACCIDENT_ESCAPE, "超过 100% 后，黑洞脱离约束，反应器将失去控制。");
        add(helper, REACTOR_ACCIDENT_EAT, "失控的黑洞会吞噬周围的方块、掉落物和生物。");
        add(helper, REACTOR_ACCIDENT_REPAIR, "越线前修复结构、补足稳定质并清空废料，仍能解除失稳。");
        add(helper, REACTOR_ACCIDENT_DEMO, "本段演示警报与视效，存档保持原样。");
        add(helper, REACTOR_ACCIDENT_ADVICE, "把稳定质和废料空间当成日常维护，而不是事故后的补救。");
    }

    private static void add(SharedTextRegistrationHelper helper, String key, String chinese) {
        helper.registerSharedText(key, chinese);
    }
}
