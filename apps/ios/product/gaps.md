# SimpleX Chat iOS -- Known Gaps & Recommendations

> Aggregation of `[GAP]` and `[REC]` annotations discovered during specification analysis. Organized by product area.
>
> **Related spec:** [spec/README.md](../spec/README.md)

---

## UI: Error Feedback

### GAP: No user-visible error on FFI command failure
**Source:** [spec/architecture.md](../spec/architecture.md)
API calls via `chatApiSendCmd` return `APIResult<R>` which can be `.error(ChatError)`. Not all error cases surface user-visible feedback in the UI.

**REC:** Audit all `chatApiSendCmd` call sites and ensure `.error` cases show appropriate alerts or banners.

---

## UI: Loading States

### GAP: No loading indicator during initial chat list population
**Source:** [spec/client/chat-list.md](../spec/client/chat-list.md)
When `ChatModel.chatInitialized` transitions to `true`, the chat list appears fully formed. There is no intermediate loading state for users with large numbers of chats.

**REC:** Add a progress indicator during `apiGetChats` for users with 100+ conversations.

---

## Flows: Group Lifecycle

### GAP: Bulk member role change — API supports batch but UI uses single-member calls
**Source:** [spec/api.md](../spec/api.md)
`APIMembersRole` accepts `NonEmpty GroupMemberId`, supporting batch role changes at the API level. However, the iOS UI (`GroupMemberInfoView.swift`) currently invokes it with a single member at a time.

**REC:** Expose batch role change in the UI for group admins managing large groups.

---

## Security

### GAP: Database passphrase not enforced by default
**Source:** [spec/database.md](../spec/database.md)
Database encryption is optional and requires the user to manually set a passphrase. New installations start with an unencrypted database.

**REC:** Consider prompting users to set a database passphrase during onboarding, especially on devices without hardware encryption.

### GAP: No forward secrecy indicator in UI
**Source:** [product/glossary.md](glossary.md)
While the double-ratchet protocol provides forward secrecy, there is no UI indicator showing whether a specific conversation has achieved forward secrecy (i.e., completed initial key exchange ratcheting).

**REC:** Add a security indicator in contact/group info showing ratchet state.

---

## Documentation

### GAP: Haskell Store layer not fully specified
**Source:** [spec/database.md](../spec/database.md)
The Haskell Store modules (`Store/Direct.hs`, `Store/Groups.hs`, `Store/Messages.hs`, etc.) are referenced by function name but not fully specified with parameter types and return types.

**REC:** Expand database spec with key Store function signatures as the specification matures.

---

## Nome Product Overlay

### GAP: Preview UI is not real-core communication evidence
**Source:** [spec/client/nome-brand-overlay.md](../spec/client/nome-brand-overlay.md)
Nome preview hosts can render deterministic invitation, group, conversation and identity states without proving a production Haskell core, signed-device execution or message delivery.

**REC:** Keep preview, simulator, generic-device, physical-device, real-communication and distribution evidence in separate gates and never promote an earlier gate to a later claim.

### GAP (resolved 2026-07-21): Offline iOS device was reported as connected
**Source:** [spec/architecture.md](../spec/architecture.md#9-device-readiness-evidence)
The physical-device readiness parser originally kept reading through the
`Devices Offline` section emitted by current Xcode, allowing an offline iPhone
to appear as connected evidence.

**Resolution:** Connected-device parsing now stops at every subsequent
`xctrace` section header and fixture tests cover connected, offline-only, mixed,
and future-section output.

### GAP: Nome release identity is not established
**Source:** [product/views/nome-experience.md](views/nome-experience.md)
The migrated source retains compatibility Bundle IDs, App Group and keychain identifiers. Nome-owned signing, domains, support/legal pages and store identity remain unresolved.

**REC:** Treat release identity as a separate migration with data-access, extension, associated-domain and rollback testing.

### GAP (resolved 2026-07-21): Fixed brand navy reduced dark-appearance readability
**Source:** [spec/client/nome-brand-overlay.md](../spec/client/nome-brand-overlay.md)
The migrated Nome overlay originally reused fixed deep navy for both brand fills and text. A complete dark-appearance smoke exposed low-contrast titles, labels and icons on black or dark-gray surfaces.

**Resolution:** Nome palettes now resolve foreground navy to the system label color in dark appearance while retaining fixed deep navy only for brand fills with a contrasting white foreground. Light and dark 14-state smoke remain separate evidence gates.

### GAP (resolved 2026-07-21): Accessibility-size trust UI truncated and overpainted messages
**Source:** [spec/client/nome-brand-overlay.md](../spec/client/nome-brand-overlay.md)
The onboarding trust pills used a fixed one-line row, the conversation safety
card could be vertically compressed while its children rendered outside the
assigned slot, and the custom message viewport could draw over neighboring
views. The disappearing-message prompt also used a fixed single-line height and
an unlabeled 28-point close button.

**Resolution:** Accessibility-size onboarding pills now stack with intrinsic
height; the safety card stacks its chips, preserves its intrinsic height and
clips the message viewport to its own layout slot; the prompt wraps and exposes
a labeled 44-point dismiss target. Accepted evidence covers light/dark
`accessibility-large` matrices and light/dark maximum-size conversation stress.

---
