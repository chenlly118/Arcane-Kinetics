# Arcane Kinetics

<img src="src/main/resources/logo.png" alt="Arcane Kinetics 聚魔盆图标" width="128" align="right" />

中文名：**奥术动力学**。

新名称与聚魔盆图标已同步到项目页面和源码，游戏内显示将在下次版本发布时更新。

[English](README.en.md) · [更新日志](CHANGELOG.md) · [整合包授权](MODPACK_PERMISSIONS.md)

Minecraft **1.21.1 / NeoForge** 的 Create × Iron's Spells 'n Spellbooks 联动 Mod。用机械法术驱动加工，以卷轴自动施法，并建造提供 Create 应力的黑洞反应器。

首个公开版本 **0.3.0 Beta** 已发布：[GitHub 下载](https://github.com/chenlly118/Arcane-Kinetics/releases/tag/v0.3.0)。文件已上传至 [Modrinth](https://modrinth.com/project/create-spellbook)，新项目正在平台审核中。发布记录见 [审核报告](docs/PRE_RELEASE_REVIEW.md)。

## 已实现功能

- 机械学派与 **14 个法术**：齿轮飞弹、奥术冲压、动力锯、应力驱动、烈焰灌注、鼓风熔炼、粉碎轮、活塞推进、奥术钻头、黄铜外骨骼、压制、弹射过载、机械师与机械大师。
- 聚魔盆、魔化置物台、法术感应器、奥术施法装置和奥术反应装置；法术加工支持 Create 置物台、传送带和序列装配。
- 卷轴印刷机：用模板、学派材料、墨水与纸批量制作原生卷轴。
- 5～19 格边长的立方体黑洞反应器、法力与稳定质接口、应力输出和失稳事故。
- 工程师手册、魔化扳手、奥术曲柄、法力容器与材料注册。
- 中英文界面、**10 个 Create Ponder 思索场景**和可选 JEI 配方展示。

## 安装环境

客户端与服务器均需安装本 Mod，并保持版本一致。

| 项目 | 本版核对版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| Java | 21 |
| NeoForge | 最低 21.1.251；既有客户端验收使用 21.1.262 |
| Create | 6.0.9（声明支持范围 6.0.9 ≤ 版本 < 6.1） |
| Iron's Spells 'n Spellbooks | 1.21.1-3.16.3（声明范围 < 1.21.1-3.17） |
| Curios API | 9.5.1+1.21.1 |
| GeckoLib | 4.9.3 |
| Iron's Lib | 1.21.1-2.2.0 |
| Player Animator | 2.0.4+1.21.1 |
| JEI（可选） | 19.64.0.455 |

Create 随包提供 Ponder、Flywheel 和 Registrate。依赖 JAR 不包含在本 Mod 中。请使用针对 Minecraft 1.21.1 的 NeoForge 文件；Forge、Fabric 和其他游戏版本不在支持范围内。

## 使用与限制

在创造物品栏或 JEI 找到设备，按 Create 的思索快捷键查看教程。[法术加工配方格式](docs/ARCANE_PROCESSING_RECIPES.md)支持整合包自定义。

这是测试版本。大型工厂长期运行、两名真人远程联机和所有第三方法术尚未全面验收；自动施法兼容性按具体法术决定。黑洞反应器失控会破坏世界，建议先在测试世界试用并保留存档备份。液态神圣灵魂和奥术构件的部分生产配方属于原整合包，未随独立 Mod 发布。

## 从源码构建

安装 JDK 21，运行：

```sh
./gradlew --no-daemon build
python scripts/release_check.py --jar build/libs/create-spellbook-1.21.1-neoforge-0.3.0.jar
```

Windows 使用 `gradlew.bat`。Gradle 从公开来源下载固定版本依赖；不需要作者的游戏目录或 `.cache/`。正式 JAR 位于 `build/libs/`，`-sources.jar` 为源码附件。首次构建需要网络；实际验证结果见审核报告，不能把配置完成等同于 CI 已通过。

## 许可与贡献

原创代码使用作者已确认的 [MIT 许可证](LICENSE)；作者已确认资源分发权并批准公开发布。上游依赖及其运行时资源保留原许可证，具体说明见 [第三方声明](THIRD_PARTY_NOTICES.md)和[许可审核](docs/LICENSING_REVIEW.md)。整合包政策见 [MODPACK_PERMISSIONS.md](MODPACK_PERMISSIONS.md)。

提交问题时请附游戏、加载器及依赖版本、复现步骤和经过脱敏的日志。
