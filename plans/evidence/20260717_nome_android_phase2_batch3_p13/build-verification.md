# P13 build and static verification

Date: 2026-07-17 (Asia/Shanghai)

## Final Gradle gates

Android command scope:

```text
:android:compileDebugKotlin
:android:testDebugUnitTest
:android:lintDebug
:android:assembleDebug
:android:assembleDebugAndroidTest
:android:assembleRelease
```

Final post-cleanup result after the focus-restoration fix:
`BUILD SUCCESSFUL in 2m 32s` (214 tasks, 71 executed, 143 up-to-date).

Android JVM unit report: 22 tests, 0 failures, 0 errors.
Desktop command: `:common:desktopTest`.
Desktop result after the same fixes: `BUILD SUCCESSFUL in 27s`; report contains 21 tests, 0
failures, 0 errors.

The final lint report contains no P13 route, seam, resource, `ConnectPlan`, or `SimpleXAPI` issue.
Repository-wide pre-existing dependency/deprecation warnings remain outside this bounded batch.

## Device instrumentation

- API 28 focused Compose + packaging: `OK (4 tests)`.
- API 35 focused Compose + packaging: `OK (4 tests)`.
- API 35 screenshot matrix: `OK (1 test)`, 72 PNGs.

After the controlled production-reachability session, the one-shot local network harness source
was removed and `:android:assembleDebugAndroidTest` was run again. The rebuilt standard test APK
contains none of the harness class or argument strings, and the focused Compose + packaging suite
again passed `OK (4 tests)` on both API 28 and API 35.

That post-session APK hashes to
`b8265d583deda07d5befe677397906e0828d3faebb33b74ad34be4e406163681`. It is a rebuilt,
non-frozen debug test package and is deliberately not substituted into the historical artifact
table below. The earlier recorded APK remains the artifact used for the original complete gate
run. No final Batch 3 manifest is created while acceptance gates remain open.

On 2026-07-18 the same cleanup was repeated after a one-shot contact-address acceptance harness:
the temporary source and app-cache bearer were removed, the standard APK returned to the same
`b8265d...` hash, its dex contained zero temporary harness strings, and the focused suite again
passed `OK (4 tests)` on API 28 and API 35.

The authoritative final cleanup removed all one-shot invitation/address/acceptance harnesses, the
TalkBack harness/TTS service and test manifest, the lifecycle receiver and manifest entry, three
known private-cache bearer files, and all temporary product trace logging. API 35 accessibility,
touch exploration, and default TTS were restored to their original disabled/null state, and the
temporary TalkBack notification permission was revoked.

The rebuilt standard test APK hashes to
`2d0dfdee303d957e835988d2f34ec8939c31c15d58ca3d166eaf01433d1d3bf5`.
Its manifest and dex have zero temporary P13 service/receiver/harness/trace strings. The same
standard focused suite passed `OK (4 tests)` on API 28 and API 35.

The complete post-cleanup API 35 screenshot matrix passed `OK (1 test)` in 182.595 seconds and
created 72/72 expected PNGs. Product-region comparison found 64 pixel-identical captures and eight
Connecting captures differing only in the indeterminate spinner phase; structured visual verdict:
99, `pass`.

## APK outputs

| Artifact | SHA-256 |
|---|---|
| `android-arm64-v8a-release-unsigned.apk` | `cdea7d2506fc306bb17627f22126210c0afa4703b5e535b76f0b6db884c2e87b` |
| `android-armeabi-v7a-release-unsigned.apk` | `36dea406f9aa2cd7b1a428764dd0fb1188fb48c0f712bae43f5896c9372eb0b8` |
| `android-arm64-v8a-debug.apk` | `050525c4728fd31d201689513284f36f4105ea5c1b2ebe20dd6e11868c91f140` |
| `android-armeabi-v7a-debug.apk` | `26603bf48aabfc70c11fe0815ed5926d6664ef2865694967cc43c787482efe9b` |
| `android-debug-androidTest.apk` | `2d0dfdee303d957e835988d2f34ec8939c31c15d58ca3d166eaf01433d1d3bf5` |

Release badging remains package `chat.simplex.app`, version `6.5.6` / 358, minSdk 28, targetSdk 35.

## Static gates

- production resources: 44 English names = 44 zh-CN names;
- debug fixture resources: 4 English names = 4 zh-CN names;
- platform seam: one common expect, one Android actual, one Desktop actual;
- Android fullscreen-modal stack mutation is dispatched to `Dispatchers.Main`; the legacy alert
  path retains its prior dispatch behavior;
- only `connectIfOpenedViaUri` opts into `ExternalActionView`; six other caller surfaces keep the
  legacy default;
- normalized branch policy: 21 exhaustive branches, exactly 7 eligible;
- no diff in `NomeHomeStateAdapter.kt`; `NomeHomeRoute.android.kt` has only the payload-free
  current-profile `FocusRequester` contract proven by real TalkBack;
- no diff in `Core.kt`, Haskell/native core, protocol/database/message-state-machine, or iOS paths;
- P13 artifact scan found no bearer-like URI/token pattern;
- current Gate G Markdown audit: 35 changed files, 552 non-web local links, and 402 `#L`
  line-number anchors; 0 missing targets or out-of-range references;
- current `git diff --cached --check`: PASS;
- current index/worktree: 140 staged paths, 0 unstaged tracked paths, and one excluded unknown
  screenshot;
- lint: 45 repository warnings, 0 errors, 0 P13-scope findings;
- current staged-text secret scan: 65 files, 0 long URI/private-key/service-token/bearer-header
  matches.

Historical Phase 1, Foundation, and Batch 2 `SHA256SUMS` manifests were each rechecked successfully
without modifying their evidence roots.

## Toolchain note

Android and Desktop gates were run as separate Gradle invocations. A previously attempted combined
Android+Desktop invocation can expose the repository's generated-resource ordering around
`cleanSimplexAssets`; the separate authoritative invocations above both pass.

An earlier up-to-date Android test APK omitted the newly compiled P13 test dex. The Android module's
generated outputs were cleaned and rebuilt, `apkanalyzer` then confirmed the P13 test classes in the
APK, and the rebuilt package passed on API 28 and API 35. A subsequent class-filter attempt initially
targeted the unrelated legacy package `chat.simplex.app.test`; the isolated P13 packages are
`chat.simplex.app.nome.dev` and `chat.simplex.app.nome.dev.test`. Re-running against the correct
instrumentation package passed. Neither event was a product assertion failure.
