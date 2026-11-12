# Changelog / 更新日志

## 0.5.0 Beta — 机械法术与跨维度物流

[完整双语更新说明 / Full bilingual release notes](release/notes/0.5.0.md)

- 应力驱动会使生物旋转；鼓风熔炼兼顾缠魂和烟熏，可推开或在潜行时吸引生物。
- 加入烟花反应装置和可跨维度传输物品的传送门漏斗。卷轴印刷机新增升级模式，奥术施法装置支持六向摆放，黑洞反应堆更新控制器界面。
- 新增法术命中加工配方。可选柴油动力联动加入奥术燃料、分馏与墨水发酵。
- 游戏内名称与图标更新为奥术动力学和聚魔盆。

- 奥术板统一采用 `create_spellbook:arcane_plate`，装有 Create: Wizardry 时两者共用同一物品，旧奥术板的数量与物品数据保留。
- 加深奇点锭和奇点块的黑紫色调。
- 魔化扳手支持潜行向上滚动切换扳手模式、向下滚动切换法杖模式，主手和副手均可使用，切换后显示当前模式。
- 扳手模式复用 Create 的旋转、潜行拆卸和机器扳手识别，法杖模式通过铁魔法原有系统施法。模式随物品保存，旧存档默认使用法杖模式。
- 精炼奥术燃料恢复最初版的像素图案和颜色，保留半透明效果。

## 0.4.13 — 可选流体联动与装配配方

- 装有 Create: Wizardry 时，法力和血液的配方与流体堆使用巫术学的流体，旧流体堆保留原有容量。
- 装有 Create Diesel Generators 时，启用奥术燃料和精炼奥术燃料、普通及超热分馏，以及三种柴油引擎的原参数。两种流体采用奥术源质配色的动画纹理，精炼燃料半透明。
- 添加液态神圣灵魂超热搅拌、空白符文与奥术构件序列组装、法力工作盆冲压配方。

## 0.4.12 — 漏斗输送与反应堆界面

- 传送门漏斗每轮最多传送四叠物品；向传送带输出时按带面的空位及时出货。保留过滤、堵塞缓存和跨维度按需加载。
- 传送门漏斗的思索使用 Create 传送带库存承载铁锭，演示从收取端进入、从输出端送出。
- 黑洞反应堆控制器采用原版容器风格的浅灰面板、物品槽和按钮。
- 恢复 0.4.10 聚魔盆的模型和贴图。移除治愈站、猩红处刑台及各自的教程。

## 0.4.11 — 思索精简与手册朝向

- 思索文案改为正向、单步说明，移除“不会/不能/无法/不再/不需要/无需”等否定式措辞。
- 精简主设备教程：减少重复步骤，正文每条 80 Tick，场景时长回到约 20–45 秒，保留关键结构与操作。
- 修正传送门漏斗演示中的铁锭路径：输入从东侧进入；输出落到下方传送带后沿带向西输送，并在带上保留成品。
- 工程师手册采用 Iron's Spellbooks 原版书本的显示变换：手持与装备时的封面/书脊朝向与之对齐，GUI 仍使用独立的手册图标；第一人称单独校正为封面对着玩家。
- 聚魔盆方块纹理改为 **16×16**：将原图压缩为四个 8×8 分区，保留侧面紫色宝石、黄铜扣件和盆底符文；同步重映射模型 UV，方块模型形状和碰撞不变。
- 游戏测试默认静音，并在测试结束后删除一次性测试存档。

## 0.4.9 — 思索教程优化

- 反应堆教程拆成搭建、运行、失稳与事故三幕；搭建按层显示完整外壳、上下核心、控制器、三类接口、反应轴和外部供给，不再只出现部分结构。
- 外置 `create:shaft`/`cogwheel` 在 PonderLevel 中依赖 Flywheel 渲染而不可见，改为可见的创造马达驱动反应轴；东侧法力储罐/泵/管道仍完整显示。
- 反应堆教程中加入换侧镜头，展示另一侧的动力连接；运行教程也先展示完整结构再讲接口。
- 教程正文改用按步骤拆分的详细文案，每条文字显示 95 Tick 并留出阅读停顿；全部场景时长提升到 20–79 秒，接近 Create 原版节奏。
- 恢复反应堆“失稳与事故”场景；只播放警报、红色轮廓和粒子，不触发真实事故。
- 四个新设备（传送门漏斗、治愈站、烟花反应装置、猩红处刑台）的思索场景延长，避免一闪而过。
- 客户端自动测试覆盖 12 个场景、11 份结构快照，并检查外置马达与储罐。

## 0.3.0 — First public beta / 首个公开测试版（2026-10-30）

This is the first public release. Earlier 0.1.x–0.2.x versions were internal development builds; the public version remains **0.3.0**.

这是首个公开发布的版本。此前 0.1.x～0.2.x 为内部开发记录，首发保留 **0.3.0**。

- 机械学派与 14 个机械法术、工程师手册、魔化扳手和奥术曲柄。
- 聚魔盆、魔化置物台、法术感应器、自动施法设备和卷轴印刷机。
- 原生 Create 置物台／传送带法术加工、JSON 配方及序列装配，配套 JEI 展示。
- 5～19 格边长的黑洞反应器、稳定质与法力供给、应力输出、失稳及事故机制。
- 手绘动态核心材质，中英文界面，10 个 Ponder 思索场景。
- 修正思索演示的玻璃管道状态和聚魔盆内腔渲染。
- 聚魔盆按用户设计板独立手绘深灰金属、黄铜护角／支脚和紫色内衬，使用独立模型替换旧 Create 换色资源。
- 鼓风熔炼的气流粉尘与附加火焰／熔岩粒子数量提高至原来的三倍。
- 固定公开构建依赖来源，增加自动构建、人工审核门槛和同一 JAR 的双平台发布流程。

English: the mechanical school and 14 spells; spell-powered machines, scroll printing and Create processing; a multiblock black-hole kinetic generator; animated core artwork; bilingual localization and 10 Ponder scenes. Includes corrected Ponder pipe states and basin cutout rendering, portable dependency declarations and a reviewed publication workflow.

**Beta limits / 测试版边界：** no exhaustive remote multiplayer, large factory endurance or third-party spell coverage. Reactor accidents can damage worlds. Some material recipes remain pack-specific. Original code is MIT; the author has confirmed asset clearance and approved publication. Fan Smelting particle counts are tripled.

完整首发说明：[release/notes/0.3.0.md](release/notes/0.3.0.md)。
