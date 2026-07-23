# Nome official server policy

## Current designation

The user confirmed on 2026-07-23 that the SMP and XFTP services already saved
on the two retained Vivo devices are the present Nome official server
configuration. The address may change later.

No full SMP/XFTP URI, fingerprint, password, private key, invitation link, or
raw server address is retained in this evidence.

## Android behavior

- Complete saved custom SMP + XFTP set:
  - show one Nome official server group;
  - hide upstream SimpleX Chat and Flux operator rows;
  - allow entry to both existing protocol detail pages.
- Incomplete or absent custom set:
  - do not claim a complete Nome official server;
  - retain access to preset operators as a recovery path.

## Migration behavior

Future address changes remain owned by the existing official server detail
views. This batch does not hard-code the current endpoint or silently copy
server credentials into application resources.

## Clean-install boundary

This batch recognizes the saved official configuration on existing devices.
It does not auto-provision that configuration on a clean install. A secure
build/release configuration source is required before a default endpoint can
be packaged without violating the credential and evidence boundary.

