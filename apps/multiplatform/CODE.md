# Coding and building

You are an expert developer for SimpleX Chat, a privacy-first decentralized messaging platform. You MUST navigate and develop this codebase using the three-layer documentation architecture described below. You MUST NOT write code without first loading the relevant product and spec context.

## Three-Layer Documentation Architecture

### Why this structure exists

LLMs start each session with no persistent understanding of the codebase. Navigating thousands of lines of flat source code to reconstruct behavior, constraints, and intent wastes context window and produces unreliable results.

The `product/`, `spec/`, and source layers form a persistent, structured representation of the system that survives across sessions. Each layer is connected to the next by bidirectional cross-references. This structure enables you to load only the context relevant to a specific change, understand all affected concepts, and maintain coherence as the system evolves.

### The layers

| Layer | Contains | Question it answers |
|-------|----------|-------------------|
| `product/` | Capabilities, user flows, views, business rules, glossary | **What** does the system do and why? |
| `spec/` | Technical design, API contracts, database schema, service internals | **How** is it organized technically? |
| `common/src/commonMain/` | Shared Kotlin/Compose code (Android + Desktop) | What does it **execute** on both platforms? |
| `common/src/androidMain/` | Android-specific Kotlin (platform implementations) | What does it execute on **Android**? |
| `common/src/desktopMain/` | Desktop-specific Kotlin (platform implementations) | What does it execute on **Desktop**? |
| `android/src/main/` | Android app module (Application, Activity, Services) | What is the **Android entry point**? |
| `desktop/src/jvmMain/` | Desktop app module (main function) | What is the **Desktop entry point**? |
| `../../src/Simplex/Chat/` | Haskell core (chat logic, protocol, database) | What does the **core** execute? |

Each layer links to the next:
- `product/concepts.md` links every concept to its spec docs, source files, and tests in a single table — this is the primary navigation entry point
- `product/views/*.md` and `product/flows/*.md` each have a **Related spec:** line linking to their most relevant spec documents
- `product/glossary.md` uses *See: [spec/...]* references and `product/rules.md` uses **Spec:** [spec/...] references to link individual terms and rules down to spec
- `spec/` documents contain **Source:** headers and inline function links pointing down to source. Line references MUST be clickable by embedding the `#Lxx-Lyy` fragment in the link URL: [`functionName()`](common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#Lxx-Lyy). You MUST NOT duplicate line numbers in the display text — the URL fragment is sufficient. Why: redundant line numbers in display text create maintenance burden on every line shift.
- Reverse direction: the Document Map (end of this file) maps source → spec → product

### Navigation workflow

When the user requests any change, you MUST follow these steps before writing any code:

1. **Identify scope.** You MUST read `product/concepts.md` and find which product concepts are affected by the requested change. Each row links to the relevant product docs, spec docs, source files, and tests. Why: concepts.md is the fastest path to identify all affected documents — skipping it risks missing impacted areas.

2. **Load product context.** You MUST read the relevant `product/views/*.md` or `product/flows/*.md` to understand current user-facing behavior. For business constraints, you MUST read `product/rules.md`. Why: product documents define the intended behavior — changing code without understanding current behavior risks breaking the user contract.

3. **Load spec context.** You MUST follow the product → spec links to read the relevant `spec/*.md` or `spec/services/*.md`. You MUST understand the technical design, function signatures, and data flows. Why: spec documents reveal technical constraints and invariants that product docs omit — ignoring them leads to implementations that violate existing guarantees.

4. **Load source context.** You MUST follow the spec → source links (with line numbers) to read the relevant source files. Why: source code is the ground truth — product and spec may lag behind actual behavior.

5. **Identify full impact.** You MUST read `spec/impact.md` to find all product concepts affected by the source files you plan to change. This determines which documents you MUST update after the code change. Why: without impact analysis, documentation updates will be incomplete, and future sessions will navigate using stale information.

For internal-only changes that do not map to a product concept (infrastructure, refactoring, non-user-facing fixes), you MUST start at step 3 using the Document Map to find the relevant spec document, then proceed to steps 4–6.

6. **Implement.** Make the code change in source, then you MUST update all affected documentation as described in the Change Protocol below.

### Key navigation documents

| Document | Purpose | When to read |
|----------|---------|-------------|
| `product/concepts.md` | Concept → doc → code → test cross-reference | Starting point for every change |
| `product/rules.md` | Business invariants with enforcement locations and tests | Before modifying any behavior |
| `product/glossary.md` | Domain term definitions | When encountering unfamiliar terms |
| `product/gaps.md` | Known issues and recommendations | Before designing a fix or feature |
| `spec/impact.md` | Source file → affected product concepts | After identifying which files to change |
| Document Map (below) | Source ↔ spec ↔ product mapping | When updating documentation |

---

## Code Security

When designing code and planning implementations, you MUST:
- Apply adversarial thinking, and consider what may happen if one of the communicating parties is malicious. Why: security vulnerabilities arise from untested assumptions about trust boundaries.
- Formulate an explicit threat model for each change — who can do which undesirable things and under which circumstances. Why: explicit threat models catch attack vectors that implicit reasoning misses.

---

## Code Style

**Follow existing code patterns — you MUST:**
- Match the style of surrounding code. Why: consistent style reduces cognitive load and prevents unnecessary diff noise.
- Use Kotlin data classes for value types, regular classes for reference types, and sealed classes/interfaces for variants. Why: correct type choices leverage the type system for compile-time correctness.
- Prefer exhaustive `when` expressions over `else` branches. Why: `else` branches bypass compiler checks for new sealed subclasses and hide bugs.

**Comments policy — you MUST:**
- Only comment on non-obvious design decisions or tricky implementation details. Why: redundant comments create maintenance burden and drift from code.
- Keep function names and type signatures self-documenting. Why: good names eliminate the need for most comments.
- Assume a competent Kotlin reader. Why: over-explaining trivial Kotlin adds noise without value.

**Diff and refactoring — you MUST:**
- Avoid unnecessary changes and code movements. Why: unnecessary changes increase review burden and hide the meaningful diff.
- Never do refactoring unless it substantially reduces cost of solving the current problem, including the cost of refactoring itself. Why: speculative refactoring has guaranteed present cost with uncertain future benefit.
- Minimize the code changes — do what is minimally required to solve users' problems. Why: smaller diffs are easier to review, less likely to introduce bugs, and faster to revert.

**Document and code structure — you MUST:**
- **Never move existing code or sections around** — add new content at appropriate locations without reorganizing existing structure. Why: moving code creates large diffs that obscure the actual change and break git blame.
- When adding new sections to documents, continue the existing numbering scheme. Why: consistent numbering preserves document navigability.
- Minimize diff size — prefer small, targeted changes over reorganization. Why: large diffs compound review errors and make rollback difficult.

**Code analysis and review — you MUST:**
- Trace data flows end-to-end: from origin, through storage/parameters, to consumption. Flag values that are discarded and reconstructed from partial data (e.g. extracted from a URI missing original fields) — this is usually a bug. Why: broken data flows are the most common source of security and correctness bugs.
- Read implementations of called functions, not just signatures — if duplication involves a called function, check whether decomposing it resolves the duplication. Why: function signatures can be misleading about actual behavior.
- Read every function in the data flow even when the interface seems clear. Why: wrong assumptions about internals are the main source of missed bugs.

---

## Plans

When developing via plans (non-trivial features, multi-step changes, architectural decisions), you MUST store the plan in the `plans/` folder before implementing. Why: plans are the persistent record of design decisions and rationale — without them, future sessions cannot understand why the system was built the way it was.

### Plan requirements

1. **File naming.** You MUST use the format `YYYYMMDD_NN.md` (e.g., `20260211_01.md`). Why: chronological ordering makes it easy to trace the evolution of design decisions.

2. **Plan structure.** Every plan MUST include: (1) Problem statement, (2) Solution summary, (3) Detailed technical design, (4) Detailed implementation steps. Why: incomplete plans lead to ad-hoc implementation that drifts from intent.

3. **Consistency with product/ and spec/.** The plan MUST be consistent with the current state of `product/` and `spec/`. If the plan introduces new behavior, it MUST describe which product and spec documents will be affected. Why: plans that contradict existing documentation create conflicting sources of truth.

4. **Adversarial self-review.** After writing the plan, you MUST run the same adversarial self-review as for code changes: verify the plan is internally consistent, consistent with product/ and spec/, and does not introduce contradictions. You MUST repeat until two consecutive passes find zero issues. Why: an incoherent plan produces incoherent implementation.

---

## Change Protocol

### The rule

Every code change MUST include corresponding updates to `spec/` and `product/`. A task is NOT complete until all three layers are coherent with each other. Why: these layers are the persistent memory that enables coherent development across sessions — stale documentation creates false confidence and compounds errors in every future change.

### What to update

1. **spec/ — on every code change.** You MUST update the corresponding spec document to reflect the change. You MUST add new functions, update changed signatures, and remove deleted ones. Why: spec documents map 1:1 to source files — divergence defeats specification.

2. **product/ — when user-visible behavior changes.** You MUST update the relevant `product/views/*.md` and any affected `product/flows/*.md`. You MUST update `product/rules.md` when business invariants change. Why: product documents are the contract with users — silent changes create confusion.

3. **Line number references — on every code change.** You MUST verify and update all `#Lxx-Lyy` references in affected spec documents. Why: stale line numbers make spec documents misleading and destroy navigational value.

4. **Cross-references — when adding or removing files.** You MUST add corresponding spec documents and update `spec/README.md` document index and reverse index. When adding pages, you MUST add `product/views/` and `spec/client/` documents. You MUST update the Document Map at the end of this file. Why: every source file must be covered for the navigation system to work.

5. **Impact graph — when adding files or changing what a file affects.** You MUST update `spec/impact.md` to reflect the source file → product concept mapping. Why: the impact graph drives documentation updates for all future changes — an incomplete graph causes future changes to miss required updates.

6. **Concept index — when adding or changing product concepts.** You MUST add or update the relevant row in `product/concepts.md` with links to product docs, spec docs, source files, and tests. Why: the concept index is the entry point for all future navigation — a missing row means future changes to that concept will miss context.

7. **[GAP] annotations — when discovering issues.** When encountering missing error handling, dead code, inconsistencies, or incomplete features, you MUST add a `[GAP]` annotation in the relevant spec or product document and add a summary to `product/gaps.md`. Why: this builds institutional knowledge about technical debt.

8. **[REC] annotations — when identifying improvements.** You MUST add a `[REC]` annotation in the relevant document. Why: capturing improvement ideas at discovery time preserves context that is lost later.

9. **Preserve document structure.** You MUST follow existing format conventions: spec documents use function-anchored links with line numbers, product documents use interaction descriptions, flow documents use Mermaid diagrams. Why: consistent structure makes documents predictable and navigable.

### Adversarial self-review

After completing all changes (code + documentation), you MUST run an adversarial self-review. You MUST check coherence both within each layer and across layers.

**Within-layer coherence — you MUST verify:**
- spec/ is internally consistent — no contradictory descriptions, state machines have no unreachable states, data model is referentially intact
- product/ is internally consistent — flows match views, rules match behavior descriptions

**Across-layer coherence — you MUST verify:**
- Every new or changed function in source appears in the corresponding spec/ document
- Every user-visible behavior change in source appears in the relevant product/ document
- All `#Lxx-Lyy` line references in affected spec documents point to the correct lines
- All cross-references resolve — product → spec links, spec → source links
- `spec/impact.md` covers all affected product concepts for the changed source files
- `product/concepts.md` rows are current for any affected concepts

**Convergence:** You MUST repeat the review-and-fix cycle until two consecutive passes find zero issues. You MUST fix all issues discovered between passes. Why: LLM non-determinism means a single review pass may miss violations — two consecutive clean passes provide confidence that the layers are coherent.

---

## Multiplatform Architecture Notes

### Kotlin Multiplatform (KMP) + Compose Multiplatform

The app uses Kotlin Multiplatform with Compose Multiplatform for shared UI. The project has three Gradle modules:

- **common/** — Shared library containing all models, views, platform abstractions, and theme system
- **android/** — Android app module (Application, Activity, Services)
- **desktop/** — Desktop JVM app module (main entry point)

### expect/actual Pattern

Platform-specific code uses Kotlin's `expect`/`actual` mechanism. The `commonMain` source set declares `expect` functions/classes, and `androidMain`/`desktopMain` provide `actual` implementations. Files follow the naming convention:
- `commonMain`: `FileName.kt` (contains `expect` declarations)
- `androidMain`: `FileName.android.kt` (contains `actual` implementations)
- `desktopMain`: `FileName.desktop.kt` (contains `actual` implementations)

When modifying platform abstractions, you MUST update both `actual` implementations.

### Source Set Structure

```
common/src/
├── commonMain/kotlin/chat/simplex/common/    -- Shared code (207 files)
│   ├── model/          -- ChatModel, SimpleXAPI, CryptoFile
│   ├── platform/       -- expect/actual platform abstractions
│   ├── ui/theme/       -- Theme system (ThemeManager, colors, types)
│   └── views/          -- Compose UI (chat, chatlist, call, settings, etc.)
├── androidMain/kotlin/chat/simplex/common/   -- Android-specific sources (67 files)
│   ├── platform/       -- actual implementations
│   └── views/          -- Android-specific view variants
├── desktopMain/kotlin/chat/simplex/common/   -- Desktop actuals (59 files)
│   ├── platform/       -- actual implementations
│   └── views/          -- Desktop-specific view variants
android/src/main/java/chat/simplex/app/       -- Android app (9 files)
desktop/src/jvmMain/kotlin/chat/simplex/desktop/ -- Desktop app (1 file)
```

### Platform Differences

| Aspect | Android | Desktop |
|--------|---------|---------|
| Layout | 2-column (chat list → chat) | 3-column (sidebar → chat list → details) |
| Background messaging | SimplexService (foreground service) + MessagesFetcherWorker (WorkManager) | Continuous (always-on process) |
| Notifications | Android NotificationManager with channels | Desktop system notifications |
| Calls | CallActivity (separate Activity) + CallService | In-window call view |
| Video playback | ExoPlayer | VLC (VLCJ) |
| Authentication | Android BiometricPrompt | Passcode only |
| Auto-update | Play Store / manual APK | Built-in AppUpdater |
| Window management | Standard Activity lifecycle | StoreWindowState persistence |
| Entry point | SimplexApp (Application) + MainActivity | Main.kt → initHaskell() → showApp() |

---

## Document Map

### PC32 Nome Android cross-cutting scope (authoritative reverse index)

The ordinary Document Map rows below continue to route each source to its **primary** spec and product documents. The PC32 reverse index in `spec/impact.md` is authoritative for the frozen Android-only foundation, the Phase 2 Batch 2 production home implementation, the frozen Phase 2 Batch 3 P13 external-link preview, the active Batch 1A P01 database-root implementation, the active Milestone 3 P11/P12, P14/P15, P16, P17/P18, P19/P20, P21/P22, P23/P24, and remote-desktop/Android-intent presentation groups, and the larger transitive scope later pages must preserve. A transitive path is a review/validation dependency, not a prediction that every file will change. Batch 2 uses a narrow shared home seam, Batch 3 uses a narrow shared connection-policy/presentation seam, Batch 1A uses a narrow root facts/renderer seam, P14/P15 use narrow request/address presentation seams, P16 uses a narrow invited-group presentation seam, P17/P18 keep the loaded-chat/item/composer owners while changing Android presentation, P19/P20 keep the verification/channel command owners behind Android presentation seams, P21/P22 keep loaded-channel/profile owners behind Android-only chrome/identity-center seams, P23/P24 keep settings/database/migration owners behind Android presentation and platform-document seams, and the remote-desktop/intent group keeps the official controller and three existing intent handlers behind Android presentation/lifecycle corrections. All Desktop actuals preserve legacy behavior.

Path aliases are relative to `apps/multiplatform/`: `CM` = `common/src/commonMain/kotlin/chat/simplex/common`, `AM` = `common/src/androidMain/kotlin/chat/simplex/common`, `MR` = `common/src/commonMain/resources/MR`, and `APP` = `android/src/main`. Every row also routes to the approved Nome documents: `spec/client/nome-android-ui.md` and `product/views/nome-android.md`.

| PC32 surface | Existing transitive source locations | Additional primary documentation route |
|--------------|--------------------------------------|----------------------------------------|
| App / root lifecycle | `CM/App.kt`; `CM/platform/AppCommon.kt`; `AM/platform/AppCommon.android.kt`; `APP/java/chat/simplex/app/{MainActivity.kt,SimplexApp.kt,nome/NomeProductionShell.kt}` | `spec/architecture.md`; `spec/client/navigation.md`; `product/flows/onboarding.md`; `product/views/chat-list.md` |
| ChatModel / SimpleXAPI / core bridge | `CM/model/ChatModel.kt`; `CM/model/SimpleXAPI.kt`; `CM/platform/Core.kt` | `spec/state.md`; `spec/api.md`; `spec/architecture.md`; `product/concepts.md` |
| AppLock / local authentication | `CM/AppLock.kt`; `CM/views/localauth/**`; `AM/views/{localauth/PlatformNomeAppLockScreen.android.kt,helpers/LocalAuthentication.android.kt,usersettings/PrivacySettings.android.kt}` | `spec/architecture.md`; `spec/client/navigation.md`; `spec/client/nome-android-ui.md`; `product/views/settings.md`; `product/views/nome-android.md` |
| Database / migration | `CM/views/database/**`; `CM/views/migration/**`; `CM/views/onboarding/SetupDatabasePassphrase.kt`; `AM/{platform/Cryptor.android.kt,views/database/**,ui/nome/database/**}`; `common/src/desktopMain/kotlin/chat/simplex/common/views/database/PlatformDatabaseRootRoute.desktop.kt` | `spec/database.md`; `spec/client/navigation.md`; `spec/client/nome-android-ui.md`; `product/flows/onboarding.md`; `product/views/settings.md`; `product/views/nome-android.md` |
| Theme | `CM/ui/theme/**`; `AM/ui/theme/**`; `CM/views/usersettings/Appearance.kt`; `AM/views/usersettings/Appearance.android.kt` | `spec/services/theme.md`; `product/views/settings.md` |
| Locale / bilingual resources | `MR/**/strings.xml`; `CM/platform/Resources.kt`; `CM/platform/UI.kt`; `AM/helpers/Locale.kt`; `AM/platform/Resources.android.kt`; `AM/platform/UI.android.kt` | `spec/architecture.md`; all affected product view copy |
| Onboarding | `CM/views/onboarding/**`; `AM/views/onboarding/**`, including `PlatformNomeOnboardingPages.android.kt` | `spec/client/navigation.md`; `spec/client/nome-android-ui.md`; `product/views/onboarding.md`; `product/flows/onboarding.md`; `product/views/nome-android.md` |
| Chat list | `CM/views/chatlist/**`; `AM/views/chatlist/**`; `AM/ui/nome/home/**`; `common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt` | `spec/client/chat-list.md`; `spec/client/navigation.md`; `product/views/chat-list.md` |
| New chat / ConnectPlan | `CM/views/newchat/**`; `AM/views/newchat/**`; `AM/ui/nome/connection/**`; `common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.desktop.kt` | `spec/api.md`; `spec/state.md`; `spec/client/navigation.md`; `product/views/new-chat.md`; `product/flows/connection.md`; `product/rules.md` |
| Chat / items / compose | `CM/views/chat/*.kt`; `CM/views/chat/item/**`; `AM/views/chat/*.kt`; `AM/views/chat/item/**` | `spec/client/chat-view.md`; `spec/client/compose.md`; `product/views/chat.md`; `product/flows/messaging.md` |
| Contacts | `CM/views/contacts/**`; `CM/views/chat/{ChatInfoView.kt,VerifyCodeView.kt,ScanCodeView.kt}` | `spec/client/chat-view.md`; `product/views/contact-info.md`; `product/flows/connection.md` |
| Groups / channels | `CM/views/chat/group/**`; `CM/views/newchat/{AddGroupView.kt,AddChannelView.kt}` | `spec/client/chat-view.md`; `product/views/group-info.md`; `product/flows/group-lifecycle.md` |
| Users / settings / network | `CM/views/chatlist/UserPicker.kt`; `AM/views/chatlist/UserPicker.android.kt`; `CM/views/usersettings/**`; `AM/views/usersettings/**` | `spec/client/navigation.md`; `spec/architecture.md`; `product/views/user-profiles.md`; `product/views/settings.md` |
| Calls | `CM/views/call/**`; `AM/views/call/**`; `APP/java/chat/simplex/app/views/call/CallActivity.kt` | `spec/services/calls.md`; `product/views/call.md`; `product/flows/calling.md` |
| Files / media / share | `CM/model/CryptoFile.kt`; `CM/platform/{Files.kt,Images.kt,RecAndPlay.kt,Share.kt,VideoPlayer.kt}`; matching `AM/platform/*.android.kt` actuals | `spec/services/files.md`; `product/flows/file-transfer.md`; `product/views/chat.md` |
| Notifications / background | `CM/platform/{Notifications.kt,NtfManager.kt,SimplexService.kt}`; matching Android actuals; `APP/java/chat/simplex/app/model/NtfManager.android.kt`; `APP/java/chat/simplex/app/{SimplexService.kt,MessagesFetcherWorker.kt}` | `spec/services/notifications.md`; `product/flows/messaging.md` |
| Remote desktop | `CM/views/remote/**` | `spec/architecture.md`; `spec/client/{navigation.md,nome-android-ui.md}`; `spec/impact.md`; `product/views/{settings.md,nome-android.md}` |
| MainActivity / intents | `APP/AndroidManifest.xml`; `APP/java/chat/simplex/app/{MainActivity.kt,SimplexApp.kt}`; `APP/java/chat/simplex/app/views/helpers/Util.kt` | `spec/architecture.md`; `spec/client/{navigation.md,nome-android-ui.md}`; `spec/impact.md`; affected navigation/connection/messaging flows and `product/views/nome-android.md` |
| Permissions | `APP/AndroidManifest.xml`; `common/src/androidMain/AndroidManifest.xml`; `AM/helpers/Permissions.kt`; Android QR-scanner and notification-onboarding actuals | `spec/client/navigation.md`; `spec/services/notifications.md`; affected onboarding/new-chat/call views |
| Android services / workers | `APP/java/chat/simplex/app/{SimplexService.kt,CallService.kt,MessagesFetcherWorker.kt}`; `APP/AndroidManifest.xml` | `spec/services/notifications.md`; `spec/services/calls.md`; `product/flows/messaging.md`; `product/flows/calling.md` |

`Core.kt`, Haskell/native core, and iOS remain read-only verification scope for PC32. Phase 2 Batch 2 authorizes its recorded get-chats wrapper and shared home seam. Phase 2 Batch 3 authorizes only typed siblings around existing plan/connect commands, the P13 safe model/policy seam, and the required Desktop-declining actual/test. Neither batch authorizes a new API owner, protocol/native change, Desktop Nome UI, or Desktop behavior change. Under the current user execution-contract amendment, ordinary UI groups use focused tests, compile, API 35 production smoke, affected visual/accessibility inspection, one review, and at least one reference-size production/side-by-side capture per page family. API 28/33/35, complete bilingual light/dark, 200%, TalkBack, release, historical manifests, and full regression are concentrated at milestones/final; high-risk database/auth/security/backup/files/calls retain their deeper fixture and lifecycle requirements. Only final RC requires two consecutive `ZERO ISSUES` reviews against the same self-verifying `REVIEW_INPUT_SHA256SUMS`; verdicts are recorded only in `FINAL_REVIEW_VERDICTS.md`.

Phase 2 Batch 2 is the one explicit exception to the default Android-only placement rule: `StartPartOfScreen` needs a platform-selectable home at the existing shared route selection point. `common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt` declares only the seam; it owns no Nome UI, navigation, model, or protocol state. Android implements the Nome P07/P08 home in `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/`, while `common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt` invokes `defaultContent` unchanged. The fallback contract is covered by `common/src/desktopTest/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRouteDesktopTest.kt`.

Phase 2 Batch 3 is the second recorded exception: all seven identity-choice call branches and six
legacy caller surfaces are already shared. `PlatformConnectionPreview.kt` may therefore contain
only safe display facts, exhaustive branch policy, callbacks, and the expect declaration; raw
bearer values remain in controller closures. Android renders P13 under
`common/src/androidMain/kotlin/chat/simplex/common/ui/nome/connection/`. The Desktop actual returns
`false`, and `PlatformConnectionPreviewDesktopTest` guards that legacy fallback.

### Shared Sources (commonMain)

| Source Location | Spec Document | Product Document |
|----------------|---------------|-----------------|
| common/.../common/App.kt | spec/architecture.md, spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| common/.../common/AppLock.kt | spec/architecture.md | product/views/settings.md |
| common/.../common/model/ChatModel.kt | spec/state.md, spec/client/chat-list.md, spec/client/nome-android-ui.md | product/concepts.md, product/views/chat-list.md, product/views/nome-android.md |
| common/.../common/model/SimpleXAPI.kt | spec/api.md, spec/architecture.md, spec/state.md, spec/client/chat-list.md, spec/client/nome-android-ui.md | product/concepts.md, product/views/chat-list.md, product/flows/connection.md, product/views/nome-android.md, product/rules.md |
| common/.../common/model/CryptoFile.kt | spec/services/files.md | product/flows/file-transfer.md |
| common/.../common/platform/Core.kt | spec/architecture.md | product/concepts.md |
| common/.../common/platform/AppCommon.kt | spec/architecture.md | product/flows/onboarding.md |
| common/.../common/platform/Notifications.kt | spec/services/notifications.md | product/flows/messaging.md |
| common/.../common/platform/NtfManager.kt | spec/services/notifications.md | product/flows/messaging.md |
| common/.../common/platform/Files.kt and platform actuals | spec/services/files.md, spec/client/nome-android-ui.md, spec/impact.md | product/flows/file-transfer.md, product/views/nome-android.md, product/rules.md |
| common/.../common/platform/SimplexService.kt | spec/services/notifications.md | product/flows/messaging.md |
| common/.../common/platform/Share.kt | spec/architecture.md | product/concepts.md |
| common/.../common/platform/VideoPlayer.kt | spec/services/files.md | product/views/chat.md |
| common/.../common/platform/RecAndPlay.kt | spec/services/files.md | product/views/chat.md |
| common/.../common/platform/UI.kt | spec/architecture.md | product/views/chat.md |
| common/.../common/platform/Platform.kt | spec/architecture.md | product/concepts.md |
| common/.../common/ui/theme/ThemeManager.kt | spec/services/theme.md, spec/client/nome-android-ui.md | product/views/settings.md, product/views/nome-android.md |
| common/.../common/ui/theme/Theme.kt | spec/services/theme.md, spec/client/nome-android-ui.md | product/views/settings.md, product/views/nome-android.md |
| common/.../common/ui/theme/Color.kt | spec/services/theme.md, spec/client/nome-android-ui.md | product/views/settings.md, product/views/nome-android.md |
| common/.../common/views/chatlist/ChatListView.kt | spec/client/chat-list.md, spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/new-chat.md, product/flows/connection.md, product/views/nome-android.md, product/rules.md |
| common/.../common/views/chatlist/PlatformHomeRoute.kt | spec/client/navigation.md, spec/client/chat-list.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| common/.../common/views/chatlist/ChatListNavLinkView.kt | spec/client/chat-list.md | product/views/chat-list.md |
| common/.../common/views/chatlist/ChatPreviewView.kt | spec/client/chat-list.md | product/views/chat-list.md |
| common/.../common/views/chatlist/UserPicker.kt | spec/client/chat-list.md | product/views/chat-list.md |
| common/.../common/views/chatlist/TagListView.kt | spec/client/chat-list.md | product/views/chat-list.md |
| common/.../common/views/chat/ChatView.kt | spec/client/chat-view.md | product/views/chat.md |
| common/.../common/views/chat/ComposeView.kt | spec/client/compose.md | product/views/chat.md |
| common/.../common/views/chat/SendMsgView.kt | spec/client/compose.md | product/views/chat.md |
| common/.../common/views/chat/ChatInfoView.kt | spec/client/chat-view.md | product/views/contact-info.md |
| common/src/commonMain/kotlin/chat/simplex/common/views/chat/VerifyCodeView.kt | spec/api.md, spec/client/chat-view.md, spec/client/nome-android-ui.md | product/views/contact-info.md, product/views/nome-android.md, product/rules.md |
| common/src/commonMain/kotlin/chat/simplex/common/views/chat/PlatformVerifyCodeLayout.kt | spec/client/chat-view.md, spec/client/nome-android-ui.md | product/views/contact-info.md, product/views/nome-android.md, product/rules.md |
| common/.../common/views/chat/group/ | spec/client/chat-view.md | product/views/group-info.md |
| common/.../common/views/chat/item/ | spec/client/chat-view.md | product/views/chat.md |
| common/.../common/views/call/CallView.kt | spec/services/calls.md | product/views/call.md |
| common/.../common/views/call/IncomingCallAlertView.kt | spec/services/calls.md | product/views/call.md |
| common/.../common/views/call/WebRTC.kt | spec/services/calls.md | product/flows/calling.md |
| common/.../common/views/newchat/NewChatView.kt | spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/new-chat.md, product/views/nome-android.md |
| common/.../common/views/newchat/PlatformNewChatRoute.kt | spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/new-chat.md, product/views/nome-android.md, product/rules.md |
| common/.../common/views/newchat/AddGroupView.kt | spec/client/navigation.md | product/views/new-chat.md |
| common/src/commonMain/kotlin/chat/simplex/common/views/newchat/AddChannelView.kt | spec/api.md, spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/new-chat.md, product/views/nome-android.md, product/gaps.md, product/rules.md |
| common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformChannelSetupRoute.kt | spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/new-chat.md, product/views/nome-android.md, product/gaps.md, product/rules.md |
| common/.../common/views/newchat/ConnectPlan.kt | spec/api.md, spec/state.md, spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/new-chat.md, product/flows/connection.md, product/views/nome-android.md, product/rules.md |
| common/.../common/views/newchat/PlatformConnectionPreview.kt | spec/api.md, spec/state.md, spec/client/navigation.md, spec/client/nome-android-ui.md | product/concepts.md, product/views/new-chat.md, product/flows/connection.md, product/views/nome-android.md, product/rules.md |
| common/.../common/views/usersettings/SettingsView.kt | spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/settings.md, product/views/nome-android.md |
| common/src/{commonMain,androidMain,desktopMain}/kotlin/chat/simplex/common/views/usersettings/PlatformSettingsHomeRoute* | spec/client/navigation.md, spec/client/nome-android-ui.md, spec/impact.md | product/views/settings.md, product/views/nome-android.md, product/rules.md, product/gaps.md |
| common/src/{commonMain,androidMain,desktopMain}/kotlin/chat/simplex/common/views/usersettings/{PlatformSettingsDetailRoute*,PlatformAboutSettingsRoute*} | spec/client/navigation.md, spec/client/nome-android-ui.md, spec/impact.md | product/views/settings.md, product/views/nome-android.md, product/rules.md |
| common/.../common/views/usersettings/Appearance.kt | spec/services/theme.md | product/views/settings.md |
| common/.../common/views/usersettings/PrivacySettings.kt | spec/client/navigation.md | product/views/settings.md |
| common/.../common/views/usersettings/networkAndServers/ | spec/architecture.md | product/views/settings.md |
| common/src/{commonMain,androidMain,desktopMain}/kotlin/chat/simplex/common/views/usersettings/networkAndServers/PlatformObservedNetworkInfo* | spec/architecture.md, spec/client/nome-android-ui.md, spec/impact.md | product/views/settings.md, product/views/nome-android.md, product/rules.md |
| common/.../common/views/usersettings/UserProfilesView.kt | spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/user-profiles.md, product/views/nome-android.md, product/rules.md |
| common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/{PlatformIdentityCenterRoute.kt,UserDeletionLifecycle.kt} | spec/client/navigation.md, spec/client/nome-android-ui.md, spec/impact.md | product/views/user-profiles.md, product/views/nome-android.md, product/rules.md |
| common/.../common/views/onboarding/ | spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/onboarding.md, product/views/nome-android.md |
| common/.../common/views/localauth/ | spec/architecture.md, spec/client/nome-android-ui.md | product/views/settings.md, product/views/nome-android.md |
| common/.../common/views/database/ | spec/database.md, spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/settings.md, product/views/nome-android.md, product/rules.md |
| common/.../common/views/migration/ | spec/database.md | product/flows/onboarding.md |
| common/.../common/views/remote/ | spec/architecture.md | product/views/settings.md |
| common/.../common/views/contacts/ | spec/client/chat-view.md | product/views/contact-info.md |
| common/.../common/views/helpers/ | spec/architecture.md | product/concepts.md |

### Android-Specific Sources

| Source Location | Spec Document | Product Document |
|----------------|---------------|-----------------|
| android/.../app/SimplexApp.kt | spec/architecture.md, spec/client/chat-list.md, spec/client/nome-android-ui.md | product/flows/onboarding.md, product/views/chat-list.md, product/views/nome-android.md |
| android/.../app/MainActivity.kt | spec/architecture.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| android/.../app/nome/NomeProductionShell.kt | spec/client/navigation.md, spec/client/nome-android-ui.md, spec/services/theme.md | product/views/chat-list.md, product/views/nome-android.md |
| android/.../app/SimplexService.kt | spec/services/notifications.md | product/flows/messaging.md |
| android/.../app/CallService.kt | spec/services/calls.md | product/flows/calling.md |
| android/.../app/MessagesFetcherWorker.kt | spec/services/notifications.md | product/flows/messaging.md |
| android/.../app/model/NtfManager.android.kt | spec/services/notifications.md | product/flows/messaging.md |
| android/.../app/views/call/CallActivity.kt | spec/services/calls.md | product/views/call.md |
| common/src/androidMain/kotlin/chat/simplex/common/ui/nome/tokens/*.kt | spec/client/nome-android-ui.md | product/views/nome-android.md |
| common/src/androidMain/kotlin/chat/simplex/common/ui/nome/theme/NomeTheme.kt | spec/client/nome-android-ui.md, spec/services/theme.md | product/views/nome-android.md, product/views/settings.md |
| common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/*.kt | spec/client/nome-android-ui.md | product/views/nome-android.md |
| common/src/androidMain/kotlin/chat/simplex/common/ui/nome/accessibility/*.kt | spec/client/nome-android-ui.md | product/views/nome-android.md |
| common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt | spec/client/chat-list.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt | spec/client/navigation.md, spec/client/chat-list.md, spec/client/nome-android-ui.md, spec/services/theme.md | product/views/chat-list.md, product/views/nome-android.md |
| common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeSearchStateAdapter.kt | spec/client/chat-list.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeSearchRoute.android.kt | spec/client/navigation.md, spec/client/chat-list.md, spec/client/nome-android-ui.md, spec/services/theme.md | product/views/chat-list.md, product/views/nome-android.md |
| common/src/androidMain/kotlin/chat/simplex/common/ui/nome/connection/NomeConnectionPreviewStateAdapter.kt | spec/state.md, spec/client/navigation.md, spec/client/nome-android-ui.md | product/flows/connection.md, product/views/new-chat.md, product/views/nome-android.md, product/rules.md |
| common/src/androidMain/kotlin/chat/simplex/common/ui/nome/connection/NomeConnectionPreviewRoute.android.kt | spec/state.md, spec/client/navigation.md, spec/client/nome-android-ui.md, spec/services/theme.md | product/flows/connection.md, product/views/new-chat.md, product/views/nome-android.md, product/rules.md |
| common/src/androidMain/kotlin/chat/simplex/common/ui/nome/database/** | spec/database.md, spec/state.md, spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/nome-android.md, product/rules.md, product/gaps.md |
| common/src/androidMain/kotlin/chat/simplex/common/views/database/PlatformDatabaseRootRoute.android.kt | spec/database.md, spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/nome-android.md, product/rules.md |
| common/src/androidMain/kotlin/chat/simplex/common/platform/Cryptor.android.kt | spec/database.md, spec/state.md, spec/client/nome-android-ui.md | product/views/nome-android.md, product/rules.md, product/gaps.md |
| common/src/androidMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.android.kt | spec/client/navigation.md, spec/client/nome-android-ui.md | product/flows/connection.md, product/views/new-chat.md, product/views/nome-android.md |
| common/src/androidMain/kotlin/chat/simplex/common/views/chat/PlatformVerifyCodeLayout.android.kt | spec/api.md, spec/client/chat-view.md, spec/client/nome-android-ui.md, spec/services/theme.md | product/views/contact-info.md, product/views/nome-android.md, product/rules.md |
| common/src/androidMain/kotlin/chat/simplex/common/views/newchat/PlatformChannelSetupRoute.android.kt | spec/api.md, spec/client/navigation.md, spec/client/nome-android-ui.md, spec/services/theme.md | product/views/new-chat.md, product/views/nome-android.md, product/gaps.md, product/rules.md |
| common/src/androidMain/kotlin/chat/simplex/common/views/chat/PlatformChannelConversationChrome.android.kt | spec/client/chat-view.md, spec/client/nome-android-ui.md, spec/impact.md | product/views/chat.md, product/views/nome-android.md, product/rules.md |
| common/src/androidMain/kotlin/chat/simplex/common/views/usersettings/PlatformIdentityCenterRoute.android.kt | spec/client/navigation.md, spec/client/nome-android-ui.md, spec/impact.md | product/views/user-profiles.md, product/views/nome-android.md, product/rules.md |
| common/src/androidMain/kotlin/chat/simplex/common/views/usersettings/PlatformSettingsHomeRoute.android.kt | spec/client/navigation.md, spec/client/nome-android-ui.md, spec/impact.md | product/views/settings.md, product/views/nome-android.md, product/rules.md, product/gaps.md |
| common/src/androidMain/kotlin/chat/simplex/common/views/usersettings/{PlatformSettingsDetailRoute.android.kt,PlatformAboutSettingsRoute.android.kt} | spec/client/navigation.md, spec/client/nome-android-ui.md, spec/impact.md | product/views/settings.md, product/views/nome-android.md, product/rules.md |
| common/src/androidMain/res/values*/nome_settings_strings.xml | spec/client/nome-android-ui.md, spec/impact.md | product/views/nome-android.md, product/rules.md |
| common/src/androidMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatHub.android.kt | spec/client/navigation.md, spec/client/nome-android-ui.md, spec/services/theme.md | product/views/new-chat.md, product/views/nome-android.md |
| common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatHub.kt | spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/new-chat.md, product/views/nome-android.md |
| common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatHub.desktop.kt | spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/new-chat.md, product/views/nome-android.md |
| common/src/androidMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatRoute.android.kt | spec/client/navigation.md, spec/client/nome-android-ui.md, spec/services/theme.md | product/views/new-chat.md, product/views/nome-android.md, product/rules.md |
| common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatRoute.kt | spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/new-chat.md, product/views/nome-android.md, product/rules.md |
| common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatRoute.desktop.kt | spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/new-chat.md, product/views/nome-android.md, product/rules.md |
| common/src/androidMain/kotlin/chat/simplex/common/views/newchat/QRCodeScanner.android.kt | spec/client/navigation.md, spec/client/nome-android-ui.md, spec/impact.md | product/views/new-chat.md, product/views/nome-android.md, product/gaps.md, product/rules.md |
| common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt | spec/client/chat-list.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| common/src/androidMain/res/values/nome_home_strings.xml | spec/client/chat-list.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| common/src/androidMain/res/values-zh-rCN/nome_home_strings.xml | spec/client/chat-list.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| common/src/androidMain/res/values*/nome_connection_preview_strings.xml | spec/client/navigation.md, spec/client/nome-android-ui.md | product/flows/connection.md, product/views/new-chat.md, product/views/nome-android.md |
| common/src/androidMain/res/values*/nome_connections_strings.xml | spec/api.md, spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/contact-info.md, product/views/new-chat.md, product/views/nome-android.md, product/gaps.md, product/rules.md |
| android/src/debug/AndroidManifest.xml | spec/client/nome-android-ui.md | product/views/nome-android.md |
| android/src/debug/java/chat/simplex/app/nome/** | spec/client/nome-android-ui.md | product/views/nome-android.md |
| android/src/debug/res/values*/strings.xml | spec/client/nome-android-ui.md | product/views/nome-android.md |
| android/src/test/java/chat/simplex/app/nome/** | spec/client/nome-android-ui.md | product/views/nome-android.md |
| android/src/androidTest/java/chat/simplex/app/nome/** | spec/client/nome-android-ui.md | product/views/nome-android.md |
| android/src/debug/java/chat/simplex/app/nome/home/NomeHomeEvidenceActivity.kt | spec/client/chat-list.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| android/src/debug/res/values*/nome_home_evidence_strings.xml | spec/client/nome-android-ui.md | product/views/nome-android.md |
| android/src/test/java/chat/simplex/app/nome/home/NomeHomeStateAdapterTest.kt | spec/client/chat-list.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeComposeTest.kt | spec/client/chat-list.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomePackagingTest.kt | spec/client/nome-android-ui.md | product/views/nome-android.md |
| android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeScreenshotTest.kt | spec/client/chat-list.md, spec/client/nome-android-ui.md, spec/services/theme.md | product/views/chat-list.md, product/views/nome-android.md |
| android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeCoreCycleTest.kt | spec/client/chat-list.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| android/src/debug/java/chat/simplex/app/nome/connection/** | spec/client/nome-android-ui.md | product/views/nome-android.md |
| android/src/debug/res/values*/nome_connection_preview_evidence_strings.xml | spec/client/nome-android-ui.md | product/views/nome-android.md |
| android/src/test/java/chat/simplex/app/nome/connection/** | spec/state.md, spec/client/navigation.md, spec/client/nome-android-ui.md | product/flows/connection.md, product/views/nome-android.md, product/rules.md |
| android/src/androidTest/java/chat/simplex/app/nome/connection/** | spec/state.md, spec/client/navigation.md, spec/client/nome-android-ui.md, spec/services/theme.md | product/flows/connection.md, product/views/nome-android.md, product/rules.md |
| android/src/debug/java/chat/simplex/app/nome/database/NomeDatabaseRootEvidenceActivity.kt | spec/database.md, spec/client/nome-android-ui.md | product/views/nome-android.md |
| android/src/debug/res/values*/nome_database_root_evidence_strings.xml | spec/client/nome-android-ui.md | product/views/nome-android.md |
| android/src/test/java/chat/simplex/app/nome/database/** | spec/database.md, spec/state.md, spec/client/nome-android-ui.md | product/views/nome-android.md, product/rules.md |
| android/src/androidTest/java/chat/simplex/app/nome/database/** | spec/database.md, spec/client/nome-android-ui.md, spec/services/theme.md | product/views/nome-android.md, product/rules.md |

### Desktop-Specific Sources

| Source Location | Spec Document | Product Document |
|----------------|---------------|-----------------|
| desktop/.../desktop/Main.kt | spec/architecture.md | product/flows/onboarding.md |
| common/.../common/DesktopApp.kt (desktopMain) | spec/architecture.md | product/views/chat-list.md |
| common/.../common/StoreWindowState.kt (desktopMain) | spec/architecture.md | product/views/settings.md |
| common/.../common/model/NtfManager.desktop.kt (desktopMain) | spec/services/notifications.md | product/flows/messaging.md |
| common/.../common/views/helpers/AppUpdater.kt (desktopMain) | spec/architecture.md | product/views/settings.md |
| common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt | spec/client/navigation.md, spec/client/chat-list.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| common/src/desktopTest/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRouteDesktopTest.kt | spec/client/navigation.md, spec/client/chat-list.md, spec/client/nome-android-ui.md | product/views/chat-list.md, product/views/nome-android.md |
| common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.desktop.kt | spec/client/navigation.md, spec/client/nome-android-ui.md | product/flows/connection.md, product/views/new-chat.md, product/views/nome-android.md |
| common/src/desktopTest/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreviewDesktopTest.kt | spec/client/navigation.md, spec/client/nome-android-ui.md | product/flows/connection.md, product/views/new-chat.md, product/views/nome-android.md |
| common/src/desktopMain/kotlin/chat/simplex/common/views/chat/PlatformVerifyCodeLayout.desktop.kt | spec/api.md, spec/client/chat-view.md, spec/client/nome-android-ui.md | product/views/contact-info.md, product/views/nome-android.md |
| common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformChannelSetupRoute.desktop.kt | spec/api.md, spec/client/navigation.md, spec/client/nome-android-ui.md | product/views/new-chat.md, product/views/nome-android.md |

### Haskell Core Sources (at `../../src/Simplex/Chat/` relative to `apps/multiplatform/`)

| Source Location | Spec Document | Product Document |
|----------------|---------------|-----------------|
| ../../src/Simplex/Chat/Controller.hs | spec/api.md | product/concepts.md |
| ../../src/Simplex/Chat/Types.hs | spec/api.md | product/glossary.md |
| ../../src/Simplex/Chat/Core.hs | spec/architecture.md | product/concepts.md |
| ../../src/Simplex/Chat/Protocol.hs | spec/architecture.md | product/concepts.md |
| ../../src/Simplex/Chat/Messages.hs | spec/api.md | product/flows/messaging.md |
| ../../src/Simplex/Chat/Messages/CIContent.hs | spec/api.md | product/flows/messaging.md |
| ../../src/Simplex/Chat/Call.hs | spec/services/calls.md | product/flows/calling.md |
| ../../src/Simplex/Chat/Files.hs | spec/services/files.md | product/flows/file-transfer.md |
| ../../src/Simplex/Chat/Store/Messages.hs | spec/database.md | product/flows/messaging.md |
| ../../src/Simplex/Chat/Store/Groups.hs | spec/database.md | product/flows/group-lifecycle.md |
| ../../src/Simplex/Chat/Store/Direct.hs | spec/database.md | product/flows/connection.md |
| ../../src/Simplex/Chat/Store/Files.hs | spec/database.md | product/flows/file-transfer.md |
| ../../src/Simplex/Chat/Store/Profiles.hs | spec/database.md | product/views/user-profiles.md |
