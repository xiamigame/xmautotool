# 项目指导手册（AGENTS.md）

> **版本** 2.4 · 2026-11-05
> 本文件定义协作规则，**它自身不受这些规则约束**——改版直接改，见 §12。
>
> **30 秒版**：先写计划 → 等用户确认 → 再动手 → 计划锁定 → 之后所有改动只在末尾追加。

## 0. 怎么用

- 复制到目标项目根目录，文件名保持 `AGENTS.md`，AI 协作工具会自动读取。
- 这是**约束**不是建议：「必须」「禁止」「允许」按字面执行。
- 本地化（路径、技术栈、命令）直接改对应条款，并在 §12 版本表加一行。

### 0.1 开工先判意图

会话第一件事不是建目录，是判断"这是问事，还是改事"。

1. **资讯类**（问概念、查现状、解释代码、方案评估、纯讨论）：正常回答。只做只读调查，**不建 feature、不写文档、不写台账、不提交、不改任何文件**。答完即止。
2. **修改类**（改代码 / 配置 / 文档，跑会改变状态的命令，发布，删除）：**先定 feature，再定基线，最后才写计划**。顺序固定：判意图 → 直接问用户「这次 feature 叫什么」，用户给的名字即视为准入 → 建 `docs/<feature>/` 与 `feature.yaml`，并把本会话写入 `sessions`（`approved_by: user`）→ 定变更基线（§0.2）→ 计划 → 确认 → 执行 → 锁定 → 追加记录。
3. **判不清**（如"看看这块能不能更好"）：按资讯处理，先问一句"要我改吗"；确认要改再转修改流程。
4. **中途转向**：资讯过程中发现确实要动手，停下，回到第 2 步走完整流程，不要顺手改。

### 0.2 定变更基线

feature 名只回答"改哪件事"，还要回答"改在哪个版本上"。以下四项在写计划之前逐项对齐，未定完不得进入 §3：

1. **源头**：基于哪个 tag / 分支（如 `v1.0.0`、`release/1.0`、`main`）。默认取当前 `HEAD`；不确定就问，不猜。
2. **分支**：修旧线用 `hotfix/<x.y.z>`；新功能用 `feature/<slug>`；纯文档或小修可直接在 `main`。是否新开由用户定。
3. **版本号**：修 bug → patch；新功能 → minor；不兼容 → major。给出建议值与理由，用户确认或改。
4. **维护线**：若这次要长期支持某个旧版本，从该 tag 拉 `release/<major>.<minor>`；否则不建。**禁止**把 `main` 合进 `release/*`。

四项结论写进 `feature.yaml` 的 `change` 段。它们决定改动范围、回滚方式与发布动作，所以必须在计划之前定，不能边写计划边补。

## 1. 优先级与冲突

取值顺序：

1. 用户当前的明确指令
2. 本文件
3. 项目内既有约定
4. 模型/工具的默认习惯

- 用户指令与本文件冲突：先指出一句冲突，再按用户指令执行。
- 冲突必须写进计划台账，注明「用户指令覆盖第 X 条」。
- 不静默选择，不自行调和两种相反要求。

## 2. 铁律与阶段门

1. **先计划后执行**：没写下来的计划，不产生任何写操作。
2. **计划须确认**：确认前只允许只读调查。
3. **执行后锁定**：被批准的计划原文冻结。
4. **改动只追加**：后续变更写在末尾追加记录里。

| 门 | 允许做什么 | 退出条件 |
|---|---|---|
| G0 调查 | 读文件、搜索、查状态 | 能写出可执行计划 |
| G1 计划 | 只写计划，不改代码/配置/依赖 | 计划已展示给用户 |
| G2 确认 | 等待 | 用户明确确认 |
| G3 执行 | 按步骤实施 | 验收标准全部满足 |
| G4 记录 | 打锁定标记、追加记录 | 记录已写入 |

单向：G1 未确认不得进 G3；执行中计划失效，退回 G1 重新确认，不得边改边补计划。

## 3. 计划

必含要素，缺一即不合格：标题、**目标**（可验证）、背景与约束、范围、**非目标**（至少一条）、步骤（可独立回滚）、影响面、风险与回滚、验收标准、需要的确认点。

写法：

- 编号列表，不写散文。
- 高危操作（删除 / 迁移 / 发布 / 花钱 / 对外可见）单列并标注。
- 不写「顺便」「顺手」——拆成独立计划。
- 不用「优化」「改进」这类无法验收的词。

## 4. 确认

**算确认**：确认 / 可以 / 按这个来 / 开始执行 / 明确选择某个选项 / 带修改意见的批准。

**不算**：沉默、表情、「嗯」、提问或质疑、「看着办」「你决定」（此时必须回问一次）、非发起人的同意。

细则：

- **确认带修改** = 修订 + 批准：先把修订写进计划，再执行，记录里注明「确认时修订」。
- **确认只对当时那一版有效**；计划改过要重新确认。
- **紧急**可申请「紧急放行：只做 X，约 N 分钟」；紧急不跳过确认，只缩短计划。
- 一次只确认一个计划，逐条执行、逐条记录。

## 5. 执行

- **先读后改**：改任何文件前先读它。
- **最小 diff**：不动计划外的行，不做无关重命名、重排、重格式化。
- **一次一个逻辑变更**：修 bug 与重构不混在一起。
- **不擅自扩张**：计划没写的功能、依赖、文件一律不做。
- **失败即停**：报错、测试变红、行为不符就停，不猜着继续。
- **不掩盖**：跳过步骤、降低标准、临时注释测试，都必须显式说明。
- **可回滚**：破坏性操作前确认能从版本控制或备份恢复。

## 6. 锁定

```markdown
<!-- LOCK id=L1 date=YYYY-MM-DD plan=P1 -->
> 🔒 **L1 已锁定（YYYY-MM-DD）** —— 本段只读。
```

- 锁定后不改、不重排、不删，包括错别字。
- 不用新版计划覆盖旧计划；新版是**新条目**。
- 只能追加新条目，或在追加记录里声明覆盖/废弃。
- 台账文件位置见 §8。

## 7. 追加

```markdown
### A<n> · YYYY-MM-DD · 提出人：<用户/agent>

- **类型**：修订 | 补丁 | 废弃 | 澄清 | 执行记录
- **关联**：L<k> 第 <m> 条
- **变更**：
- **影响**：
- **生效**：
- **状态**：ACTIVE
```

- 编号只增不减，不补跳号。
- 与旧条目冲突时以最新 ACTIVE 为准，但必须写明「覆盖 A<x> 第 y 条」。
- 撤回只能追加 `SUPERSEDED BY A<x>`，不删原文。
- 一条记录只讲一件事。
- 执行完必须补一条**执行记录**：做了什么 / 跳过什么 / 怎么验的 / 遗留。

## 8. 目录与台账

- 文档放 `docs/<feature>/`，`<feature>` 用 kebab-case，表示「这次在做什么」。
- **feature 名由用户定**：开工直接问「这次 feature 叫什么」，用户给的名字即准入依据；不自行起名、不给候选；复用已有 feature 也要问。
- 目录内最小集：
  - `feature.yaml`：分支 / 版本 / 状态台账，**唯一可原地更新**的文件；
  - `plan-<YYYY-MM-DD>-<session>.md`：会话计划与记录，**只追加**。
- **会话准入**：`feature.yaml` 维护 `sessions` 名单，身份用**运行时唯一标识**，不用日期或 slug：
  - 条目字段：`id`（运行时唯一标识，如环境变量 `DSH_SESSION_ID`，形如 `session-<uuid>`）、`label`（人类可读名，仅用于命名 `plan-<label>.md`）、`role`（owner|member）、`status`（approved|pending|revoked）、`joined`、`approved_by`。
  - 开工先查名单：运行时 id 在名单且 `approved` 才能写；不在名单、`pending`、`revoked` 一律**只读**。
  - 加入：会话申请 → **只有用户能批准**（owner 不得代批）→ 批准后写入名单（`id` 与 `label` 同时填），并在自己 plan 文件记录。
  - **锁定**：条目一旦写入 `id`，即按该标识锁定，不得改指另一会话；只允许改 `label`（改名）和 `status`（撤销/恢复需用户批准）。
  - 撤销：`revoked` 立即失去写权，条目保留不删，恢复需用户再次批准。
  - 一个会话同一时间只挂一个 feature；挂第二个需用户单独确认，并在两边名单都登记。
- **防挂错**：以运行时 id 比对名单，**不得用 label 判断身份**（label 可能撞车或被改）；`plan-<label>.md` 的文件名必须能在名单里找到对应条目，否则停写；`owner_session` 必须指向 `role: owner` 且 `approved` 的条目，并写明其 `id`（`owner_session` 存 id，不存 label）。
- 迁移或改名：旧路径留指针，校验内容一致后再删。
- 不往别人的 feature 目录写；`docs/` 根不放散文档。

## 9. 版本控制

项目无 `.git` 时，先提议 `git init -b main`，同意后执行。**remote 由用户决定**：未明确指示禁止 `remote add` / `push`。

**分支**

- `main`：主干，承载最新版本线，始终可发布。
- `release/<major>.<minor>`：维护线（如 `release/1.0`），长期存在。
- `feature/<slug>`、`hotfix/<x.y.z>`：短期分支。

**版本与分支解耦**：分支名只到 minor，patch 用 tag 表达（`v1.0.1`）。不要用 patch 号当分支名。

**backport**

1. 从 `release/1.0` 切 `hotfix/1.0.1` → 修复 → 合回 `release/1.0` → 打 `v1.0.1`；
2. 仅当 `main` 也受影响，才 cherry-pick 到 `main`；
3. 禁止把 `main` 合进 `release/*`；禁止 rebase 共享分支；禁止 force push 已推送分支。

**发布**：合入维护线 → 更新台账 `version` → 打 annotated tag。tag 是版本事实来源，分支只是容器。

**寿命**：`release/*` 保留至 EOL（台账标 `eol` 并记最后版本）；`feature/*`、`hotfix/*` 合并后删除，删前确认已合并。

**提交**：`type(scope): 摘要`，type ∈ feat|fix|docs|refactor|test|chore；一次提交一个逻辑变更；生成物与日志不入库。

**台账字段**：`is_repo / repo_root / branch / base / worktree / upstream / release_line / version / tags`；本次变更基线另记在 `change` 段：`source_ref / source_version / new_branch / release_line_action / version_bump / target_version`（§0.2）。

误删分支从 tag 重建（`git branch <name> v1.0.1`），不改写已推送历史。

## 10. 验证与汇报

完成定义（缺一即报「未完成」）：

1. 计划每步都有产出，可指认。
2. 验收标准有证据（命令输出、测试结果、可观察行为），不是「应该可以」。
3. 无计划外改动。
4. 追加记录已写入并标状态。

汇报四段，按序：**做了什么** / **没做什么** / **怎么验的** / **遗留与建议**。

禁止：夸大完成度、隐藏失败、把「计划」说成「已完成」。

## 11. 反模式

- 边改边补计划。
- 把「我觉得用户会同意」当确认。
- 一次执行打包多个计划，出问题无法归因。
- 静默修改已锁定内容。
- 删旧记录让文档"变干净"。
- 顺手重格式化整个文件，制造巨大 diff。
- 报错就换写法绕过，不报原始失败。
- 验收标准不可执行。

## 12. 本文件的维护

- 本文件**不受** §2、§6、§7 约束：它定义规则，自身按普通规范文档维护，**直接改版**。
- 改版必须更新下表，并在同一次提交里说明改了什么。
- 复制到新项目后，各项目维护自己的版本表，不要求与本文件同步。

| 版本 | 日期 | 说明 |
|---|---|---|
| 2.4 | 2026-11-05 | feature 名由用户直给即准入；新增 §0.2 变更基线（源头/分支/版本号/维护线）与 `change` 段 |
| 2.3 | 2026-11-05 | 新增开工意图判定：资讯类不落盘，修改类必须先定 feature 才能计划 |
| 2.2 | 2026-11-05 | 会话身份改用运行时唯一标识（`DSH_SESSION_ID`）锁定，`label` 仅用于命名 |
| 2.1 | 2026-11-05 | 新增会话准入名单 `sessions`：approved 才可写、加入需用户批准、防挂错校验 |
| 2.0 | 2026-11-05 | 合并初版与历次修订：会话隔离、feature 目录、git 多版本模型；取消对自身的追加约束；精简篇幅 |
| 1.0 | 2026-11-05 | 初版：计划 → 确认 → 锁定 → 追加四铁律 |

## 附录 A · 模板

### A.1 计划

```markdown
## 计划 P<n> · <标题>

**目标**：
**背景与约束**：

**范围**
1.
2.

**非目标**
-

**步骤**
1. [ ] 改 <文件/系统>：<动作>（可回滚点）
2. [ ] 验证：<命令>

**影响面**：
**风险与回滚**：
**验收标准**：
**需要的确认点**：
```

### A.2 锁定块

```markdown
<!-- LOCK id=L<n> date=YYYY-MM-DD plan=P<n> -->
> 🔒 **L<n> 已锁定（YYYY-MM-DD）** —— 本段只读。
```

### A.3 追加记录

```markdown
### A<n> · YYYY-MM-DD · 提出人：

- **类型**：
- **关联**：
- **变更**：
- **影响**：
- **生效**：
- **状态**：ACTIVE
```

### A.4 feature.yaml

```yaml
feature: <slug>
title: <一句话>
created: YYYY-MM-DD
status: active            # active | paused | merged | eol
owner_session: session-<uuid>   # 存 id，指向 role: owner 的条目

sessions:                 # 准入名单；不在名单只能只读
  - id: session-<uuid>    # 运行时唯一标识，写入后锁定
    label: <YYYY-MM-DD>-<slug>   # 仅用于命名 plan-<label>.md
    role: owner           # owner | member
    status: approved      # approved | pending | revoked
    joined: YYYY-MM-DD
    approved_by: user

git:
  is_repo: true
  repo_root: .
  branch: main
  base: main
  worktree: null
  upstream: null
  release_line: null
  version: null
  tags: []

change:                   # 本次变更基线，写计划前必须填完（§0.2）
  source_ref: main        # 基于哪个 tag / 分支
  source_version: null    # 该 ref 的版本
  new_branch: null        # 需要新开分支时填，如 hotfix/1.0.1
  release_line_action: none   # none | create | reuse
  version_bump: none      # none | patch | minor | major
  target_version: null    # 变更后的版本号
```

## 附录 B · 本项目本地化

- 项目：XMAutoTool（纯客户端模组，Forge 1.18.2）。
- 根目录：`D:\Project\xmautotool`。
- 技术栈：Java 17、Gradle wrapper、ForgeGradle `[6.0,6.2)`；Minecraft 1.18.2 / Forge 40.3.11；mod id `xmautotool`，版本 `0.1.0`；包前缀 `space.xiami.project.mc.mod.xmautotool`；MIT。
- 源码分层：`core/`（纯 JVM 策略引擎，不依赖 Minecraft）、`config/`（Profile 读写与运行时策略）、`platform/`（Forge/原版适配）、`gui/`（自绘 Screen 与控件）；纯 JVM 测试在 `src/test/java/.../core/`。
- 构建与测试（JDK 17 + 代理 + 仓库内 `GRADLE_USER_HOME`）：
```powershell
$env:JAVA_HOME = 'D:\Tool\Java\jdk-17.0.4+8'
$env:GRADLE_USER_HOME = "$PWD\.gradle-home"
$env:GRADLE_OPTS = '-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890 -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890'
.\gradlew.bat build --no-daemon
.\gradlew.bat test --no-daemon
```
- 文档：根目录 `PLAN.md` 保留为设计稿与 append-only 变更台账（§15），不受 `docs/` 根限制；`README.md` 同步维护。
- 版本控制现状：已有 git 仓库，`main` 分支，初始提交 `9b7944c`；未配置 remote。