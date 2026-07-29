# Nome build records

Every device or distributable build must record the following fields. Unknown
historical values must be marked `not recorded`; they must never be inferred
from a later commit or artifact.

Use `scripts/release/check-nome-build-source.sh` before and after packaging.
The gate requires an authorized consolidated branch, rejects tracked, staged,
and untracked files, permits ignored caches, fingerprints configuration without
printing its values, and binds an optional artifact size/hash to the same clean
commit.

Build numbers increase monotonically per platform and distribution identity.
Android, iOS, Desktop, and Web status must be recorded separately; a successful
build or install on one platform does not imply another passed.

Copy `TEMPLATE.md` for each new build. A formal release requires
`Controlled dirty patch: no` and a separate authorization for distribution.
