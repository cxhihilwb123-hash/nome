# Nome next-thread development entry

Updated: 2026-07-29 (Asia/Shanghai)

Use this file when starting any new Nome client development task. It points to
the current authority after repository consolidation; older dated handoffs are
historical evidence and must not override it.

## Copy-paste prompt

```text
继续开发 Nome 客户端。唯一客户端权威工作树是
/Users/forkman03/project/nome/nome-client，权威基线是
codex/nome-v656-unified；Core、Android、iOS、macOS Desktop 和未来 Windows
Desktop 都在这一条 Git 主线内。开始前完整阅读仓库根目录 AGENTS.md 和
plans/README.md，先只读核验仓库根目录、git status，并确认当前 HEAD 包含
统一合并提交 124863fe607eb84c177f8ccce7c76793f1ceeb55，再执行本次开发。

不要在 simplex-chat、simplex-chat-ios-consolidated 或
simplex-chat-ios-integration 中继续开发；它们只是保留的恢复/证据工作树。
不要 reset、clean、stash、批量 add、改写历史、prune、强推、删除备份或未经
授权删除文件/worktree。只暂存本次任务的明确路径，按受影响平台运行测试并
记录提交和证据。Website 必须在独立仓库
/Users/forkman03/project/nome/website 的 main 主线上单独处理。远端 push、tag、
部署、生产变更、TestFlight/App Store 或公开发布必须取得单独明确授权。
```

## Start-of-task verification

Run these checks from the client repository before editing:

```sh
cd /Users/forkman03/project/nome/nome-client
test "$(git rev-parse --show-toplevel)" = "/Users/forkman03/project/nome/nome-client"
git status --short --branch
git merge-base --is-ancestor 124863fe607eb84c177f8ccce7c76793f1ceeb55 HEAD
```

The ancestry command must exit successfully. A feature branch may advance past
the recorded tip, but it must descend from the unified client history. If the
checkout already contains unrelated changes, preserve them and isolate the new
task instead of resetting, cleaning, stashing, or broadly staging the tree.

## Repository boundary

| Scope | Development authority |
|---|---|
| Core, Android, iOS, macOS Desktop, Windows Desktop source | `/Users/forkman03/project/nome/nome-client` |
| Website and invitation control plane | `/Users/forkman03/project/nome/website` |
| Historical dirty or diagnostic material | Verified backups and retained recovery worktrees; read-only unless recovery is explicitly requested |

For the current branch SHA, verification history, retained-worktree reasons,
and authorization boundary, always use `plans/README.md` rather than copying an
older SHA from this prompt.
