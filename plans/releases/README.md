# Nome release records

This directory is the durable index for coordinated Nome client releases. A
release branch is temporary; the records and tags described here are not.

## One release line, multiple platform results

Create one temporary client branch from the accepted unified authority:

```text
release/nome-v<marketing-version>
```

Core, Android, iOS, macOS and Windows changes for that version use this one
release line. Do not create permanent Android, iOS, macOS or Windows authority
branches. Website/control-plane work remains in the independent Website
repository and is referenced from the release record when it participates in a
release.

Platform results stay independent. Each platform gets its own build number,
build record, artifact hash, signing result, verification matrix and publication
readback. A pass on one platform never marks another platform as passed.

## Durable release identity

Every coordinated version keeps all of the following after the temporary branch
is retired:

1. One release record copied from [`TEMPLATE.md`](./TEMPLATE.md).
2. One platform build record copied from [`../builds/TEMPLATE.md`](../builds/TEMPLATE.md)
   for every produced artifact.
3. An annotated platform source tag for every approved published artifact:

   ```text
   nome-v<version>-android-b<build>
   nome-v<version>-ios-b<build>
   nome-v<version>-macos-b<build>
   nome-v<version>-windows-b<build>
   ```

4. An optional annotated umbrella tag `nome-v<version>` on the commit that
   finalizes this release record, only after the declared platform set is frozen.
   Platform source tags remain the authority for the exact source of each
   artifact.
5. SHA-256, byte size, signing evidence and publication readback for every
   artifact.

Tags bind source identity; they do not prove that a package was signed, uploaded,
approved by a store or publicly available. Those are separate fields in the
platform build and release records.

The existing upstream-style `v6.5.6` tag predates this convention and does not
identify the current Nome unified release. Never move or reuse it. Nome-owned
release tags use the `nome-v` prefix to avoid that ambiguity.

## Artifact storage

Do not commit APK, AAB, IPA, app archives, DMG, PKG, MSI or other large binaries
to this Git repository.

- Local retained artifacts:
  `/Users/forkman03/project/nome/deliverables/releases/nome-v<version>/<platform>/`
- Public Website downloads: Website R2, with a checked-in manifest and readback
  evidence in the Website repository.
- Store submissions: the store's immutable build/upload identity, recorded in
  the corresponding platform build record.
- Git: release records, build records, compact logs, manifests and
  `SHA256SUMS.txt` only.

An artifact is considered the same artifact only when its byte size and SHA-256
match. A filename or version label alone is insufficient.

## Release lifecycle

1. Start `release/nome-v<version>` from a verified descendant of
   `codex/nome-v656-unified` after feature freeze.
2. Commit only release fixes and release metadata to that branch. Square or any
   other unfinished feature stays out unless it was explicitly accepted before
   the branch point.
3. Build each platform from a clean committed source state and create its build
   record. Different platform build numbers are expected.
4. After explicit authorization, create the annotated platform tag on the exact
   source commit used for that artifact, then upload or submit it.
5. Record the remote tag readback and the Website/store publication readback.
6. Merge every release-only fix back into the unified client authority before
   retiring the temporary branch.
7. Retire or delete the temporary branch/worktree only after the merge-back and
   cleanup gates pass and deletion is explicitly authorized.

Branch creation and local builds do not authorize a push, tag, deployment,
store submission or public release. Each external mutation still requires
explicit authorization.

The current Android API 36 toolchain assessment is recorded in
[`20260807_android_api36_toolchain_assessment.md`](./20260807_android_api36_toolchain_assessment.md).

## Status vocabulary

Use only these release-level states:

- `DRAFT`: record exists; the declared release set is not frozen.
- `CANDIDATE`: at least one platform artifact exists, but publication is not
  complete.
- `PARTIAL`: publication was intentionally completed for only a declared subset
  of platforms.
- `PUBLISHED`: every platform declared in the release record has publication
  readback.
- `WITHDRAWN`: a previously available release was withdrawn; retain the record
  and explain why.

Never infer `PUBLISHED` from a successful build, a tag, an upload request or one
platform's store state.
