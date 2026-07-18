# P13 owner-proof production boundary

Date: 2026-07-18 (Asia/Shanghai)

Verdict: `OwnerVerification.Verified` and `OwnerVerification.Failed` are **not production
reachable inside P13 on the v6.5.6 route**. They remain renderer/model states with deterministic
coverage. No owner signature was constructed, extracted, printed, or retained to reach this
verdict.

## Source proof

1. Android `MainActivity.processIntent` converts an external `ACTION_VIEW` into `AppOpenUrl` with
   only `remoteHostId`, `uri`, and `source=ExternalActionView`.
2. `AppOpenUrl` has exactly those three fields; it has no `LinkOwnerSig`.
3. `connectIfOpenedViaUri` is the only production caller that opts into the Android P13 entry
   policy. Its `planAndConnect` call supplies the URI and policy but omits `linkOwnerSig`, so the
   declared default is `null`.
4. The two production UI call sites that do possess `mc.ownerSig` are the framed chat item and
   chat-list preview. Both call `planAndConnect(..., linkOwnerSig=mc.ownerSig, ...)` without a
   presentation policy, so the declared default remains `Legacy`.
5. A P13 retry carries the original `linkOwnerSig`; because the only P13 producer starts with
   `null`, retry cannot introduce a signature.
6. The core `verifyLinkOwner` implementation is `fmap` over `Maybe LinkOwnerSig`. With `Nothing`,
   it returns `Nothing`; `OVVerified` or `OVFailed` can only be constructed when a signature was
   supplied.

The production partitions are therefore disjoint:

```text
external ACTION_VIEW -> P13 + linkOwnerSig=null -> ownerVerification=null
signed in-chat link  -> Legacy + linkOwnerSig=Just(...) -> owner verification may be returned
```

## Reproduction anchors

- `apps/multiplatform/android/src/main/java/chat/simplex/app/MainActivity.kt`
  (`processIntent`)
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt`
  (`AppOpenUrl`)
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt`
  (`connectIfOpenedViaUri`)
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/chat/item/FramedItemView.kt`
  and `views/chatlist/ChatPreviewView.kt` (signed in-chat link producers)
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/newchat/ConnectPlan.kt`
  (`planAndConnect` defaults, typed P13 plan, retry)
- `src/Simplex/Chat/Library/Commands.hs` (`connectPlan`, `verifyLinkOwner`)

The focused common test `ownerVerificationMapsOnlyReturnedOwnerFact` still verifies that P13's
display model faithfully maps a typed verified or failed fact if future product routing makes
one reachable. Debug renderer rows and screenshots prove presentation only and make no real-core
claim.
