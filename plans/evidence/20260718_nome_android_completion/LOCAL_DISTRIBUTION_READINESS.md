# Nome Android Local Distribution Readiness Audit

Date: 2026-07-18
Branch: `codex/nome-android-v656`
Audited HEAD: `415375bdc8b7c392d5855f7b8fe13bf3ceddb01a`
Scope: local Android source and unsigned artifacts only
Verdict: **NOT FOR DISTRIBUTION**

This is a technical readiness record, not legal advice. It does not authorize a remote push,
store submission, public release, production signing, or use of real user data.

## Decision

The current tree can produce a local, non-debuggable, unsigned Android release artifact carrying
the Nome visible name and approved Nome launcher/in-app brand assets. That artifact is suitable
only as an internal release-candidate input.

Public distribution remains prohibited because all of the following are still unresolved outside
the authorized local implementation scope:

1. the release application ID is still the official upstream `chat.simplex.app`;
2. the verified-link manifest still declares SimpleX-controlled domains, but no Nome-owned domain,
   deployed `assetlinks.json`, production package decision, or production signer certificate
   exists;
3. no production signing material was generated or used;
4. no approved Nome publisher/legal identity, support endpoint, privacy policy, terms, public
   corresponding-source URL, or store-account declarations exist;
5. the checked-in Fastlane listing is the official SimpleX listing and is explicitly excluded;
6. the final relay/operator defaults and public compatibility wording require an owner decision;
7. the final device/accessibility/regression gates and the two final same-summary `ZERO ISSUES`
   reviews have not run.

No percentage, restoration, uptime, security, privacy, online, timeout, or success claim is
inferred from this audit.

## Reproducible artifacts

The release build completed locally with Android Studio JBR 21 and Gradle 8.12. A transient Kotlin
incremental-cache failure initially reported an unresolved `NomeProductionShell`; rerunning
`:android:compileReleaseKotlin --rerun-tasks` completed, and the following normal
`:android:assembleRelease` completed without a source workaround.

| Artifact | SHA-256 | Classification |
| --- | --- | --- |
| `android-arm64-v8a-release-unsigned.apk` | `490e99abba485644e59d7047001c5aad10c0d370f8ca8b2177d7088cb4e52f9c` | internal, unsigned, not for distribution |
| `android-armeabi-v7a-release-unsigned.apk` | `42e7c8886146ce4403b5f010f0ed15a22c4e26b951a64f6ae376a9a7befd7fa4` | internal, unsigned, not for distribution |
| release `output-metadata.json` | `6d31f28db0f0384450d632489d11f9dd117d5a302adc6d2c6f09a8eaa4de65a3` | local build metadata |
| `android-arm64-v8a-debug.apk` | `7a4fb5f1f0bd6a037b0d97f49dd64a64b00e24b536eed106bfa49a40d13bafe3` | local debug verification only |

`aapt dump badging` reports package `chat.simplex.app`, version code `358`, version name `6.5.6`,
minimum SDK 28, target SDK 35, native code `arm64-v8a`, and application label `Nome` for every
packaged locale. The release manifest has no `debuggable=true` value. `apksigner verify` returns
`DOES NOT VERIFY` with `Missing META-INF/MANIFEST.MF`, which is the expected result for the
intentionally unsigned output and is not a distributable-signature claim.

The release APK contains `lib/arm64-v8a/libsimplex.so` and the expected production assets. A ZIP
name scan found the standard Kotlin coroutine `DebugProbesKt.bin` but no Nome debug activity,
fixture, test, probe, or command-counter artifact. A DEX package scan found the production
`NomeProductionShell` and no exact project debug-fixture/test-activity/probe/counter type.

## Application identity and manifest

| Field | Current release value | Audit result |
| --- | --- | --- |
| Application ID | `chat.simplex.app` | blocker: upstream identity; final coexist/upgrade decision absent |
| Label | `Nome` | pass for local RC |
| Version | code `358`, name `6.5.6` | factual upstream baseline |
| SDK | min 28, target 35, compile 35 | pass |
| Backup flags | `allowBackup=false`, `fullBackupOnly=false` | pass |
| Native extraction | `extractNativeLibs=false` | pass |
| Signing | none | expected locally; blocker for distribution |
| Compatibility scheme | `simplex:` | retained by the approved compatibility plan |
| Verified web links | upstream SimpleX/Flux hosts | blocker until an owned-domain decision is implemented |

The app-owned activities, services, notification receiver, file provider, and call receiver are
non-exported except for the launcher/deep-link activity and launcher aliases. The boot receiver is
exported for `BOOT_COMPLETED`. AndroidX WorkManager's exported job service is protected by
`android.permission.BIND_JOB_SERVICE`; AndroidX diagnostics/profile receivers are protected by
`android.permission.DUMP`.

Declared capability permissions are:

- camera and video capture for QR/video features;
- microphone and audio settings for voice messages and calls;
- notifications, vibration, wake lock, boot, full-screen intent, battery-optimization request,
  and foreground-service types for message/call lifecycle;
- Internet and network-state access for protocol transport;
- storage compatibility plus platform document/file flows for import, export, attachment, and
  archive operations;
- biometric/fingerprint APIs for local app authentication.

The permission list is a capability inventory, not proof that a capability is always used or that
any external service is available.

## App Links and domain ownership

The release manifest retains `android:autoVerify=true` for:

- `simplex.chat`;
- `smp4`–`smp12` and `smp14`–`smp19` under `simplex.im`;
- `smp1`–`smp6` under `simplexonflux.com`;
- paths `/invitation`, `/contact`, `/a`, `/c`, `/g`, and `/i`.

These are compatibility domains controlled by upstream operators, not proved Nome-owned domains.
Android verification requires each host to serve an HTTPS
`/.well-known/assetlinks.json` statement containing the final package name and production signer
SHA-256 certificate, without redirects. No final Nome package, signer, owned host, or deployed
statement exists, so verified links cannot be claimed. See the
[Android App Links association requirements](https://developer.android.com/training/app-links/configure-assetlinks)
and [App Links verification guidance](https://developer.android.com/training/app-links/test-applinks).

Before distribution, the owner must decide whether to:

1. use a new Nome application ID and Nome-controlled HTTPS hosts while retaining `simplex:` as a
   compatibility scheme; or
2. pursue an explicitly authorized upgrade/signature/domain arrangement.

The current local artifact implements neither decision.

## Brand and asset provenance

Approved brand inputs are outside the upstream source tree at
`/Users/forkman03/project/nome/design/design/brand/`.

| Asset group | Source/provenance | SHA-256 or count | Local status |
| --- | --- | --- | --- |
| canonical light app icon | approved `nome-app-icon-1024.png` | `a5acadcba5bdb7d474e9f2864c4b0032e657da1ec3de37111460f1bd3bdf3467` | launcher families generated mechanically |
| light lockup | approved `nome-logo.png` | `c7781d1154e734dccbcc2e5da76b89d1686052399b241435510af99c79b28f15` | startup/about/call/app brand |
| dark lockup | approved `nome-lockup-dark.png` | `4cc8486083ff7b6efc3beace4e039b830259f40436a31fe1278e28bf00a7db39` | dark startup/about/call/app brand |
| mark | approved `nome-mark.png` | `4c670ae25eac8ef34f9dcc6c125a00c806cb7182013d403888e00d803aa9dc48` | source for notification silhouette |
| dark launcher variant | deterministic crop/composition from approved dark lockup on approved navy `#0E1B2D` | derived, not a new canonical input | internal derivative |
| notification/service icon | deterministic solid monochrome outer silhouette from approved mark | derived, not hand-drawn | Android notification-compatible derivative |
| shared upstream UI images | `common/.../MR/images` | 294 files | inherited UI/protocol/operator resources; app-brand resources are Android-overlaid |
| placeholder default SVG assets | `common/.../assets/default/MR/images` | 38 files | transparent runtime placeholders; names may retain protocol terms |
| Android bitmap/SVG resources | `android/src/main/res` | 48 files | Nome launcher, lockup, call, notification, plus framework resources |
| official Fastlane metadata | `fastlane/metadata/android/en-US` | 12 files, 9 images | excluded; not Nome store material |

Android-only resource overlays replace the five shared app-brand resources `logo`,
`logo_light`, `icon_foreground_common`, `ic_simplex_light`, and `ic_simplex_dark` without changing
the Desktop or iOS sources. The built debug APK contains these exact Nome override payloads:

| Packaged resource | SHA-256 |
| --- | --- |
| `res/drawable-xxxhdpi-v4/logo.png` | `43a2d6e5e8e20391bb40113a7b67dae8542ff27d573ca3b75766ab1dcf8b8f28` |
| `res/drawable-xxxhdpi-v4/logo_light.png` | `6bb07261a1b663c54b327e3c8fa77c6033faf05d61007081833af6785dd5024e` |
| `res/drawable-xxxhdpi-v4/icon_foreground_common.png` | `bb2dd959890337be93580f2a1b3afbe9633eab277e552aab374382ef98fd2cf5` |
| `res/drawable-xxxhdpi-v4/ic_simplex_light.png` | `309fa387675756ffe8710a23b8addb7c0d7887beed1ad1704cf8a1fa639fb03a` |
| `res/drawable-xxxhdpi-v4/ic_simplex_dark.png` | `6533c58fbb61c4a662313e4b6a6ffd9a5a46f18db6a1520df5b70e0ad6484164` |

The API 35 launcher smoke is retained at
`audit/api35-nome-launcher-brand.png`, SHA-256
`14899cd753455d49b3d4f42d4580e50b2397cc0eba999fe1f9eccf51ae39ab39`. It shows the approved Nome
launcher artwork and the local `Nome Dev` label. The app window capture remained protected by the
production `FLAG_SECURE`; that protection was not disabled for the audit.

Remaining `SimpleX` text is classified, not globally renamed:

- protocol nouns such as SimpleX links/addresses, factual upstream source/license references, and
  preset-operator disclosures are retained for compatibility and attribution;
- upstream product-name service/call/launcher and app-logo uses were replaced by Android Nome
  resources;
- official SimpleX/Flux operator logos may appear only where the product truthfully identifies the
  corresponding operator or network agreement;
- the SimpleX name/logo must not be used as Nome software branding or to imply endorsement.

This follows the local [SimpleX trademark policy](../../../docs/TRADEMARK.md), which permits
factual compatibility descriptions but prohibits SimpleX branding for a fork.

## License and corresponding source

The repository is licensed under GNU AGPLv3. The local dependency inventory already records
SQLCipher's BSD-style license, VLC's LGPLv2 license, WebRTC's BSD-3-Clause license, and the Haskell
dependency license set. GNU's official AGPL text requires the corresponding source for distributed
object code to include the source and scripts needed to generate, install, run, and modify the
work; modified source must carry appropriate notices. Network interaction also activates the
AGPL section 13 source-offer requirement. See the
[official GNU AGPLv3 text](https://www.gnu.org/licenses/agpl-3.0.en.html).

The bundled fonts are:

- Inter Regular/Italic/Light/Medium/Semibold/Bold, whose embedded copyright identifies the 2020
  Inter Project Authors;
- Noto Color Emoji Regular, whose embedded copyright identifies Google.

Both upstream projects publish the font under SIL OFL 1.1:
[Inter license](https://github.com/rsms/inter/blob/master/LICENSE.txt) and
[Noto Emoji license](https://github.com/googlefonts/noto-emoji/blob/master/LICENSE).
The checkout now includes a combined copyright and full OFL 1.1 notice at
`apps/multiplatform/common/src/commonMain/resources/assets/licenses/FONT-LICENSES.txt`,
SHA-256 `7c20a5c56fba5d9b9a1a02a600ba9f6cbb2f408eee56646c2303b465634d8dd2`.
Both rebuilt debug and release APKs contain the byte-identical
`assets/licenses/FONT-LICENSES.txt`. This closes the local font-notice packaging gap without
changing the font binaries.

Draft corresponding-source notice, deliberately incomplete:

> Nome for Android is a modified client based on SimpleX Chat v6.5.6 and is licensed under
> GNU AGPLv3. Corresponding source for this exact distributed build, including the source revision,
> modification notices, build scripts, and dependency manifests, is available at
> `<APPROVED PUBLIC SOURCE URL>`. The software is provided without warranty under the license.

The placeholder is not a valid offer. Before distribution it must be replaced by a durable public
URL that resolves to the exact source revision used to build the signed artifact. No remote
repository was created or mutated during this audit.

## Privacy and data-disclosure draft

The checked-in `PRIVACY.md` is the policy and conditions for SimpleX Chat Ltd and preset server
operators. It is useful as an upstream network/operator disclosure but is **not** a Nome publisher
privacy policy and must not be presented as one.

Factual implementation inventory for a future owner-approved Nome policy and store Data Safety
form:

- local profiles, contacts, messages, preferences, database keys/state, attachments, and archives
  can be stored on the device;
- protocol traffic and encrypted message/file payloads can transit user-selected or preset relay,
  file, and call infrastructure;
- public addresses/channels and support interactions have separate operator/content disclosures;
- QR scanning, camera/video, microphone/audio, notifications, document/file access, foreground
  services, boot handling, and local biometric authentication are optional feature paths tied to
  the permissions listed above;
- deletion, backup, import, export, and migration outcomes depend on the official operation result
  and must not be described as guaranteed;
- a static source and `releaseRuntimeClasspath` scan found no explicit analytics, advertising,
  attribution, or crash-reporting SDK. This is not proof of zero data collection and does not
  replace owner/vendor review or runtime network inspection.

Google Play requires the developer to accurately declare app and third-party SDK collection and
sharing and to provide a privacy-policy link; the form is required even when the final declaration
is no collection. See the
[Google Play Data Safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en).

## Local store-copy draft

These strings are technical drafts only. They are not approved publishing material.

- Title: `Nome`
- Short description: `A local-first Android client compatible with the SimpleX network.`
- Compatibility disclosure:
  `Nome is an independent client based on SimpleX Chat v6.5.6. It is compatible with the SimpleX network and is not endorsed by SimpleX Chat Ltd.`
- Source disclosure:
  `This app is distributed under GNU AGPLv3. Corresponding source for this exact build is available at <APPROVED PUBLIC SOURCE URL>.`

The checked-in Fastlane title, descriptions, icon, and screenshots remain official SimpleX
materials. They are excluded from Nome distribution and cannot be used as Nome store assets.

The draft deliberately makes no claim of being official, secure, private, anonymous, online,
available, restored, successful, or complete.

## Release-blocker checklist

- [ ] owner-approved Nome application ID and coexist-versus-upgrade decision;
- [ ] owner-controlled production signing key and certificate process;
- [ ] Nome-controlled HTTPS domain and per-host `assetlinks.json` for the final package/certificate;
- [ ] approved publisher/legal entity, support URL/email, privacy policy, terms, and jurisdiction;
- [ ] public corresponding-source URL pinned to the exact signed-build revision;
- [ ] final modification notice tied to the public corresponding-source revision;
- [ ] final relay/operator defaults and factual operator disclosure;
- [ ] Nome-specific store icon, screenshots, descriptions, content rating, regions, and store account;
- [ ] owner-confirmed Data Safety answers including SDK and runtime-network review;
- [ ] final visual-asset/trademark review of all reachable states;
- [ ] final API 28/33/35, bilingual, light/dark, scaling, TalkBack, upgrade, release, and regression
      evidence;
- [ ] final same-summary two consecutive `ZERO ISSUES` reviews.

## Verification record

Completed locally:

- debug and unsigned release assemble;
- `aapt` package/label/version/SDK/permission/manifest inspection;
- `apksigner` unsigned-state check;
- APK asset/native-library inventory;
- exact brand input and packaged-overlay hashes;
- API 35 install, cold launch, live-process/no-fatal smoke, and launcher-brand capture;
- static runtime dependency scan for common analytics/ad/attribution/crash SDK names;
- local AGPL, trademark, privacy, dependency-license, font, Fastlane, and App Links review.

No production signer, remote mutation, public domain change, store submission, public distribution,
or real-user-data operation occurred.

## Audit closure

The local legal/assets/brand/application-identity/App Links/privacy/distribution audit is complete
as a technical gate with the explicit `NOT FOR DISTRIBUTION` result. The later concentrated
Milestone 3 and final-RC matrices close the locally approved P11/P14/P15/P16/P17/P20/P21 owner
states. Public-channel Observer membership/post receipt remains explicitly not verified and is
carried as the declared external validation gap; it is not a distribution or success claim.
