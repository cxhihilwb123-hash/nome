# Nome client development authority

These instructions apply to the whole client repository.

## Required development entry

- The only long-lived Nome client development worktree is
  `/Users/forkman03/project/nome/nome-client`.
- The client authority branch is `codex/nome-v656-unified`. New client work must
  start from this branch or a verified descendant of its accepted merge
  `124863fe607eb84c177f8ccce7c76793f1ceeb55`.
- Core, Android, iOS, macOS Desktop, and future Windows Desktop are modules in
  this one Git history. Do not create permanent platform authority worktrees.
- The Website/control-plane authority is the separate repository
  `/Users/forkman03/project/nome/website` on `main`.

Before changing client source, read `plans/README.md` and verify:

```sh
test "$(git rev-parse --show-toplevel)" = "/Users/forkman03/project/nome/nome-client"
git status --short --branch
git merge-base --is-ancestor 124863fe607eb84c177f8ccce7c76793f1ceeb55 HEAD
```

Do not proceed from another checkout merely because it contains newer-looking
or dirty files. If the ancestry check fails, return to the unified authority or
create the intended branch from it.

## Retained recovery worktrees

The following paths are recovery/evidence sources, not development authorities:

- `/Users/forkman03/project/nome/simplex-chat`
- `/Users/forkman03/project/nome/simplex-chat-ios-consolidated`
- `/Users/forkman03/project/nome/simplex-chat-ios-integration`

Do not edit, build release artifacts from, merge in place, reset, clean, stash,
or delete these worktrees. Their dirty or process-occupied state is preserved
intentionally. Consult the consolidation records before recovering any content.

## Working rules

- Start ordinary client branches from `codex/nome-v656-unified`.
- Use a temporary worktree only for a release, hotfix, or large isolated
  migration. Give it a specific purpose and remove it only after its cleanup
  gate and explicit deletion authorization.
- Preserve unrelated changes. Stage exact paths only; never use bulk staging.
- Run tests appropriate to every affected module and record the source commit,
  commands, results, and artifact hashes when producing a build.
- Use `scripts/release/check-nome-build-source.sh` and
  `plans/builds/TEMPLATE.md` for formal build provenance.
- Do not use reset, Git clean, stash, history-rewriting rebase, worktree prune,
  force push, backup deletion, or unapproved file/worktree deletion.
- Push, tag, deployment, production mutation, TestFlight/App Store action, and
  public release require separate explicit authorization.

## Canonical records

- Current authority index: `plans/README.md`
- Unified client acceptance:
  `plans/consolidation/20260729_unified_client_execution_record.md`
- Safe cleanup boundary:
  `plans/consolidation/20260729_authorized_worktree_cache_cleanup_record.md`
- Copy-paste entry for a new task:
  `plans/20260729_nome_next_thread_development_entry.md`
