# Nome Android Final Review Summary

Date: 2026-07-20 (live host date verified in `Asia/Shanghai`)
Branch: `codex/nome-android-v656`
Frozen base: `3fa40fc8677ea894a80693d576caffd178275b80`
Official functional baseline: `v6.5.6` / `59fce95d`
Distribution boundary: `NOT FOR DISTRIBUTION`

This is the immutable summary for both consecutive final review rounds. Reviewers must verify the
same `REVIEW_INPUT_SHA256SUMS` before reviewing. No reviewed source, test, product, spec, ledger,
report, or visual-baseline file may change between the two verdicts. Post-review additions are
limited to recording the two verdicts, the mechanically generated final manifest, and the final
checkpoint identity.

## Delivery result

- P01–P24 and all 14 Android-reachable P1 families are implemented or terminally classified.
- P01–P24 references are page-level visual acceptance baselines. Accepted production pages were
  compared at matching API 35 language/viewport and preserve official v6.5.6 routes, states,
  actions, data, and interaction owners. P16 and P21 are terminally classified separately. P16's
  real private invitation is functionally ready and its retained comparison calibrates shared
  structure, but it is not acceptance evidence for the reference's materially different public
  invitation. P21's real owner state was compared and corrected, but that owner/composer image is
  not acceptance evidence for the reference's observer-only state.
- `FIRST_USE` remains official onboarding/root-owned. `FILTERED_NO_RESULT` is produced only after
  the real P09 query producer.
- Database/key, authentication, verification, backup/migration, file, media, call, remote,
  lifecycle, app-lock, notification/share, and destructive-action lanes retain real controlled
  fixtures and fail-closed behavior.
- Haskell/native core, `Core.kt`, protocol, database semantics, archive format, and iOS are
  unchanged.

## Declared external validation gap

The public-channel owner create/link/populate/open/delete/absence lifecycle is verified.
Observer membership, peer post receipt, and the reference's observer-only P21 visual state are
**not verified**. The owner comparison is retained as owner-presentation evidence only and does
not close the observer baseline. On the third consecutive
diagnostic turn, both relays actually carried by a fresh owner short link failed observer-side
official testing at `Connect / BROKER/NETWORK`, before prepare/connect. Source membership and
target model/core views remained unchanged and clean. Under the user's 2026-07-20 completion
amendment this is a declared non-blocking local-RC `NOT REACHABLE LOCALLY` validation gap. It must
not be represented as observer visual acceptance, membership, receipt, online state, relay
health, delivery, timeout, or success.

## Current verification

- Android aggregate:
  `:android:testDebugUnitTest :android:lintRelease :android:assembleDebug
  :android:assembleDebugAndroidTest :android:assembleRelease :android:bundleRelease` —
  `BUILD SUCCESSFUL in 3m 33s`, 205 tasks (31 executed / 174 up-to-date), using
  `--no-daemon -Pkotlin.compiler.execution.strategy=in-process` after an earlier daemon-backed
  compile stopped making progress and was interrupted without a product-failure claim.
- Android JVM: 42 tests, 0 failures, 0 errors, 0 skipped.
- Release lint: 0 fatal, 0 errors, 68 warnings.
- Desktop: `BUILD SUCCESSFUL in 1s`, 13 tasks (3 executed / 10 up-to-date), 42 tests,
  0 failures/errors/skips.
- Exact current debug main APK SHA-256:
  `c0527a1870a63ae6de00a73888c227a4006f47cfc51f925571e04f55e77ca94e`.
- Exact current androidTest APK SHA-256:
  `45c79a70e787a84116c6258a56731b7c5fa97a77f1574caefa393f946772ee59`.
- API 28: `OK (88 tests)` in 93.514s with explicit
  `nomeControlledProducerSkip=true` and `nomeCrossApiScreenshotSkip=true`.
- API 33: `OK (88 tests)` in 138.579s with the same two explicit arguments.
- API 35: `OK (88 tests)` in 511.513s with only
  `nomeControlledProducerSkip=true`; all API-35 screenshot bodies executed normally. An earlier
  current-source API 35 attempt at animation scales 1/1/1 was stopped while the database
  screenshot waited for Compose idle at item 43. It is an invalid harness attempt without an
  elapsed-time or product-timeout claim. The successful rerun used animation scales 0/0/0, then
  restoration to 1/1/1 was verified.
- All 13 argument-driven controlled producer/lifecycle harnesses fail closed when their required
  action or role is absent or invalid. On API 28, the exact 13-class run produced the expected
  13 failures in 0.066s without a bypass and `OK (13 tests)` in 0.066s only with explicit
  `nomeControlledProducerSkip=true`. The 88-test general matrix uses that explicit bypass and is
  regression/packaging evidence, not producer-execution evidence; real producer and lifecycle
  claims remain supported only by their focused controlled fixture runs. The separate P14
  guarded capture also fails closed on API 35 unless the exact token is supplied or that same
  explicit general-regression bypass is present. Its exact no-token run produced the expected
  failure in 0.106s, and the explicit general-regression bypass passed `OK (1 test)` in 0.046s.
- All three exact-package cold starts complete; filtered Nome FATAL/ANR count is zero.
- API 35 restoration: 1080×2400/420dpi, font 1.0, portrait/auto-rotate, animation scales 1,
  no enabled accessibility service, no system per-app locale override, populated Chinese Home,
  checked screenshot protection (`colors=1`, mean `0.25`).
- The exact API 35 install was initially fresh because the package was absent before that install.
  The disposable emulator then completed the official Chinese onboarding with a synthetic local
  identity and reached the real populated Home. The latest in-place install retained that local
  fixture. After the current matrix, an explicit resolved `MainActivity_default` cold start
  completed in 2.387s and showed zero filtered Nome FATAL/ANR records. API 33 completed in
  1.189s; API 28 reported `ThisTime=2.749s` / `WaitTime=2.781s`, while its stale task-level
  `TotalTime` was excluded. This
  is local fixture recovery, not preservation of real-user data or a real-user-data claim.
- Documentation cross-reference target: 44 Markdown documents after this summary is included,
  with zero missing local paths or anchors.
- Historical evidence roots remain untouched; the unknown
  `apps/multiplatform/Screenshot_1784275771.png` is excluded and untouched.
- The P14 production-capture test now fails closed on API 35 unless the exact controlled fixture
  token is supplied or the explicit general-regression producer bypass is present. When capture is
  requested, current user `IncogTarget35` and requester `P21Observer35` must both match. Its raw
  screenshot exists only in memory, both identity regions are redacted before the first file
  write, and only the redacted filename can be emitted. The exact guarded API 35 capture passed
  `OK (1 test)` in
  4.42s; the device directory contained only the 127,290-byte redacted PNG (SHA-256
  `352ff9807852d359a42ce30c22663b5d8a518b34b38009c9eb193116348d87bc`), after which that temporary
  verification output was removed and the directory returned to zero files.
- The retained P14 evidence tree contains only the replacement `-redacted-v2` production,
  responsive, and comparison PNGs. A pending `Direct + contactRequestId` Home row now announces
  the contact-request semantic instead of a direct conversation; its focused test passed on
  API 28/33/35 in 1.102s / 1.773s / 1.869s before the complete matrix above.
- A prior final-review attempt found that Android public-channel presentation suppressed every
  official non-message timeline item and that post projection trimmed real leading/trailing
  whitespace. That attempt is invalid and does not count. Non-message membership, history,
  security, and other official timeline items now remain on the legacy renderer; only simple text
  and file messages use the Nome post card. First-line title/body projection preserves the real
  text, including blank lines and surrounding whitespace. The new regression coverage is included
  in the 42-test Desktop suite and the rebuilt aggregate and three-device matrix above.
- A later review attempt found that the retained P21 comparison paired an owner/composer
  production state with an observer/read-only reference while the records called the comparison
  visually accepted. That attempt is invalid and does not count. The owner image remains useful
  only for shared composition and owner-presentation review; every current product/spec/evidence
  record now explicitly classifies the observer-only visual baseline as externally
  `NOT REACHABLE LOCALLY` with the declared Observer producer gap.
- The next review attempt reported `groupDirectInv` as a Home-routing defect, but an exact
  v6.5.6 source comparison disproved that finding: the official owner opens the Direct chat and
  `ComposeContextMemberContactActionsView` owns group-invitation acceptance in-chat; the
  standalone P14 route remains limited to a non-null `contactRequestId`. Explanatory Android
  comments now preserve that boundary without changing behavior.
- The same attempt found a real P21 deep-link defect: forward-layout channel mode always created
  its list at index zero, so `openAroundItemId` could be ignored. Channel mode now resolves the
  target item before constructing the forward `LazyListState`, while the no-target path remains
  index zero. The regression test is included in the 42-test Desktop suite and the rebuilt
  aggregate and three-device matrix above. That review is invalid and does not count.
- The following review attempt found that the Nome P21 file card had replaced the official
  `CIFileView` with a static filename/size row, removing real receive, wait, progress, error,
  open, and save behavior. That attempt is invalid and does not count. Eligible `MCFile` posts now
  embed the official `CIFileView` and its unchanged `receiveFile` callback inside the Nome card;
  simple text remains presentation-only and all non-message timeline items remain on the legacy
  renderer. The focused regression, 42-test Desktop suite, Android aggregate, and current
  API 28/API 33/API 35 88-test matrix all include the correction.
- The next review attempt found that the controlled network diagnostic wrote concrete SMP/XFTP
  hostnames to logcat, contradicting its no-address contract and potentially exposing custom
  infrastructure in test logs. That attempt is invalid and does not count. Hostnames now remain
  only in memory for exact selection/restoration assertions; configured-server and protocol-test
  logs expose only operator/protocol counts, preset booleans, validation totals, and sanitized
  result classes.
- The first round against the following hostname-safe digest returned `ZERO ISSUES`, but its
  independent second round found six stale symbol line ranges in product/spec links. Because that
  required changing the hashed payload, neither round counts. The links now point to the current
  definitions of `ConnectionPreviewIdentity`, `switchUIRemoteHost`, `connectIfOpenedViaUri`,
  `nomeDirectE2EEInfo`, `StartPartOfScreen`, and `MainScreen`; this is documentation-only and
  changes no product behavior.
- The next first-round attempt found that the expanded manifest contained 70 entries while the
  ledger still stated 64, and that two historical-fix paragraphs still called the current Desktop
  regression suite 41 rather than 42 tests. That round is invalid and does not count. The scope
  and current suite-size records now agree with the exact manifest and aggregate.
- The following security review found that real link-bearer byte arrays in the contact-request
  and public-channel test bridges were not cleared on every send/read exit path. That round is
  invalid and does not count. The contact-request, public-channel, generic two-client invitation,
  remote-desktop invitation/hash, and controlled database-key test bridges now clear sensitive
  temporary byte arrays in `finally`, including assertion, socket, and decode failures. Focused
  real bridge/lifecycle evidence exercises the correction; androidTest compile and host aggregate
  package it. The general three-device matrix uses the explicit producer bypass and is not
  producer-execution evidence.
- The next independent Round 1 found that the retained P16 comparison paired a public-group
  reference with a private-group production invitation and therefore could not close page-state
  visual acceptance. It also found a current-tense historical 67/67/72 device and 35-test Desktop
  paragraph in the page matrix. That round is invalid and does not count. P16's private route and
  real lifecycle proof remain ready, but the comparison is now structural calibration only and
  the public-reference visual state is terminally `NOT REACHABLE LOCALLY`. The old matrix counts
  are explicitly historical and superseded by the current 88/88/88 and 42-test results.
- Round 1 against the next corrected digest returned `ZERO ISSUES`, but independent Round 2
  found that both public-channel androidTests could select an unrelated channel through a broad
  display-name heuristic, and the status action emitted arbitrary public-channel names. Both
  rounds are invalid and do not count. The producer now writes a test-only record containing the
  exact creating user, remote host, group id, and a per-run nonce; every later mutation or link
  operation requires that record and exact identity match. No display-name fallback remains.
  Status results expose only a public-channel count and controlled-record/channel booleans. The
  focused guard/status checks and bounded real producer attempts exercise the correction;
  androidTest compile and host aggregate package it. The general 88/88/88 matrix uses the explicit
  producer bypass and is not producer-execution evidence. A bounded live create attempt was
  stopped after 571.15s before the official API
  returned or wrote a fixture record; it is an invalid network-harness attempt, not success or a
  product timeout. Force-stop left the record absent, and the read-only status action then passed
  `OK (1 test)` in 2.316s with `publicChannelCount=0` and no channel name.
- A later pre-review audit found that two retained evidence sections still described intermediate
  host/test-APK results in current tense. Those sections now explicitly identify their
  pre-final-current-source stage, and the completion ledger records that the in-flight reviewer
  edited the frozen payload instead of reporting the issue read-only. That attempt is invalid and
  does not count. No source, test, device, artifact, or product truth changed.
- Round 1 against the resulting 70-file digest returned `ZERO ISSUES`, but independent Round 2
  found that the stored public-channel fixture nonce was only checked for nonblank text and was
  then discarded. A stale or tampered record for the same active user and remote host could
  therefore redirect a controlled rename/populate/link/open/delete action by changing its group
  id. Both rounds are invalid and do not count. Every record-bound producer action and the
  two-client source now require a caller-supplied canonical UUID, persist that exact UUID, and
  accept the record only when stored and supplied nonces are equal together with the expected
  active user, remote host, and group id. API 35 guard checks prove missing and malformed nonce
  inputs fail closed, while the canonical UUID status check passed read-only with no record or
  channel. Those focused guard/status checks exercise the correction; the rebuilt androidTest APK
  and final host aggregate package it. The general API 28/API 33/API 35 matrix uses the explicit
  producer bypass and is not producer-execution evidence.
- The following same-digest Round 1 returned `ZERO ISSUES`, but Round 2 found that five
  API-35-only screenshot evidence tests returned silently on a wrong API. The same pattern also
  existed in the guarded P14 production capture. Both rounds are invalid and do not count.
  All six tests now fail on a wrong API by default and permit lower-API full-regression skip only
  when `nomeCrossApiScreenshotSkip=true` is explicitly supplied. On API 28, the focused class
  failed without the argument and passed with it. The rebuilt test APK then passed the explicit
  API 28/API 33 lower-API matrix and the normal API 35 88-test matrix above; API 35 animation
  scales were restored to 1/1/1.
- Round 1 against review-manifest digest
  `58c5176e45f945daa1ebf89053e797500c66a693766397a453474a4333157206`
  found that argument-driven producer/lifecycle androidTests returned silently when their required
  arguments were absent, so the ordinary 88-test matrix could falsely appear to execute them.
  That round is invalid and does not count. All 13 parameterized controlled harnesses now fail
  closed on absent or invalid required inputs and bypass only with explicit
  `nomeControlledProducerSkip=true`. The exact API 28 negative/explicit-bypass proof is recorded
  above, followed by the rebuilt aggregate and three-device general regression. Focused
  controlled fixture runs remain the only evidence for actual producer/lifecycle results.
- Round 1 against the next 70-file review-manifest digest
  `dc1795ad033a9e7fa4701a8062debee3c755327abcbe4dfb550c0dd349726b3a`
  found that the separate P14 guarded capture still returned silently when its exact opt-in token
  was absent or invalid, contradicting the fail-closed summary. That round is invalid and does not
  count. The P14 test now fails on API 35 without the exact token unless
  `nomeControlledProducerSkip=true` explicitly identifies a general-regression matrix; the actual
  capture still requires the exact fixed fixture and remains supported only by its focused guarded
  run.

## Final same-summary reviews

- Final Round 1 — independent code/product/spec/test/evidence review:
  `ZERO ISSUES`.
- Final Round 2 — independent security/release/privacy/evidence review:
  `ZERO ISSUES`.
- Both rounds independently verified all 70 entries of the exact same frozen
  `REVIEW_INPUT_SHA256SUMS`, whose SHA-256 is
  `109d094e913a751fca7370cde366da3132ef46ebf3f1e4cd06f216fbfbe1b4a4`.
- No manifest-listed byte changed between the two reviews. The only permitted post-review changes
  are these verdict/closure records, the final manifest, and the checkpoint identity represented
  by the commit containing this report. `REVIEW_INPUT_SHA256SUMS` remains frozen to preserve the
  exact reviewed-byte identity; the final `SHA256SUMS` covers the post-review freeze payload.

## Unsigned local artifacts

- Universal APK:
  `apps/multiplatform/android/build/outputs/apk/release/android-release-unsigned.apk`;
  498,412,556 bytes; 1,730 entries; SHA-256
  `f08bdbceacadd09b1162de82019fb91fb93db8f1d103d34825d6a47de9414695`.
- `aapt`: package `chat.simplex.app`, version 358 / 6.5.6, min API 28, target API 35,
  label `Nome`, `arm64-v8a` and `armeabi-v7a`.
- APK verification: `DOES NOT VERIFY`; no signing manifest.
- AAB: `apps/multiplatform/android/build/outputs/bundle/release/android-release.aab`;
  117,299,693 bytes; 1,735 entries; SHA-256
  `9318126a0742101a65563903319589bbac8b633f7978cf18d275c9fd0309c33b`.
- AAB verification: no manifest / unsigned JAR; zero signature entries.
- APK/AAB entry-name and DEX scans: zero controlled androidTest/evidence/final-report leaks.

## Required reviewer verdict

Review the full hashed payload for functional regressions, route/state/action/data fidelity,
visual-baseline contradictions, fabricated facts, privacy/security leakage, release-test
isolation, accessibility semantics, lifecycle/double-submit defects, Desktop changes, forbidden
core/protocol/database/archive/iOS changes, documentation contradictions, and excluded-file
handling.

Return exactly `ZERO ISSUES` only if there is no actionable critical, high, medium, or low issue.
Otherwise report every issue with file and line evidence; any issue invalidates both-round closure
until fixed and a new summary/hash is frozen.
