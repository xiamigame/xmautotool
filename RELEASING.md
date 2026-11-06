# 发布与维护

仓库历史已压缩为单一提交：**tag 是版本唯一事实来源**，不要依赖旧提交 SHA 或旧 `v0.2.0`。

## 发布模型

**一个 Minecraft 版本一个 tag/release。**

- tag 格式：`v<mod_version>-<mc_version>`，例如 `v0.3.0-1.18.2`。
- 每个 release 只放该 MC 版本 adapter 构建出的 jar，不混放多个 MC 版本。
- `mod_version` 在 `gradle.properties`，是代码/功能版本；MC 版本由 tag 后缀和 `adapter/<mc>/` 目录表达。
- 新增 1.19.2 adapter 时，打 `v0.3.0-1.19.2`（只有 `common` 逻辑变了才升 0.3.1/0.4.0），不会动 1.18.2 的 release。

## 分支模型

- `main`：唯一开发线，承载最新版本；所有功能、修复先落到这里。
- `release/<major>.<minor>`：只在需要长期维护旧版本时创建；`main` 不合并进 `release/*`。
- `feature/*`、`hotfix/*`：短期分支，合并后删除。
- 多 Minecraft 版本不建分支，用 `adapter/<mc版本>/` 子工程表达。

## 版本号

- **patch**：修 bug，不改配置格式与行为契约。
- **minor**：新功能、新界面、新 adapter。
- **major**：配置格式或行为不兼容。

只有 `common` 或 `mod_version` 语义变化才升 `mod_version`；只新增 adapter、且 `common` 未变时，可以继续用同一 `mod_version`，靠 tag 的 MC 后缀区分。

## 发布步骤

1. 在 `main` 上确认测试与目标 adapter 构建通过：
   ```powershell
   .\gradlew.bat :common:test :adapter:1_18_2:build --no-daemon
   ```
2. 需要时修改 `gradle.properties` 的 `mod_version`，同步 README / 游戏内说明。
3. 提交：`chore(release): <version>`。
4. 打 annotated tag（tag 的 mod 版本必须等于 `gradle.properties`）：
   ```powershell
   git tag -a vX.Y.Z-<mc版本> -m "XMAutoTool X.Y.Z for Minecraft <mc版本>"
   git push origin main
   git push origin vX.Y.Z-<mc版本>
   ```
5. GitHub Actions 的 `release` 工作流解析 tag：校验 adapter 目录与 `mod_version`，只构建该 adapter，创建对应 Release。
6. 检查 Release 标题 `XMAutoTool X.Y.Z (Minecraft <mc>)` 与资产 `xmautotool-<mc>-<mod>.jar`。

## 热修

- 从对应 tag 切 `hotfix/<x.y.z>`（若有维护线，则从 `release/<major>.<minor>` 切）。
- 修复、补测试，bump patch，打 `vX.Y.Z-<mc>` tag，重复发布步骤。
- 仅当 `main` 也受影响时，把修复 cherry-pick 到 `main`。
- 禁止 rebase / force-push 已发布的 `main`（历史重写属于仓库级操作，需单独评审并通知所有使用者）。

## 新增 Minecraft 版本

1. 复制 `adapter/1_18_2/` 为 `adapter/<新版本>/`。
2. 改该目录的 `gradle.properties`（Minecraft/Forge/mappings）与 `build.gradle`。
3. 实现同一组端口：`Host`、`Game`、`Canvas`；界面与引擎代码在 `common`，不改。
4. `settings.gradle` 加一行 `include 'adapter:<新版本>'`。
5. 本地验证：`.\gradlew.bat :adapter:<新版本>:build --no-daemon`。
6. 提交后打对应 tag：`v<mod_version>-<新版本>`；**不需要**改 CI matrix。

## CI 说明

`.github/workflows/release.yml`：

- 触发：推送 `v*` tag。
- `Parse tag`：解析 `v<mod>-<mc>`，校验 `adapter/<mc>/build.gradle` 存在、tag 的 mod 版本与 `gradle.properties` 一致；不匹配直接失败。
- 构建 job：先 `:common:test`，再 `:adapter:<mc>:build`，上传单个 jar。
- 发布 job：创建/更新对应 Release，标题带 MC 版本，附件只放该 adapter 的 jar。
- 一次性 cleanup：tag 为 `v0.3.0-1.18.2` 时删除旧的 `v0.3.0` release/tag（该 release 是规则迁移前的产物）。

## 注意事项

- 发布前确认 `git status` 干净、`mod_version` 与 tag 前缀一致。
- 不要复用已发布的 tag；不要往旧 release 追加不同提交构建的 jar。
- `docs/` 不入库（本地台账）；`AGENTS.md` 入库。
- 历史压缩后旧 tag/分支已删除；不要凭旧 SHA 取文件。
