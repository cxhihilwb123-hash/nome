# Nome Android Milestone 3 Verification

Date: 2026-07-19
Branch: `codex/nome-android-v656`
Checkpoint parent: `415375bdc8b7c392d5855f7b8fe13bf3ceddb01a`
Scope: all locally ready Android connection, conversation, identity, settings, archive/migration,
remote-unpaired, and Android-reachable P1 presentation/lifecycle surfaces accumulated after the
Milestone 2 checkpoint

## Verdict

The concentrated Milestone 3 non-producer matrix passes. The current tree compiles, packages,
passes its Android/Desktop regression suites, passes the API 28/API 33/API 35 device suites,
retains the historical evidence manifests, and restores the production client after the
language/theme/scaling/TalkBack/privacy checks.

This verdict does not promote an unavailable external result:

- P11 produced and persisted one real one-time invitation, but a later bounded attempt again
  returned the official `smp12.simplex.im` connection error. The created row is only a pending
  invitation; it is not peer use or a connection. Because the ready bearer screen was not retained
  safely at the reference viewport, P11 page-level ready-state visual acceptance remains open.
- P14 reached the official request confirmation on a second API 35 client, then returned the
  official `smp9.simplex.im` network error. No incoming request appeared on the source client.
- P15 produced a real reusable address, persisted across an emulator disk reboot, and its redacted
  API 35 production/reference comparison passes. The route does not claim that the address was
  shared or used.
- P16 submitted one real controlled group invitation, but the second client received no invitation.
- P17 created a new sender-side text item, but the peer did not receive it. Real transfer receipt,
  download/save/playback, incoming ringing, and a connected call remain unproven.
- P20 again returned the official `smp12.simplex.im` connection error before public-channel
  creation. No link or delete result exists, and therefore P21 still has no real public-channel
  producer.
- Remote desktop remains genuinely unpaired. No desktop QR producer exists for pair/switch/revoke
  or connected-secret-clearing proof.

These are external producer gates, not substituted with fixtures or inferred success. They remain
open for the final release-candidate gate.

## Build and host regression

The final aggregate Android invocation after the Milestone 3 source corrections was:

```text
./gradlew --max-workers=1 :android:testDebugUnitTest :android:lintRelease :android:assembleDebug \
  :android:assembleDebugAndroidTest :android:assembleRelease
```

The final post-lifecycle-fix Android run returned `BUILD SUCCESSFUL in 2m 54s`, 196 tasks.

- Android JVM tests: 42 tests, 0 failures, 0 errors, 0 skipped.
- Release lint: 0 errors and 68 warnings. The warnings are the existing dependency/resource/
  deprecation set plus a redundant activity label; no warning is represented as a clean
  distribution audit.
- Desktop regression then ran separately with `--max-workers=1` and passed in 52s; 35 tests, 0
  failures, 0 errors, 0 skipped.
- The corrections before the device matrix included the intended `DESTRUCTIVE_SECONDARY`
  foundation contract, approved accent ownership for Material secondary colors, and early visible
  busy/failure status on the Android public-address route.
- The first attempt to run Android and Desktop compilation concurrently after the later lock/modal
  isolation correction exhausted the Kotlin daemon and raced Desktop resource generation. It
  returned an Android compiler OOM plus missing generated `NotoColorEmoji` symbols. Stopping the
  daemon and running Android, then Desktop, serially closed both failures with the green results
  above; no source workaround or dependency change was made.

## Device suites

The instrumentation APK was installed with the production debug APK and invoked through the
runner declared by `pm list instrumentation`. API 28 and API 33 excluded only the five
API-35-sized screenshot classes; API 35 ran the complete suite including them.

| Device | Result | Duration | Coverage boundary |
|---|---:|---:|---|
| API 28 arm64 | `OK (67 tests)` | 282.987s | focused policies, real route/platform/lifecycle depth; API-35 screenshot classes excluded |
| API 33 arm64 | `OK (67 tests)` | 123.356s | focused policies, route/platform/lifecycle regression; API-35 screenshot classes excluded |
| API 35 arm64 | `OK (72 tests)` | 294.049s | complete suite, including Foundation, Home, connection preview, database root, and connection-management screenshot matrices |

The API 28 focused Foundation plus public-contact-method rerun passed 8 tests after the two
Milestone 3 source corrections.

## Production language, theme, scale, and accessibility

The populated API 35 production client was installed from the final debug APK and cold launched.
All eight Home combinations retained the production Nome title, Home hierarchy, Search action,
and New connection action:

| Language | Theme | Font scale |
|---|---|---:|
| Simplified Chinese | light | 100% |
| Simplified Chinese | light | 200% |
| Simplified Chinese | dark | 100% |
| Simplified Chinese | dark | 200% |
| English | light | 100% |
| English | light | 200% |
| English | dark | 100% |
| English | dark | 200% |

English and Chinese were selected through the official Appearance route rather than an unsupported
locale override. Cleanup restored Simplified Chinese, light theme, font scale 1.0, and an empty
system per-app locale.

Real TalkBack was bound as
`com.google.android.marvin.talkback/.TalkBackService` with touch exploration. A real
touch-exploration/double-tap activation opened production Search; another representative
activation reached the official create-private-group route without submitting a destructive
action. Instrumentation separately covers the affected semantic labels and 48dp actions. Cleanup
disabled the service and touch exploration, restored font/theme/language, and returned the
temporary notification permission to its original ungranted sensitive state.

The concentrated locked/deferred lifecycle used a real, temporary local app passcode on the
controlled API 35 client and a cold `ACTION_SEND` text intent. The first check exposed underlying
fullscreen-profile semantics while the app was unauthorized. `MainScreen` already isolated the
primary content, but the independent fullscreen modal host sat outside that semantic boundary.
The Android modal/switching-user host is now also `clearAndSetSemantics` while unauthorized.

After installing the corrected APK, the cold share intent presented only the passcode owner. A
wrong passcode stayed fail-closed. The lock page contained no profile-page, shared-text, contact,
or share-list node. Entering the correct temporary passcode restored the deferred official Share
list with the controlled contacts and note target. Back cleared the shared content, and the
official Privacy and security route disabled the temporary app lock again. Screenshot protection
remained enabled.

## Visual acceptance and sensitive-state handling

All ordinary families retain their accepted API 35 production/reference comparisons recorded in
the completion ledger and page matrix. The newly available P15 READY state was inspected at the
Chinese/light/reference viewport against the P15 visual acceptance baseline:

- production, redacted:
  `visual-baselines/api35-zh-light/P15-production-ready-390x844-redacted-v1.png`,
  SHA-256 `eb9c04445d533297cc018e3be3c05c086f8fc4ac81764f5febc8badfb54830ca`;
- same-size reference/production comparison, redacted:
  `visual-baselines/api35-zh-light/P15-reference-vs-production-ready-780x844-redacted-v1.png`,
  SHA-256 `40eb92f0c5ba70fe7c4dd25c033f6502f018d3ffea472e078c8b760426c76777`.

The comparison retains the baseline composition, QR/link card, copy/share actions, confirmation
state, address-management hierarchy, destructive styling, advanced settings, disclosure, density,
and action sizing. The real bearer and QR are replaced with neutral blocks before entering the
evidence tree. No raw bearer image, clipboard value, UI dump, log, or filename is retained.

Production screenshot protection was disabled only through the official Privacy and security
screen for the bounded redaction capture, then immediately restored. The final switch is checked;
the restored ADB screencap has one color and mean 0.25.

## Historical evidence manifests

The frozen manifests were read from the P13 checkpoint object and rehashed without checkout or
modification:

| Evidence root | Result |
|---|---:|
| Phase 1 | 48/48 |
| Phase 2 foundation | 131/131 |
| Phase 2 Batch 2 | 254/254 |
| P13 | 139/139 |

## Unsigned artifacts and release isolation

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| `android-arm64-v8a-debug.apk` | 343236443 | `7a4fb5f1f0bd6a037b0d97f49dd64a64b00e24b536eed106bfa49a40d13bafe3` |
| `android-armeabi-v7a-debug.apk` | 329177547 | `d533d18d54e866bfb4587550df43250196690b0cef9be404704673a877919f00` |
| `android-debug-androidTest.apk` | 3206135 | `9f4af82bf5f710014a0e9a29681c4256070d0a505883524c7a797c1d84c9809e` |
| `android-arm64-v8a-release-unsigned.apk` | 304213436 | `490e99abba485644e59d7047001c5aad10c0d370f8ca8b2177d7088cb4e52f9c` |
| `android-armeabi-v7a-release-unsigned.apk` | 290154540 | `42e7c8886146ce4403b5f010f0ed15a22c4e26b951a64f6ae376a9a7befd7fa4` |

Release badging reports package `chat.simplex.app`, version code 358, version name 6.5.6, minimum
API 28, target API 35, label Nome, and the expected ABI. Both release APKs have 1724 ZIP entries.
The filename scan found no evidence/test/fixture/private-key artifact, and the DEX scan found no
workspace/evidence path, bearer marker, GitHub marker, or evidence activity. The packaged font
notice matches the source SHA-256
`7c20a5c56fba5d9b9a1a02a600ba9f6cbb2f408eee56646c2303b465634d8dd2`.

`apksigner verify` returns `DOES NOT VERIFY` with missing `META-INF/MANIFEST.MF`, which is the
expected result for these explicitly unsigned local artifacts. It is not a signing success.
The distribution verdict remains `NOT FOR DISTRIBUTION`; application identity, verified-link
ownership, production signer, publisher policy/store material, and exact-build public source URL
remain unresolved external release inputs.

## Milestone checkpoint boundary

The Milestone 3 checkpoint may be created only after the final dirty-scope, forbidden-path,
sensitive-text, conflict, staged-scope, and diff checks are green. The retained unknown
`apps/multiplatform/Screenshot_1784275771.png` is excluded and untouched. The checkpoint does not
close the producer gates listed above and does not authorize push, publication, production signing,
domain/store mutation, or real-user-data action.
