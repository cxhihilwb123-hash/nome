# Android release signing boundary

The Android build accepts release signing material without storing it in the
repository. Supply all four Gradle properties or none of them:

- `nomeReleaseStoreFile`
- `nomeReleaseStorePassword`
- `nomeReleaseKeyAlias`
- `nomeReleaseKeyPassword`

A partially configured set fails during Gradle configuration instead of
silently producing an unexpectedly unsigned or incorrectly signed artifact.

Use a credential manager or CI secret store to expose the values at build time.
Gradle also maps environment variables named
`ORG_GRADLE_PROJECT_<propertyName>` to project properties. The keystore path
must point outside the Git repository. Never put a keystore, password, secret
property file or credential value in a commit, build record, terminal capture or
support message.

## Certificate roles

Treat a Play upload certificate and an application signing certificate as
different identities:

- The upload key authenticates an AAB submitted to Google Play.
- The application signing key signs the APK delivered to users. Under Play App
  Signing, Google may hold and use that key for Play-delivered APKs.
- A directly downloaded Website APK can update an installed app only when its
  application ID and signing certificate match the installed build.

Do not assume that an AAB accepted by Play proves a Website APK has the right
certificate, or the reverse. Record and compare certificate SHA-256 values in
the Android platform build record without recording private-key material.

## Required verification

For a candidate build, record all of the following:

1. Clean source-gate output and exact source commit.
2. AAB/APK byte size and SHA-256.
3. APK signing certificate verification with `apksigner verify --verbose
   --print-certs`.
4. AAB signature verification with `jarsigner -verify`.
5. Upgrade installation over the currently distributed build, without deleting
   its application data.
6. Google Play upload/readback and Website upload/download readback as separate
   gates after explicit authorization.

Passing compile, signing or local installation is not publication evidence.
