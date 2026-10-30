# 思索教程（Ponder）

本项目使用 Create 6.0.9 内置的 Ponder `1.0.81+mc1.21.1`。教程完全走 Ponder 当前 API：
`PonderPlugin` 注册、`PonderStoryBoard` 编排、`.nbt` 结构快照、镜头与 POI、区块/方块显隐、
轮廓高亮、输入提示、粒子与文字。没有沿用旧版 `PonderScene` 构造方式，也没有安装额外依赖。

## 场景一览

| 入口物品 | 场景 ID | 内容 |
|---|---|---|
| 全部 9 种反应堆方块 | `black_hole_reactor_assembly` | 9³ 示例逐层搭建、核心位置、三类接口、侧面反应轴 |
| 全部 9 种反应堆方块 | `black_hole_reactor_operation` | 法力、稳定质、启动/停止、应力输出、坍缩物质 |
| 全部 9 种反应堆方块 | `black_hole_reactor_safety` | 失稳条件、增长速率、100% 阈值与不可逆失控 |
| 全部 9 种反应堆方块 | `black_hole_reactor_accident` | 事故视觉演示；只播放警报和粒子，不触发真实事故 |
| 奥术施法装置 | `arcane_casting_arm_setup` | 底部应力、右侧法力、卷轴装取、模式与红石脉冲 |
| 奥术施法装置 | `arcane_casting_arm_modes` | 脉冲、炮台、加工三种模式 |
| 聚魔盆 | `focusing_basin` | 法术命中产液、伤害换算、容量、管道与手动查看 |
| 魔化置物台 | `arcane_depot` | 放料、施法装置对位、配方、产物与物流 |
| 卷轴印刷机 | `scroll_printer` | 模板/学派材料/墨水/纸张、输出卷轴与白名单过滤器 |
| 奥术反应装置 | `arcane_reactor` | 无应力自动索敌、卷轴配置、背面法力与内部回蓝 |

反应堆的 9 个入口为 `arcane_casing`、`arcane_reinforced_glass`、`reactor_controller`、
`reactor_mana_interface`、`reactor_port`、`reactor_output`、`black_hole_reaction_core`、
`black_hole_energy_core`、`black_hole_reaction_shaft`，它们打开完全相同的四幕。

Ponder 索引中新增标签 `create_spellbook:arcane_engineering`（奥术动力学）。

## 结构快照

- `assets/create_spellbook/ponder/black_hole_reactor.nbt`：由 `scripts/generate_ponder_schematics.py`
  生成的 9×9×9 有效结构，控制器朝南，反应轴朝西；外部带法力储罐、泵、管道、Create 轴。
- `arcane_casting_arm.nbt`：底部创造马达与竖直轴、右侧法力管线、顶部拉杆，正前方三格的魔化置物台。
- `focusing_basin.nbt`：开放内腔的聚魔盆、机械泵与单方块储罐；管道为横向直管。
- `arcane_depot.nbt`：魔化置物台与正前方三格的施法装置、法力管线。
- `scroll_printer.nbt`：置物台上方两格的印刷机、顶部供墨管线与储罐。
- `arcane_reactor.nbt`：自动反应装置、背面进液管与储罐。

所有玻璃管道使用 `create:glass_fluid_pipe` 的 `axis` 属性，不再错误套用普通管道的六向连接属性；
聚魔盆使用对称的 `create:block/basin/block` 模型与静态奥术贴图，并在客户端把该方块设为
`RenderType.cutout()`。Create 盆体顶面的开口是贴图透明像素，若用 solid 渲染就会被画成不透明黑面，
同时 `block_directional` 还会多出侧面流道；现在内腔与内部液面都能正常看到，也没有多余侧向结构。

生成脚本中的几何与 `ReactorScanner` 的规则逐条对应：棱边机壳、非棱边外壳、空腔、
顶/底面核心、侧面唯一朝外反应轴、三类接口各至少一个。修改结构时请重新运行脚本，不要手改二进制。

```powershell
python scripts/generate_ponder_schematics.py
python scripts/generate_ponder_lang.py
```

## 文案与本地化

所有正文走 Ponder 的 shared text：Java 中的键在 `PonderTexts`，玩家实际看到的中英文位于
`assets/create_spellbook/lang/{zh_cn,en_us}.json` 的 `create_spellbook.ponder.shared.*`。
场景标题使用 `create_spellbook.ponder.<scene>.header`。这样不依赖文本指令的注册顺序。

## 验证

开发期自动测试：

```powershell
python scripts/build_local.py --tests
python scripts/run_ponder_client.py
```

`PonderClientTest` 会创建一次性平坦世界，编译全部 10 个场景，检查 6 个快照中的关键方块、
全部反应堆方块入口、卷轴/法力/墨水/产物是否由场景指令真实写入，再打开 Ponder UI 截图。通过标记：

- `CSB_PONDER_COMPILE_PASS scenes=10 schematics=6`
- `CSB_PONDER_PASS`

截图输出到 `build/reports/ponder-client/`。测试类位于 `gametest`，发布 JAR 不包含。

## 注意

- 思索场景为纯客户端虚拟世界，不会创建黑洞实体、不会调用 `ReactorAccidents.trigger`，
  因此不会破坏存档或世界。事故演示只播放粒子和文字。
- 场景只演示已实现机制。文案中的 50%、204,800 SU、256 RPM、100 mB/秒、125 秒等均为当前默认配置；
  服务器改配置后数值会变化，但教程中的操作关系不变。
- Ponder 的 `PonderLevel` 不 tick 方块实体。储罐类显示需要在场景脚本里同时把
  `LerpedFloat` 液位设置到目标值，`PonderFx.fillCreateTank` 已处理。
- 卷轴印刷机的压印动画由方块实体 Tick 驱动，Ponder 中不运行该 Tick；教程用印刷头粒子与成品变化表达流程。
