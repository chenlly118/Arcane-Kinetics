# CurseForge 发布检查：Arcane Kinetics 0.3.0

日期：2026-10-30。项目 ID **1773802**，地址 `https://www.curseforge.com/minecraft/mc-mods/arcane-kinetics`。

## 项目资料

| 项目 | 检查或处理结果 |
| --- | --- |
| 名称／Slug | 页面核实为 Arcane Kinetics／arcane-kinetics |
| 类别 | Mods；主要 Create，附加 Technology、Magic，填写正确 |
| 许可证 | 页面核实 MIT License |
| 第三方分发 | 页面核实 Allow distribution to 3rd party，与整合包收录政策一致 |
| 源码 | 页面核实为 https://github.com/chenlly118/Arcane-Kinetics |
| 英文简介 | 原内容使用旧名称 Create Spellbook；已提供新版简介，作者确认已保存 |
| 详细介绍 | 原内容仅两段简介；已准备完整双语介绍 `curseforge-description.md`，作者确认已保存 |
| 图标 | General 中已有图标；本轮浏览器连接中断，未独立完成图像内容复核 |
| 图库 | 当前为空，属于可继续补充的展示资料 |
| 默认依赖 | 网页默认关系为空；本轮上传及后续自动发布均显式附带完整关系，手工上传时需注意补齐 |

## 版本文件

原文件 **9369806** 显示 Uploading、仅标 Client、Java 未选、更新日志和依赖为空。本轮保留原记录，没有删除或覆盖。公开 CDN 返回空响应不能单独证明原文件损坏，也可能受平台处理／新项目审核影响。

使用固定版本 MC-Publish，通过官方上传 API 成功提交新文件 **9369987**：

- 展示名：Arcane Kinetics 0.3.0 Beta。
- Minecraft 1.21.1；NeoForge；Java 21；Client + Server。
- 必需：Create（328085）、Iron's Spells 'n Spellbooks（855414）、Curios（309927）、GeckoLib（388172）、Iron's Lib（1492763）、playerAnimator（658587）。
- 可选：JEI（238222）。
- 完整更新日志随文件提交。
- 输入文件直接下载自 GitHub `v0.3.0` Release，未重新构建。
- 输入 JAR SHA-256：`0cabf25e53847b4e62a915ccb679a055ce3239152476e4df97fbf6a85b9fdb8b`，与 GitHub／Modrinth 发布文件相同。

[文件页面](https://www.curseforge.com/minecraft/mc-mods/arcane-kinetics/files/9369987) · [上传记录](https://github.com/chenlly118/Arcane-Kinetics/actions/runs/41026166106)。

上传 API 返回成功及文件 ID。公开文件页当前未可读，CDN 暂未返回有效 JAR，因此尚未完成 CurseForge 下载端的哈希回读，也不能据上传成功宣称平台审核通过。待平台处理及审核后确认新文件可下载，再决定旧记录是否重复或需要清理。

## 后续维护

- `CURSEFORGE_TOKEN` 仅存 GitHub Actions Secret；没有读取或输出其值。
- `curseforge-existing.yml` 仅用于本次已发布 0.3.0 的补充分发；需要明确触发，不随普通源码提交上传。
- 常规 `publish.yml` 已加入 CurseForge，同一候选 JAR 可统一发布到三个平台。
- main 中的 Arcane Kinetics 游戏内名称与图标修改仍为未发布内容。本轮没有发布这些源码修改，也未更改已有 GitHub／Modrinth 文件。
- 浏览器编辑连接超时后又被网址安全检查停止；网页简介与介绍由作者手动保存，其余文件发布通过官方 API 完成。
