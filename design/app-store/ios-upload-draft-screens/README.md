# Nome iOS App Store screenshot draft

This directory contains a generated 10-slot JPEG planning package. It removes
the eleventh candidate and flattens alpha channels, but it still marks
preview-core/debug-derived screens as `needs-real-core`.

Use this check for the draft:

```bash
scripts/ios/check-app-store-screenshots.sh --dir design/app-store/ios-upload-draft-screens
```

Strict final mode must continue to fail until the `needs-real-core` files are
replaced and `MANIFEST.md` / `FINAL_BLOCKERS.md` no longer record
preview/debug/needs-real-core status:

```bash
scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens
```
