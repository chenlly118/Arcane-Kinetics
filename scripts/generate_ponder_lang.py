"""Add the Ponder scene and shared-text headings to both language files.

Keeping this script in-tree makes the long language table easy to review and
regenerate. Existing keys are preserved.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = ROOT / 'src/main/resources/assets/create_spellbook/lang'

ZH = {
    # Headers
    'create_spellbook.ponder.black_hole_reactor_assembly.header': '黑洞反应器：搭建',
    'create_spellbook.ponder.black_hole_reactor_operation.header': '黑洞反应器：运行',
    'create_spellbook.ponder.black_hole_reactor_safety.header': '黑洞反应器：失稳与失控',
    'create_spellbook.ponder.black_hole_reactor_accident.header': '黑洞反应器：事故演示',
    'create_spellbook.ponder.arcane_casting_arm_setup.header': '奥术施法装置：配置',
    'create_spellbook.ponder.arcane_casting_arm_modes.header': '奥术施法装置：三种模式',
    'create_spellbook.ponder.focusing_basin.header': '聚魔盆：收集液态法力',
    'create_spellbook.ponder.arcane_depot.header': '魔化置物台：法术加工',
    'create_spellbook.ponder.scroll_printer.header': '卷轴印刷机：纸张变卷轴',
    'create_spellbook.ponder.arcane_reactor.header': '奥术反应装置：自动施法',
    'create_spellbook.ponder.tag.arcane_engineering': '奥术动力学',
    'create_spellbook.ponder.tag.arcane_engineering.description': '把法术接入 Create 的动力、物流与流体网络。',

    # Shared text
    'create_spellbook.ponder.shared.reactor_assembly_intro': '黑洞反应器由奥术机壳围成立方体，边长可以是 5 到 19 格。',
    'create_spellbook.ponder.shared.reactor_assembly_floor': '先铺底层。十二条棱边必须全部使用奥术机壳。',
    'create_spellbook.ponder.shared.reactor_assembly_energy_core': '能量核心放在底面中央，或紧贴底面内侧。',
    'create_spellbook.ponder.shared.reactor_assembly_walls': '再逐层围出四壁；非棱边可以换成强化玻璃，方便观察内部。',
    'create_spellbook.ponder.shared.reactor_assembly_controller': '控制器装在侧面，正面朝外，整座结构只能有一个。',
    'create_spellbook.ponder.shared.reactor_assembly_mana': '法力接口负责输入液态法力，多个接口共享同一储罐。',
    'create_spellbook.ponder.shared.reactor_assembly_stabilizer': '稳定质端口只接受黑洞稳定质，燃料会从这里补进反应器。',
    'create_spellbook.ponder.shared.reactor_assembly_output': '输出口只导出坍缩物质，接上漏斗或物流就能取走。',
    'create_spellbook.ponder.shared.reactor_assembly_roof': '继续向上封顶。',
    'create_spellbook.ponder.shared.reactor_assembly_reaction_core': '反应核心装在顶面中央，或紧贴顶面内侧；它与能量核心成对工作。',
    'create_spellbook.ponder.shared.reactor_assembly_shaft': '侧面只装一个反应轴，轴口朝外，接 Create 动力网络。',
    'create_spellbook.ponder.shared.reactor_assembly_check': '空手右键控制器打开面板；Shift 右键可以重新检测结构。',

    'create_spellbook.ponder.shared.reactor_operation_panel': '结构成立后，右键控制器打开面板。',
    'create_spellbook.ponder.shared.reactor_operation_mana': '液态法力从法力接口泵入，整座反应器共用一份储量。',
    'create_spellbook.ponder.shared.reactor_operation_pairs': '每对核心默认每秒消耗 100 mB 法力。',
    'create_spellbook.ponder.shared.reactor_operation_stabilizers': '每个核心对需要一件黑洞稳定质，每秒消耗 1 点耐久。',
    'create_spellbook.ponder.shared.reactor_operation_start': '点“启动”，黑洞会从零平滑升到 50%% 尺寸。',
    'create_spellbook.ponder.shared.reactor_operation_stress': '50%% 时每对核心提供 204,800 SU 应力，反应轴默认以 256 RPM 输出。',
    'create_spellbook.ponder.shared.reactor_operation_output': '坍缩物质从输出口导出，共享缓存上限是 1024 个。',
    'create_spellbook.ponder.shared.reactor_operation_no_mana': '法力不足只会让黑洞收缩，不会引发事故。',
    'create_spellbook.ponder.shared.reactor_operation_stop': '点“停止供能”，尺寸会平滑收缩到零。',

    'create_spellbook.ponder.shared.reactor_safety_trigger': '稳定质不足、废料满仓或结构破损，都会触发失稳。',
    'create_spellbook.ponder.shared.reactor_safety_growth': '失稳后尺寸每 5 Tick 上涨 0.1%%，应力也跟着上涨。',
    'create_spellbook.ponder.shared.reactor_safety_threshold': '100%% 仍在约束内；超过 100%% 才会真正失控。',
    'create_spellbook.ponder.shared.reactor_safety_time': '从正常的 50%% 开始，大约 125 秒后越过这条线。',
    'create_spellbook.ponder.shared.reactor_safety_repair': '在越线前修好结构、补足稳定质并清空废料，就能解除失稳。',
    'create_spellbook.ponder.shared.reactor_safety_runaway': '一旦失控，黑洞会吞噬周围的方块和生物，反应器无法再关掉它。',
    'create_spellbook.ponder.shared.reactor_safety_advice': '保持燃料与废料空间充足，比事后补救可靠得多。',

    'create_spellbook.ponder.shared.casting_setup_power': '奥术施法装置从底部接入旋转动力。',
    'create_spellbook.ponder.shared.casting_setup_stress': '转速过低或应力超载时，它不会开始施法。',
    'create_spellbook.ponder.shared.casting_setup_mana': '右侧输入液态法力，内部储罐默认 1000 mB。',
    'create_spellbook.ponder.shared.casting_setup_scroll': '手持法术卷轴右键装入，空手右键取回。',
    'create_spellbook.ponder.shared.casting_setup_template': '装入会消耗一张卷轴；之后施法不再消耗它。',
    'create_spellbook.ponder.shared.casting_setup_mode': '用 Create 扳手右键，可以在三种模式之间切换。',
    'create_spellbook.ponder.shared.casting_setup_pulse': '脉冲模式只在红石信号上升沿施放一次。',
    'create_spellbook.ponder.shared.casting_setup_cost': '施法按卷轴魔耗扣除液态法力，默认 1 点魔耗对应 1 mB。',
    'create_spellbook.ponder.shared.casting_setup_cooldown': '冷却按法术分别记录，换卷轴也不能跳过。',

    'create_spellbook.ponder.shared.casting_modes_cycle': '扳手右键依次切换：红石脉冲、自动炮台、奥术加工。',
    'create_spellbook.ponder.shared.casting_modes_pulse': '脉冲模式朝正前方施放，不需要目标。',
    'create_spellbook.ponder.shared.casting_modes_turret': '炮台模式自动锁定 16 格内可见的敌对生物。',
    'create_spellbook.ponder.shared.casting_modes_processing': '加工模式朝正前方 2 到 16 格、同一高度或低一格的置物台施法。',
    'create_spellbook.ponder.shared.casting_modes_recipe': '台上的物品必须满足这条法术对应的加工配方。',
    'create_spellbook.ponder.shared.casting_modes_result': '命中后输入被消耗，成品留在置物台的输出缓存里。',
    'create_spellbook.ponder.shared.casting_modes_belt': '传送带也能作为目标，但速度太快时法术会脱靶。',
    'create_spellbook.ponder.shared.casting_modes_limits': '三种模式都需要动力和法力；切换模式不会重置冷却。',

    'create_spellbook.ponder.shared.basin_intro': '聚魔盆会把命中它的攻击法术转化为液态法力。',
    'create_spellbook.ponder.shared.basin_damage': '只有造成伤害的法术才会产出法力。',
    'create_spellbook.ponder.shared.basin_rate': '默认每点法术伤害转化为 10 mB 法力。',
    'create_spellbook.ponder.shared.basin_capacity': '默认容量 2000 mB；装满后多余的伤害会被丢弃。',
    'create_spellbook.ponder.shared.basin_pipe': '它的每一面都能接 Create 流体管道。',
    'create_spellbook.ponder.shared.basin_storage': '用动力泵把法力送进储罐，或直接用桶取用。',
    'create_spellbook.ponder.shared.basin_read': '空手右键可以查看当前储量和容量。',
    'create_spellbook.ponder.shared.basin_note': '它只负责收集，不会自己寻找目标施法。',

    'create_spellbook.ponder.shared.depot_intro': '魔化置物台沿用 Create 置物台的物品、物流和比较器行为。',
    'create_spellbook.ponder.shared.depot_place': '把要加工的原料放在台面上。',
    'create_spellbook.ponder.shared.depot_arm': '把奥术施法装置放在正前方 2 格以外，并保持视线。',
    'create_spellbook.ponder.shared.depot_recipe': '法术与原料必须匹配一条法术加工配方。',
    'create_spellbook.ponder.shared.depot_example': '例如紫水晶碎片被邪术冲击波命中，会变成回响碎片。',
    'create_spellbook.ponder.shared.depot_result': '命中后原料被消耗，成品进入置物台的输出缓存。',
    'create_spellbook.ponder.shared.depot_automation': '漏斗、机械臂或传送带可以继续取走成品。',
    'create_spellbook.ponder.shared.depot_vanilla': '普通 Create 置物台也能执行同样的法术加工。',

    'create_spellbook.ponder.shared.printer_setup': '卷轴印刷机要放在置物台或传送带上方两格，中间留空。',
    'create_spellbook.ponder.shared.printer_no_power': '它不需要旋转动力，只需要墨水、模板和学派材料。',
    'create_spellbook.ponder.shared.printer_template': '侧面装入一张法术卷轴作模板；模板不会被消耗。',
    'create_spellbook.ponder.shared.printer_focus': '另一侧放入对应学派的材料，例如烈焰棒对应火焰学派。',
    'create_spellbook.ponder.shared.printer_ink': '顶部输入液态墨水，墨水品质决定印出的卷轴等级。',
    'create_spellbook.ponder.shared.printer_paper': '把纸放在下方的置物台或传送带上。',
    'create_spellbook.ponder.shared.printer_cycle': '每张纸消耗一份墨水和一份学派材料，成品落到下方。',
    'create_spellbook.ponder.shared.printer_filter': '白名单过滤器可以放多张卷轴，印刷时轮流选择。',
    'create_spellbook.ponder.shared.printer_note': '印刷头下方必须留空；被方块挡住时会停止。',

    'create_spellbook.ponder.shared.caster_no_power': '奥术反应装置不需要旋转动力，也不吃应力。',
    'create_spellbook.ponder.shared.caster_scroll': '持法术卷轴右键装入；空手右键取回。',
    'create_spellbook.ponder.shared.caster_mana': '背面只能输入液态法力，内部储罐默认 1000 mB。',
    'create_spellbook.ponder.shared.caster_regen': '已加载时每个 Tick 恢复 1 mB，装满后停止。',
    'create_spellbook.ponder.shared.caster_auto': '它每 10 Tick 搜索 16 格内可见的敌对生物。',
    'create_spellbook.ponder.shared.caster_combat': '找到目标后，按卷轴的魔耗、吟唱和冷却正常施法。',
    'create_spellbook.ponder.shared.caster_limits': '它不会攻击玩家、宠物或友方，也不会把法力送回管道。',
    'create_spellbook.ponder.shared.caster_note': '装置只负责战斗，不会为聚魔盆、加工台或感应器供能。',

    'create_spellbook.ponder.shared.reactor_accident_setup': '稳定质不足、废料满仓或结构破损时，反应器开始失稳。',
    'create_spellbook.ponder.shared.reactor_accident_growth': '黑洞尺寸每 5 Tick 增加 0.1%%，应力也随尺寸上升。',
    'create_spellbook.ponder.shared.reactor_accident_escape': '超过 100%% 后，黑洞脱离约束，反应器无法再关闭它。',
    'create_spellbook.ponder.shared.reactor_accident_eat': '失控的黑洞会吞噬周围的方块、掉落物和生物。',
    'create_spellbook.ponder.shared.reactor_accident_repair': '越线前修复结构、补足稳定质并清空废料，仍能解除失稳。',
    'create_spellbook.ponder.shared.reactor_accident_demo': '本段只演示警报与视效，不会在存档中生成真实事故。',
    'create_spellbook.ponder.shared.reactor_accident_advice': '把稳定质和废料空间当成日常维护，而不是事故后的补救。',
}

EN = {
    'create_spellbook.ponder.black_hole_reactor_assembly.header': 'Black Hole Reactor: Assembly',
    'create_spellbook.ponder.black_hole_reactor_operation.header': 'Black Hole Reactor: Operation',
    'create_spellbook.ponder.black_hole_reactor_safety.header': 'Black Hole Reactor: Instability',
    'create_spellbook.ponder.black_hole_reactor_accident.header': 'Black Hole Reactor: Accident',
    'create_spellbook.ponder.arcane_casting_arm_setup.header': 'Arcane Casting Arm: Setup',
    'create_spellbook.ponder.arcane_casting_arm_modes.header': 'Arcane Casting Arm: Three Modes',
    'create_spellbook.ponder.focusing_basin.header': 'Focusing Basin: Collecting Mana',
    'create_spellbook.ponder.arcane_depot.header': 'Arcane Depot: Spell Processing',
    'create_spellbook.ponder.scroll_printer.header': 'Scroll Printer: Paper into Scrolls',
    'create_spellbook.ponder.arcane_reactor.header': 'Arcane Auto Caster: Automatic Casting',
    'create_spellbook.ponder.tag.arcane_engineering': 'Arcane Kinetics',
    'create_spellbook.ponder.tag.arcane_engineering.description': 'Spells wired into Create kinetics, logistics and fluid networks.',

    'create_spellbook.ponder.shared.reactor_assembly_intro': 'A Black Hole Reactor is a cube of Arcane Casing, 5 to 19 blocks on each side.',
    'create_spellbook.ponder.shared.reactor_assembly_floor': 'Start with the floor. All twelve edges must be Arcane Casing.',
    'create_spellbook.ponder.shared.reactor_assembly_energy_core': 'Place the Energy Core in the centre of the floor, or directly above it.',
    'create_spellbook.ponder.shared.reactor_assembly_walls': 'Build the walls layer by layer. Non-edge panels can be Reinforced Arcane Glass.',
    'create_spellbook.ponder.shared.reactor_assembly_controller': 'Mount one Controller on a side face, facing outwards.',
    'create_spellbook.ponder.shared.reactor_assembly_mana': 'The Mana Interface accepts liquid mana. Multiple interfaces share one tank.',
    'create_spellbook.ponder.shared.reactor_assembly_stabilizer': 'The Stabilizer Port only accepts Black Hole Stabilizers.',
    'create_spellbook.ponder.shared.reactor_assembly_output': 'The Output Port only exports Collapsed Matter.',
    'create_spellbook.ponder.shared.reactor_assembly_roof': 'Continue upwards and close the roof.',
    'create_spellbook.ponder.shared.reactor_assembly_reaction_core': 'The Reaction Core sits in the centre of the roof, or just below it. It pairs with the Energy Core.',
    'create_spellbook.ponder.shared.reactor_assembly_shaft': 'Mount exactly one Reaction Shaft on a side face, pointing into your kinetic network.',
    'create_spellbook.ponder.shared.reactor_assembly_check': 'Right-click the Controller to open its panel. Sneak-right-click to re-check the structure.',

    'create_spellbook.ponder.shared.reactor_operation_panel': 'Once the structure is formed, right-click the Controller.',
    'create_spellbook.ponder.shared.reactor_operation_mana': 'Liquid mana is pumped into the Mana Interface. The whole reactor shares one tank.',
    'create_spellbook.ponder.shared.reactor_operation_pairs': 'Each core pair consumes 100 mB of mana per second by default.',
    'create_spellbook.ponder.shared.reactor_operation_stabilizers': 'Each core pair needs one Black Hole Stabilizer and wears 1 durability per second.',
    'create_spellbook.ponder.shared.reactor_operation_start': 'Press Start. The field grows smoothly from 0%% to 50%%.',
    'create_spellbook.ponder.shared.reactor_operation_stress': 'At 50%%, each pair provides 204,800 SU; the Reaction Shaft outputs 256 RPM by default.',
    'create_spellbook.ponder.shared.reactor_operation_output': 'Collapsed Matter leaves through the Output Port. The shared buffer holds 1,024 items.',
    'create_spellbook.ponder.shared.reactor_operation_no_mana': 'Running out of mana only shrinks the field. It does not cause an accident.',
    'create_spellbook.ponder.shared.reactor_operation_stop': 'Press Stop to shrink the field smoothly back to zero.',

    'create_spellbook.ponder.shared.reactor_safety_trigger': 'Missing stabilizers, a full waste buffer, or a broken structure starts instability.',
    'create_spellbook.ponder.shared.reactor_safety_growth': 'While unstable, the field grows by 0.1%% every 5 ticks, raising stress too.',
    'create_spellbook.ponder.shared.reactor_safety_threshold': '100%% is still contained. Only above 100%% does it escape.',
    'create_spellbook.ponder.shared.reactor_safety_time': 'From a healthy 50%%, that takes about 125 seconds.',
    'create_spellbook.ponder.shared.reactor_safety_repair': 'Repair the structure, refill the stabilizers and clear the waste before then to recover.',
    'create_spellbook.ponder.shared.reactor_safety_runaway': 'Once it escapes, the black hole devours blocks and creatures, and the reactor cannot stop it.',
    'create_spellbook.ponder.shared.reactor_safety_advice': 'Keep fuel and waste space topped up; prevention is far more reliable than repair.',

    'create_spellbook.ponder.shared.casting_setup_power': 'The Arcane Casting Arm takes rotation from a shaft below it.',
    'create_spellbook.ponder.shared.casting_setup_stress': 'It will not start a cast when rotation is too low or the network is overstressed.',
    'create_spellbook.ponder.shared.casting_setup_mana': 'Feed liquid mana into its right side. The internal tank holds 1,000 mB by default.',
    'create_spellbook.ponder.shared.casting_setup_scroll': 'Right-click with a spell scroll to insert it; right-click empty-handed to take it back.',
    'create_spellbook.ponder.shared.casting_setup_template': 'Inserting consumes one scroll. Casting does not consume the template.',
    'create_spellbook.ponder.shared.casting_setup_mode': 'Use a Create wrench to cycle its three modes.',
    'create_spellbook.ponder.shared.casting_setup_pulse': 'Pulse mode casts once on each redstone rising edge.',
    'create_spellbook.ponder.shared.casting_setup_cost': "Casts spend liquid mana equal to the scroll's mana cost; 1 mana is 1 mB by default.",
    'create_spellbook.ponder.shared.casting_setup_cooldown': 'Cooldowns are tracked per spell. Swapping scrolls does not skip them.',

    'create_spellbook.ponder.shared.casting_modes_cycle': 'The wrench cycles Redstone Pulse, Auto Turret, and Arcane Processing.',
    'create_spellbook.ponder.shared.casting_modes_pulse': 'Pulse mode casts straight ahead and needs no target.',
    'create_spellbook.ponder.shared.casting_modes_turret': 'Turret mode locks onto hostile creatures within 16 blocks that it can see.',
    'create_spellbook.ponder.shared.casting_modes_processing': 'Processing mode casts at a depot 2–16 blocks ahead, on the same level or one below.',
    'create_spellbook.ponder.shared.casting_modes_recipe': 'The item on the depot must match a spell processing recipe.',
    'create_spellbook.ponder.shared.casting_modes_result': "On a hit the input is consumed and the result enters the depot's output buffer.",
    'create_spellbook.ponder.shared.casting_modes_belt': "Belts can be targets too, but a fast belt may carry the item out of the spell's path.",
    'create_spellbook.ponder.shared.casting_modes_limits': 'All three modes need rotation and mana. Switching modes does not reset cooldowns.',

    'create_spellbook.ponder.shared.basin_intro': 'The Focusing Basin turns damaging spells that hit it into liquid mana.',
    'create_spellbook.ponder.shared.basin_damage': 'Only spells that deal damage produce mana.',
    'create_spellbook.ponder.shared.basin_rate': 'By default, each point of spell damage becomes 10 mB of mana.',
    'create_spellbook.ponder.shared.basin_capacity': 'Its default capacity is 2,000 mB. Once full, extra damage is discarded.',
    'create_spellbook.ponder.shared.basin_pipe': 'Every side can connect to Create fluid pipes.',
    'create_spellbook.ponder.shared.basin_storage': 'Pump the mana into a tank, or fill a bucket directly.',
    'create_spellbook.ponder.shared.basin_read': 'Right-click empty-handed to check the stored amount.',
    'create_spellbook.ponder.shared.basin_note': 'It only collects; it never casts spells on its own.',

    'create_spellbook.ponder.shared.depot_intro': 'The Arcane Depot keeps the item handling, logistics and comparator behaviour of a Create Depot.',
    'create_spellbook.ponder.shared.depot_place': 'Place the ingredient you want to process on the depot.',
    'create_spellbook.ponder.shared.depot_arm': 'Put an Arcane Casting Arm at least 2 blocks in front, with a clear line of sight.',
    'create_spellbook.ponder.shared.depot_recipe': 'The spell and the ingredient must match a spell processing recipe.',
    'create_spellbook.ponder.shared.depot_example': 'For example, Eldritch Blast turns an Amethyst Shard into an Echo Shard.',
    'create_spellbook.ponder.shared.depot_result': "On a hit the ingredient is consumed and the result enters the depot's output buffer.",
    'create_spellbook.ponder.shared.depot_automation': 'Funnels, mechanical arms or belts can take the result away.',
    'create_spellbook.ponder.shared.depot_vanilla': 'A regular Create Depot can run the same spell processing.',

    'create_spellbook.ponder.shared.printer_setup': 'Place the Scroll Printer two blocks above a depot or belt, leaving the gap empty.',
    'create_spellbook.ponder.shared.printer_no_power': 'It needs no rotation: only ink, a template and a school focus.',
    'create_spellbook.ponder.shared.printer_template': 'Insert a spell scroll as the template; the template is not consumed.',
    'create_spellbook.ponder.shared.printer_focus': 'Put the matching school focus on the other side, such as a Blaze Rod for Fire.',
    'create_spellbook.ponder.shared.printer_ink': 'Feed liquid ink into the top; the ink quality sets the printed scroll level.',
    'create_spellbook.ponder.shared.printer_paper': 'Place paper on the depot or belt below.',
    'create_spellbook.ponder.shared.printer_cycle': 'Each sheet uses one ink portion and one focus, and the result drops onto the depot.',
    'create_spellbook.ponder.shared.printer_filter': 'A whitelist filter can hold several scrolls and cycles through them.',
    'create_spellbook.ponder.shared.printer_note': 'The space under the print head must stay empty; an obstruction stops printing.',

    'create_spellbook.ponder.shared.caster_no_power': 'The Arcane Auto Caster needs no rotation and no stress.',
    'create_spellbook.ponder.shared.caster_scroll': 'Right-click with a spell scroll to insert it; right-click empty-handed to take it back.',
    'create_spellbook.ponder.shared.caster_mana': 'The rear face only accepts liquid mana; the internal tank holds 1,000 mB by default.',
    'create_spellbook.ponder.shared.caster_regen': 'While loaded it recovers 1 mB per tick, stopping when full.',
    'create_spellbook.ponder.shared.caster_auto': 'Every 10 ticks it looks for a visible hostile creature within 16 blocks.',
    'create_spellbook.ponder.shared.caster_combat': "Once it finds a target, it casts with the scroll's normal mana, cast time and cooldown.",
    'create_spellbook.ponder.shared.caster_limits': 'It never attacks players, pets or allies, and cannot drain its mana back into pipes.',
    'create_spellbook.ponder.shared.caster_note': 'It only fights; it does not fuel the basin, processing devices or sensor.',

    'create_spellbook.ponder.shared.reactor_accident_setup': 'Missing stabilizers, a full waste buffer or a broken structure starts instability.',
    'create_spellbook.ponder.shared.reactor_accident_growth': 'The field grows by 0.1%% every 5 ticks, and stress rises with it.',
    'create_spellbook.ponder.shared.reactor_accident_escape': 'Above 100%%, the black hole escapes and the reactor can no longer shut it down.',
    'create_spellbook.ponder.shared.reactor_accident_eat': 'An escaped black hole devours nearby blocks, dropped items and creatures.',
    'create_spellbook.ponder.shared.reactor_accident_repair': 'Before that point, repair the structure, refill stabilizers and clear waste to recover.',
    'create_spellbook.ponder.shared.reactor_accident_demo': 'This segment only shows alarms and effects; no real accident is created in your save.',
    'create_spellbook.ponder.shared.reactor_accident_advice': 'Treat stabilizers and waste space as routine maintenance, not a repair after the fact.',
}


def update(path, entries):
    data = json.loads(path.read_text(encoding='utf-8'))
    data.update(entries)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('updated', path.name, len(entries), 'keys ->', len(data))


if __name__ == '__main__':
    update(LANG / 'zh_cn.json', ZH)
    update(LANG / 'en_us.json', EN)
