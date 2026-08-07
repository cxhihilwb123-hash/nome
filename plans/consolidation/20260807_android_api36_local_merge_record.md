# Android API 36 local unified merge record

Date: 2026-08-07 (Asia/Shanghai)

## Scope

This record closes the local merge of the short-lived Android API 36 migration
line into the unified Nome client authority. It does not authorize or claim a
remote push, tag, production build, store upload or public release.

## Merge identity

- Target branch: `codex/nome-v656-unified`
- Previous target: `7e94b5512c8dd6c1362a5c79e9deb6b6b11b5ea7`
- Source branch: `codex/nome-android-api36-20260807`
- Accepted source head: `9c5ae7afb82424ad86f68d2a823c6fa6580d359a`
- Implementation source: `ae23430762157cf38945e4ca3b0ab24c5ea06933`
- Merge commit: `60cb74fc401bfd4cc839e9ae472a7595626c007d`
- Merge method: ordinary non-fast-forward merge with the `ort` strategy
- Conflicts: none

The source branch was a verified descendant of the previous unified target.
No force operation, rebase, reset, stash, cleanup or history rewrite was used.

## Accepted changes

- Android application `compileSdk` and `targetSdk`: 35 to 36
- Android Gradle Plugin: 8.7.0 to 8.9.1
- Kotlin and Compose compiler plugin line: 2.1.20 to 2.2.10
- Deprecated `kotlinOptions` configuration migrated to `compilerOptions`
- API 36 build and runtime evidence added to the canonical project index

Gradle 8.12, Compose 1.8.2, Android min SDK 28 and application version
6.5.6/build 373 remain unchanged.

## Verification accepted at merge

The detailed commands, configuration fingerprints, artifact hashes and signer
certificate are recorded in
[`../builds/20260807_nome_android_6.5.6_code373_api36_debug.md`](../builds/20260807_nome_android_6.5.6_code373_api36_debug.md).

Accepted gates:

- Gradle configuration: PASS
- Android arm64-v8a and armeabi-v7a Debug assembly: PASS
- Android unit tests: PASS
- Android lint: PASS
- Shared Android Kotlin compilation: PASS
- Shared Desktop tests: PASS
- APK compile/target SDK 36 metadata and Debug signature: PASS
- Isolated Android 16 fresh install and onboarding: PASS
- Android 16 notification and foreground-service behavior: PASS
- Background retention and force-stop cold relaunch: PASS
- Same-signed API 35 to API 36 preserved-data replacement: PASS
- Android 16 reboot service and identity recovery: PASS

The application sets `FLAG_SECURE`, so runtime state was verified through
Android UI Automator, package/service/notification state and logs rather than
ordinary screenshots. The inspected runtime logs contained no application
crash, ANR, native abort, link error, SQLite exception or migration failure.

## Remaining boundaries

- Two-client invitation delivery and messaging: NOT RUN
- Channel create/join/presentation: NOT RUN
- Camera/microphone permission and calls: NOT RUN
- Backup import and reinstall recovery: NOT RUN
- Production signing and release assembly: NOT RUN
- GitHub/LAN push of the merged unified branch: completed after separate
  explicit authorization; both remotes read back at
  `769e4058ba5df86fdb9bd36f2f002eca65626e5c`
- Tag, deployment, store upload and public release: not performed

Existing SDK XML, resource-format, deprecated API, unresolved opt-in marker,
Manifest and native C pointer-type warnings remain follow-up cleanup work; none
failed the accepted build or runtime gates.

## Authorized publication addendum

After the local merge and documentation closure, the user separately
authorized remote synchronization. The exact unified ref
`codex/nome-v656-unified` was ordinarily fast-forwarded from
`7e94b5512c8dd6c1362a5c79e9deb6b6b11b5ea7` to
`769e4058ba5df86fdb9bd36f2f002eca65626e5c` on both:

- GitHub: `https://github.com/cxhihilwb123-hash/nome.git`
- LAN Gitea: `gitea-lan:forkman/nome.git`

Both remote refs were read back at the expected commit after the push. The
short-lived migration branch, tags and artifacts were not pushed, and the
official SimpleX `origin` remote was not changed.
