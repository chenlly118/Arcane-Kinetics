# 思索教程（Ponder）

本项目使用 Create 6.0.9 内置的 Ponder `1.0.81+mc1.21.1`：`PonderPlugin` 注册、
`PonderStoryBoard` 编排、`.nbt` 结构快照、镜头与 POI、区块显隐、轮廓高亮、输入提示、
粒子与文字。没有沿用旧版 `PonderScene` 构造方式，也没有额外依赖。

## 场景一览

| 入口物品 | 场景 ID | 内容 |
|---|---|---|
| 全部 9 种反应堆方块 | `black_hole_reactor_assembly` | 9³ 示例逐层搭建：机壳、玻璃、上下核心、控制器、三类接口、反应轴、外部动力与法力供给 |
| 全部 9 种反应堆方块 | `black_hole_reactor_operation` | 打开面板、法力与稳定质、启动、应力输出、坍缩物质、停机 |
| 全部 9 种反应堆方块 | `black_hole_reactor_safety` | 失稳条件、增长速率、100% 阈值、修复窗口与失控后果 |
| 奥术施法装置 | `arcane_casting_arm_setup` | 底部动力、侧面法力、卷轴配置、模式切换、置物台加工与冷却 |
| 聚魔盆 | `focusing_basin` | 法术命中产液、伤害换算、容量、手动读取与管道抽取 |
| 魔化置物台 | `arcane_depot` | 放料、施法装置对位、配方、产物与常规 Create 物流 |
| 卷轴印刷机 | `scroll_printer` | 放置规则、模板/学派材料/墨水/纸张、连续印刷与白名单过滤器 |
| 奥术反应装置 | `arcane_reactor` | 无需应力、卷轴配置、背面法力、内部回蓝、自动索敌与限制 |
| 传送门漏斗 | `utility_portal_belts` | 末影珍珠配对、输入/输出、堵塞缓存与过滤 |
| 烟花反应装置 | `utility_firework` | 复制烟花配置、法力消耗、周期发射与红石暂停 |

反应堆的 9 个入口为 `arcane_casing`、`arcane_reinforced_glass`、`reactor_controller`、
`reactor_mana_interface`、`reactor_port`、`reactor_output`、`black_hole_reaction_core`、
`black_hole_energy_core`、`black_hole_reaction_shaft`，它们打开完全相同的三幕。

Ponder 索引中的标签为 `create_spellbook:arcane_engineering`（奥术动力学）。

## 结构快照

- `black_hole_reactor.nbt`：9×9×9 有效反应堆，控制器朝南、反应轴朝西；外部西侧用可见的
  创造马达驱动反应轴，东侧带法力储罐、泵和管道。旧版外置 `create:shaft`/`cogwheel`
  依赖 Flywheel 渲染，在 PonderLevel 中不可见，因此改用有实体模型的马达。
- `arcane_casting_arm.nbt`：底部创造马达与竖直轴、右侧法力管线、顶部拉杆和置物台。
- `focusing_basin.nbt`：开放内腔的聚魔盆、机械泵与单方块储罐。
- `arcane_depot.nbt`：魔化置物台与正前方三格的施法装置、法力管线。
- `scroll_printer.nbt`：置物台上方两格的印刷机、顶部供墨管线与储罐。
- `arcane_reactor.nbt`：自动反应装置、背面进液管与储罐。
- `utility_portal_belts.nbt`：两条带控制器、段索引和物品库存的 Create 传送带。
- `utility_firework.nbt`：烟花反应装置、管线和红石拉杆。

反应堆几何与 `ReactorScanner` 规则逐条对应：棱边机壳、非棱边外壳、空腔、顶/底面核心、
侧面唯一朝外反应轴、三类接口各至少一个。修改结构时重新运行脚本，不要手改二进制。

```powershell
python scripts/generate_ponder_schematics.py
python scripts/generate_ponder_lang.py
```

聚魔盆使用项目原绘的模型和 128×128 奥术贴图，盆口保持开放。

## 文案与时长

正文使用 `PonderTexts` 的 shared text，按“一个动作一条说明”拆分；场景标题使用
`create_spellbook.ponder.<scene>.header`。正文采用正向、简洁的步骤说明；每条文字显示 80 Tick，
文字结束后留出 10 Tick 的阅读停顿，结构显隐和镜头移动另有等待时间。

当前场景时长（Tick，客户端自动测试实测）：

| 场景 | Tick | 约 |
|---|---:|---:|
| 反应堆：搭建 | 892 | 45 秒 |
| 反应堆：运行 | 646 | 32 秒 |
| 反应堆：失稳与事故 | 398 | 20 秒 |
| 奥术施法装置 | 597 | 30 秒 |
| 聚魔盆 | 513 | 26 秒 |
| 魔化置物台 | 528 | 26 秒 |
| 卷轴印刷机 | 558 | 28 秒 |
| 奥术反应装置 | 534 | 27 秒 |
| 传送门漏斗 | 538 | 27 秒 |
| 烟花反应装置 | 452 | 23 秒 |

## 验证

```powershell
python scripts/build_local.py --tests
python scripts/run_portal_ponder_client.py --all-scenes --instance $testInstance --disable-early-window
```

`$testInstance` 为本机安装了依赖的测试实例目录。

`PonderClientTest` 创建一次性平坦世界，编译全部 10 个场景，检查结构快照中的关键方块、
反应堆 9 个入口、法力/墨水/卷轴/产物写入，以及铁锭进出两条传送带的库存，再打开 Ponder UI 截图。
通过标记：

- `CSB_PONDER_COMPILE_PASS scenes=10 schematics=9`
- `AK_PORTAL_PONDER_BELT_PASS`
- `CSB_PONDER_PASS`

截图输出到 `build/reports/portal-ponder-client-*/`。

## 注意

- 思索场景是纯客户端虚拟世界，不创建黑洞实体，不调用 `ReactorAccidents.trigger`，
  不会破坏存档或世界。
- 文案中的 50%、204,800 SU、256 RPM、100 mB/秒、1000 mB 等均为默认配置；服务器改配置后
  数值会变化，但操作关系不变。
- 漏斗场景使用可见传送带的原生库存；储罐液位由 `PonderFx.fillCreateTank` 设置，印刷机压印
  由 `PonderFx.printCycles` 的分段动画表现。
