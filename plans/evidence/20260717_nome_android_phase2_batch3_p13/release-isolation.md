# P13 release isolation

The final debug manifest contains:

```text
chat.simplex.app.nome.connection.NomeConnectionPreviewEvidenceActivity
android:exported=false
android:excludeFromRecents=true
```

An `aapt dump xmltree` search of the arm64 release APK returned no
`NomeConnectionPreviewEvidenceActivity` or debug evidence resource match. The release APK package
remains `chat.simplex.app`, version `6.5.6` / code `358`, minSdk 28, targetSdk 35.

The debug Activity, locale mutation, synthetic fixtures, fixture strings, and renderer badge are
not production navigation or real-core evidence.

## Final post-cleanup scan

- arm64 release SHA-256:
  `cdea7d2506fc306bb17627f22126210c0afa4703b5e535b76f0b6db884c2e87b`;
- armv7 release SHA-256:
  `36dea406f9aa2cd7b1a428764dd0fb1188fb48c0f712bae43f5896c9372eb0b8`;
- release application ID/version/min/target:
  `chat.simplex.app`, `6.5.6` / `358`, API 28 / API 35;
- release manifest and dex temporary P13 receiver/TTS/harness/trace hits: zero;
- rebuilt debug manifest lifecycle-receiver hits: zero;
- rebuilt debug dex temporary P13 receiver/TTS/harness/trace hits: zero;
- rebuilt standard androidTest manifest and dex temporary service/harness/trace hits: zero;
- the expected non-exported debug evidence Activity remains present only in the debug manifest.

The release APKs are unsigned local artifacts. No production signing material was generated or
used, and no distribution or remote mutation occurred.
