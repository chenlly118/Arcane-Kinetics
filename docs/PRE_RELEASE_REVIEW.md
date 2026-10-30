# 首发审核：Arcane Engineering 0.3.0 Beta

审核日期：2026-10-30。作者已批准 MIT、资源分发和公开发布；以下区分本机验证与尚待完成的远程步骤。

## 项目与许可

- GitHub：[`chenlly118/create-Spellbook`](https://github.com/chenlly118/create-Spellbook)，用户已创建，最后核实为 private；目标 public。GitHub 连接可写源码，不提供可见性管理接口。
- Modrinth：用户已创建 [`create-spellbook`](https://modrinth.com/project/create-spellbook/settings)，已通过 Secret 认证核实正式 ID 为 `HTyTLSgC`。使用 Slug 发布；空草稿在首次上传版本前被 API 标记为通用 `project` 类型。
- 版本保留 **0.3.0**，标签 **v0.3.0**；GitHub prerelease、Modrinth beta。Mod ID 保持 `create_spellbook`，展示名为奥术工程 / Arcane Engineering。
- 原创代码 MIT，署名 Sakura。作者确认提供素材原创或有分发权，并确认不存在 Create 美术资源二次分发；`release/approvals.json` 三项为 true。依据与上游例外见 [许可记录](LICENSING_REVIEW.md)及 [第三方声明](../THIRD_PARTY_NOTICES.md)。
- 聚魔盆已独立手绘替换旧 Create 换色资源；机械投影通过运行时模型及着色器实现蓝色透明效果。

## 内容与安装

Java 21、Minecraft 1.21.1、NeoForge 21.1.251+。首发核对 Create 6.0.9、Iron's Spells 1.21.1-3.16.3、Curios 9.5.1、GeckoLib 4.9.3、Iron's Lib 2.2.0、Player Animator 2.0.4；JEI 19.64.0.455 可选。客户端和服务器均需安装。依赖独立安装，不内嵌到本 Mod。

已实现机械学派与 14 个法术、自动施法／卷轴印刷、Create 法术加工及序列装配、黑洞应力反应器、工程师手册和 10 个 Ponder 场景。部分材料配方仍属于原整合包。最新发布前调整仅将鼓风熔炼气流粉尘从每批 3 个改为 9 个，附加火焰／熔岩粒子从 1 个改为 3 个；范围、伤害及触发频率不变。

## 本机验证

- 标准 Gradle 正式构建成功；粒子代码调整后编译成功，许可声明同步后归档构建也成功。未重跑历史大批量测试，`test` 无测试源码。
- 412 个拟公开文件、235 个资源通过凭据扫描、JSON 和语言键校验。JAR 资源与源码一致，Java 21 字节码、Mixin 类及许可齐全，无 GameTest 类、缓存、存档或依赖 JAR。
- 与上一份已安装包比较，仅 `FanAirflow.class` 和 `META-INF/THIRD_PARTY_NOTICES.md` 改变。
- 本地正式包：`dist/0.3.0/create-spellbook-1.21.1-neoforge-0.3.0.jar`。
- SHA-256：`d649975405014c3971e2e6dafcabfd7d6bbe940565978b10b86d678fb92700c0`。
- 公开授权检查通过；包装报告 `candidate_only=false`。这不代表已经上传或通过平台审核。
- 已按项目约定使用安装脚本备份并安装到 mod test；安装结果见本机报告。仅替换本 Mod，保留一个生效版本。
- 先前聚魔盆定向客户端验证已完成模型四朝向、内腔、法力同步、资源重载和原生物品截图；本次粒子小调整未重复该轮美术验收，未进行新的真人联机或大型工厂测试。

## 发布流程与剩余步骤

用户已确认 GitHub Actions Secret `MODRINTH_TOKEN` 配置完成。Token 不进入源码、命令参数或日志，也不需要读取它。

1. 默认使用 `create-spellbook` Slug，无需另填 ID Variable。发布工作流认证后解析正式 ID，并同步 MIT、双语介绍、technology / magic 分类及源码链接。
2. 将扫描过的源码和工作流提交到仓库，核实 main push 的 **Build candidate** 成功。它只构建，不发布。
3. 按已有授权公开仓库；手动运行 **Publish approved candidate**，填写 CI run ID、候选 SHA-256 和 `publish-v0.3.0`，或在该 CI commit 创建 `codex/publish/v0.3.0/<run_id>/<sha256>` 分支触发。工作流在审核的 CI commit 建立版本标签。
4. 发布工作流校验批准、可信 CI 来源、commit、项目 ID 和哈希，使用固定版本 MC-Publish 将同一 JAR 发布到 GitHub 与 Modrinth，不在发布时重新构建。
5. 下载 GitHub 文件逐字节核对，并核对 Modrinth SHA-512 / SHA-1。通过官方 API 固定审核过的依赖版本，再提交新项目公开审核。记录两平台和 Actions 的实际链接；部分成功时只补缺失平台，不盲目重跑双平台发布。

当前远程 CI、版本上传与 Modrinth 审核尚未完成。浏览器控制因无法可靠识别当前网址而停止；仓库可见性与 Modrinth 私有设置需要所有者协助，源码提交仍可通过 GitHub 连接完成。
