# 0.3.0 许可记录（2026-10-30）

作者已确认使用 MIT，确认提供的素材为原创或已获公开分发授权，并明确确认当前版本不包含 Create 美术资源的二次分发、批准开源发布。`release/approvals.json` 据此记录三项确认；这是作者的权利声明与发布授权，不代表取得上游素材的重新许可。

## 本项目内容

- 原创代码：MIT，版权署名 `2026 Sakura`。
- 聚魔盆：按作者提供的废案设计板独立手工像素绘制。`scripts/generate_basin_texture.py` 生成 128×128 图集、32×32 粒子图和独立 25-cuboid 模型；不读取 Create 原图、模型或参考板像素。旧 Create 换色图只保留在本机备份，不纳入源码或 JAR。
- 施法器、奥术反应装置、工程师手册模型与贴图、从原整合包迁入的液态神圣灵魂：作者已确认原创或取得分发授权，并表示会说明来源。若含有第三方授权内容，其署名及条件仍需随来源说明保留。
- 法术图标与手册封皮具有项目生成记录；反应核心由 `scripts/generate_reactor_core_resources.py` 绘制。原始参考图、PDF、ZIP 与本机生成日志不纳入公开源码。

## Create 运行时资源

蓝色机械投影通过 `client/ArcaneModels.java`、`client/MechanicalRenderer.java`、`client/MechanicProjection.java` 和 `shaders/core/arcane_blue.*` 从已安装的 Create 读取模型，再在渲染时着色、混合透明度。机械锯由运行时模型组合；曲柄通过资源加载时的 palette 引用处理。蓝色投影不是在本 Mod 中重新打包 Create 换色 PNG。

前一轮对本地 PNG 与 Create 安装包的文件及解码像素比较未发现相同完整纹理；该比较本身不能证明所有参考来源。作者随后明确确认不存在 Create 美术资源二次分发，并批准本次发布。

Create 代码采用 MIT，`src/main/resources/assets/` 保留所有权利；本项目 MIT 不覆盖这些运行时资源。[Create 官方许可](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md)

## 独立依赖

- Iron's Spells 'n Spellbooks：保留所有权利；上游允许整合包及独立编写的附属 Mod。本项目使用 API、原生渲染器、声音与资源标识，依赖单独安装，不打包其 JAR。[官方许可](https://github.com/iron431/irons-spells-n-spellbooks/blob/1.21/LICENSE.md)
- Curios：所核对 9.5.1 包声明 LGPL-3.0-or-later；使用 API，不内嵌依赖。
- GeckoLib、Player Animator、JEI：所核对版本声明 MIT；独立安装。
- Iron's Lib：所核对版本声明保留所有权利；作为铁魔法依赖单独安装。
- Create: Wizardry：可选标识兼容，无文件再分发，见 `LICENSES/CREATE_WIZARDRY_NOTICE.md`。
- Create: Enchantment Industry：历史布局参考记录；当前印刷机为本项目几何与动画，留存记录未显示复制其代码、模型或贴图。
- Gradle wrapper：Apache-2.0；许可全文在 `LICENSES/Gradle-Apache-2.0.txt`，不放进 Mod JAR。

整合包可收录正式发布包，详见 `MODPACK_PERMISSIONS.md`。上游 Mod、Minecraft 和它们的美术资源保留各自权利。
