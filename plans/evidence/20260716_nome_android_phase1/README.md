# Nome Android Phase 1 evidence

This directory records the unchanged official SimpleX Chat `v6.5.6` Android baseline running as the isolated debug package `chat.simplex.app.nome.dev`. It is a pre-Nome baseline and capability gate, not a claim that the approved Nome UI has been implemented.

## Runtime result

- Git source: tag `v6.5.6`, commit `59fce95d3cd08897b4ef742447b785cf2e56c7ce`.
- Build package: `chat.simplex.app.nome.dev`, label `Nome Dev`, version `6.5.6` (`358`), minSdk 26, targetSdk 35.
- Original clean launch: `LaunchState: COLD`, `TotalTime: 1738 ms`, `Status: ok`.
- Post-forced-Gradle-rerun clean launch: `LaunchState: COLD`, `TotalTime: 1505 ms`, `Status: ok`.
- Real native/core trace: `libapp-lib.so` loaded; the SimpleX server and receiver started; the Android network observer reported Wi-Fi online; the scoped process log contains no fatal exception, ANR, or native-load failure.
- Real state mutation: official onboarding created synthetic local user `NomeBaselineQA`, accepted the official network conditions, and reached the real empty connection surface.
- Reinstall/upgrade-fixture check: the newly assembled arm64 APK installed onto the non-empty AVD without clearing data; cold launch returned directly to the Chinese dark empty-user page and real core startup succeeded. Ninja had reused current native objects, so this is not a fresh-native-build claim.
- Minimum-version check: the local unchanged-source debug APK and the official signed v6.5.6 arm64 APK both fail on the dedicated API 26 AVD with the same `getentropy` `UnsatisfiedLinkError`. The NDK 23 header marks that symbol as introduced in API 28, and both release ABIs contain the unresolved reference.
- Screenshot boundary: official v6.5.6 enables Android `FLAG_SECURE` by default. The first capture is intentionally black. Subsequent captures changed only the isolated app's `PrivacyProtectScreen` runtime preference; source and APK bytes were unchanged.

## Reproducible local fixture

The API 35 AVD was moved out of `/tmp` and restored successfully from:

`/Users/forkman03/project/nome/simplex-chat/apps/multiplatform/.nome-local/api35-avd`

The directory is intentionally Git-ignored. Current physical sizes after the final snapshot refresh:

- complete stable AVD directory after the verified reinstall and clean shutdown: 7.8 GiB;
- `nome-v656-nonempty-baseline` snapshot directory: 2.5 GiB;
- snapshot `ram.bin`: 2,636,092,098 bytes.

The exact official APKs and detached signature files are retained under the ignored `apps/multiplatform/.nome-local/artifacts/v6.5.6/` directory. `artifact-provenance.txt` records URLs, hashes, stable absolute paths, extracted-library destinations, and the PGP verification boundary.

## Build and install evidence

| File | Result |
|---|---|
| `gradle-phase1-verification.log` | Current normal verification PASS; many tasks were already up-to-date. |
| `gradle-phase1-verification-rerun.log` | Deliberately retained failed attempt: concurrent Desktop + Android forced compilation exceeded the repository's default 2 GiB Kotlin daemon limit. This is infrastructure evidence, not a source-test pass. |
| `gradle-desktopTest-rerun.log` | Authoritative low-memory forced rerun: `desktopTest` PASS, 12 of 13 tasks executed. |
| `gradle-android-rerun.log` | Authoritative low-memory forced rerun: Android unit test, lint, both ABI CMake Gradle tasks, and assemble PASS; 101 of 102 tasks executed. Ninja reused current native objects, so this is not claimed as a fresh native compilation. |
| `native-task-reuse-boundary.txt` | Captured Gradle task lines and `.ninja_log` mtimes that bound the claim to task execution with reused native objects. |
| `gradle-android-install-api35.log` | Newly assembled arm64 split installed on the one online API 35 AVD; PASS. |
| `api35-post-rebuild-cold-start.txt` | Reinstalled non-empty fixture cold launch; PASS. |
| `api35-post-rebuild-process-logcat.txt` | Raw scoped process log proving native wrapper and real SimpleX server/receiver startup. |
| `api35-post-rebuild-ui.xml` | Native UI tree proving the persisted non-empty Chinese state after reinstall. |

Existing upstream Kotlin/compiler, resource, and SDK XML warnings are retained in the raw logs. This evidence set has no fresh C compiler trace and makes no C-warning-retention claim. No production source was modified to silence warnings.

## Artifact and device evidence

- `artifact-provenance.txt`: exact release URLs, hashes, stable paths, destinations, and verification boundary.
- `apksigner-official-*.txt`: raw APK v2/v3 signature and signer-certificate output.
- `aapt-*.txt`: raw official and local APK package/version/SDK/ABI badging.
- `elf-*.txt`: raw per-ABI ELF header, notes, SONAME, `NEEDED`, and `RUNPATH` output.
- `getentropy-*.txt` and `ndk23-getentropy-api-level.txt`: both ABI symbol references and API-level declaration.
- `api35-device-getprop.txt` and `api26-device-getprop.txt`: raw emulator build/device fingerprints and properties.
- `api26-official-v656-crash-logcat.txt`: raw official signed APK crash.
- `api26-local-debug-start.txt` and `api26-local-debug-crash-logcat.txt`: raw unchanged-source local APK launch and crash.

## Native screenshot and accessibility matrix

| Evidence family | Purpose |
|---|---|
| `nome-v656-cold-start-flag-secure-black.png` | Records the official default screenshot-protection behavior; it is not the sole proof of why the screen is black. |
| `nome-v656-welcome-light-en.png` + XML | Official welcome, English/light, before Nome. |
| `nome-v656-home-empty-light-en.png` + XML | Real local user, English/light. |
| `nome-v656-home-empty-dark-en.png` + XML | Real local user, English/dark. |
| `nome-v656-home-empty-light-zh-CN.png` + XML | Real local user, Simplified Chinese/light. |
| `nome-v656-home-empty-dark-zh-CN.png` + XML | Real local user, Simplified Chinese/dark, recaptured after full app stop and Chinese cold start. |
| `native-screenshot-comparison.md` | Side-by-side P03/P08 approved reference versus native baseline, plus all four language/theme quadrants. |

The final four home UI trees each contain six clickable nodes; all six have a non-empty accessible name either on the node or a descendant. Each tree has four focusable nodes. The final Chinese dark tree uses `返回`, `资料图片占位符`, and `开始新聊天`; the earlier mixed-language capture was replaced. This is a baseline semantic-tree check, not a substitute for later TalkBack traversal, 200% font, contrast, and 48dp target gates.

## Integrity and privacy

`SHA256SUMS` is regenerated after the final evidence change and is the complete checksum manifest for every file in this directory other than the manifest itself. No database key, passphrase, token, invitation/contact link, private key, or real personal data is stored here. Emulator ADB public/build properties are not private credentials.
