# Phase 1 iOS consolidation execution record

Date: 2026-07-29 (Asia/Shanghai)

## Verdict

**PASS.** The clean iOS authority branch is
`codex/nome-v656-consolidated-ios`; its Phase 1 evidence commit is recorded
below. Existing dirty integration and build 374 source trees were not edited.
No backup was deleted, and no merge, rebase, reset, clean, stash, push,
deployment or publication was performed.

## Authority and ancestry

- Start: `5dbf8ced68bbf6de286a04ae8189829ce5afb799`.
- Integration ancestor `e50f32fcec027029c95d4556cceaf600f8a83edb`: PASS.
- Activation ancestor `46cc7632691c51d5469f8a00304d7475a47b1a1b`: PASS.
- Core call-reject ancestor `72c6b213c02565829b33d80c35bbd9594f263890`: PASS.
- Legacy-iOS call-reject head `5dbf8ced68bbf6de286a04ae8189829ce5afb799`: PASS.
- `NomeActivation.swift` and `NomeActivationPolicy.swift`: present in Git.
- Incoming reject control and the invitation `apiEndCall` fallback: present in
  Git and compiled by the clean arm64 build.

## Responsibility commits

| Responsibility | Commit |
|---|---|
| A: server and build configuration | `a10346f5eb3d70a02ea4f0830c89440c8bbd62f6` |
| B: call runtime and diagnostics | `c077f59dfca52a59689da5fd885a59c4d902bc43` |
| C: channel UI and Chinese presentation | `9d0a52b84beebedab126b791ae71352bd94b768f` |
| Build 372/373/374 provenance and build 349 record | `35110880452f6b8618f5e9fa6c4be1955243bc9c` |

Each responsibility group was applied from its own binary patch and committed
separately. Overlapping files were resolved semantically; no whole temporary
file replaced `ChatView.swift`, `SwipeLabel.swift` or `SimpleXAPI.swift`.

## Build 374 recovery decision

Phase 0 proved 22 tracked build 374 changes matched the preserved integration
delta. The remaining five temporary-only files were signing/container/Keychain
adaptations that remove or bypass activation dependencies. Applied: **0 of 5**.
All five remain recoverable from the verified Phase 0B archive.

## Tests and acceptance

| Gate | Result | Evidence |
|---|---|---|
| `git diff --check` | PASS | final clean-worktree check |
| Activation policy reducer | PASS | 28/28 after A, B and C |
| arm64 Simulator clean build | PASS | clean and build succeeded |
| Activation no endpoint | PASS | 1 XCTest, 0 failures |
| Activation disabled | PASS | normal preview send, 1 XCTest, 0 failures |
| Activation enforced | PASS | blocked, redeemed, sent and persisted after relaunch; 1 XCTest, 0 failures |
| Two-party call rejection | PASS | Alice invited Bob with a WebRTC video call; Bob rejected; Alice received `call with bob ended`; 14 matched examples, 0 failures |
| Channel create/join/delivery/count | PASS | single- and multi-relay cases; 2 examples, 0 failures |
| Channel list/header/Chinese/member surfaces | PASS | current source assertions and clean arm64 build, plus preserved physical build 373/374 UI evidence from the same channel delta |
| Consolidated artifact identity | PASS | build 349 Debug Simulator ZIP and SHA-256 recorded |

Channel Core acceptance creates a channel, prepares its links, joins multiple
subscribers, delivers messages and verifies the public subscriber count. The
current Git source separately asserts the channel list filter/badge path,
channel header/top-card copy, Chinese labels, subscriber count and member-list
navigation. Physical build 373/374 records prove those same UI surfaces were
displayed on an iPhone; their unknown historical Git SHA remains explicitly
unknown and is not attached to the new artifact.

## RealCore simulator diagnostic boundary

An attempted two-simulator UI call diagnostic was not used as acceptance
evidence. Xcode first skipped the environment-gated test; the corrected attempt
then showed both named RealCore simulators at first-run onboarding because their
old identities/databases were absent. That is a missing fixture precondition,
not a call-reject product result. The experimental test addition was removed,
the attempt logs were retained and hashed, and the repository's deterministic
two-Chat Core regression supplied the two-party behavior proof.

## Artifact

- Source commit: `9d0a52b84beebedab126b791ae71352bd94b768f`.
- File: `Nome-iOS-6.5.6-build349-20260729-Debug-Simulator.app.zip`.
- SHA-256: `4678125a45cb92efa2c1c059cb9435167259eea2f3a441564638884a1cbe22e6`.
- Boundary: internal arm64 Simulator Debug package; not an IPA or public build.

## Evidence root

`/Users/forkman03/project/nome/consolidation-evidence/20260729_phase1_ios`

The packet contains `SHA256SUMS`; raw failed/skipped attempts are named as
non-acceptance diagnostics in its `README.md`.
