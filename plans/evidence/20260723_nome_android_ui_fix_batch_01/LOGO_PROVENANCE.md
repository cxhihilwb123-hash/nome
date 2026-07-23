# Logo provenance and resource specification

## Authority

The local brand authority is `/Users/forkman03/project/nome/design/design/brand/README.md`. It states that the folder contains the approved Nome logo direction and identifies `nome-logo.png` as the canonical primary horizontal logo and `nome-lockup-dark.png` as the dark-background lockup.

Approved inputs:

| Input | Specification | SHA256 |
|---|---|---|
| `design/design/brand/nome-logo.png` | 700 x 285, RGB | `c7781d1154e734dccbcc2e5da76b89d1686052399b241435510af99c79b28f15` |
| `design/design/brand/nome-lockup-dark.png` | 620 x 190, RGB | `4cc8486083ff7b6efc3beace4e039b830259f40436a31fe1278e28bf00a7db39` |
| `design/design/product/nome-mark-transparent.png` | 310 x 320, RGBA | `621e68b971c6e8b71509db06184b5198c5710478e23d9c4713f9318bce1b709c` |

No network asset was downloaded and no new brand geometry was drawn.

## Previous Android resource

- `drawable-nodpi/nome_header_logo.png`
- 91 x 37, RGB, no alpha
- SHA256 `1c30615446230f229c4ba12b513aeaddbc48497c63d50d204ebfd78c3e0b475c`

This resource required large high-density upscaling and carried an opaque background.

## New Android resources

| Resource | Specification | SHA256 |
|---|---|---|
| `drawable-nodpi/nome_header_logo.png` | 700 x 285, 8-bit RGBA | `ffe97ea23fa157ad77e2e6f2054cf998108f75e22d2c1a31d7558ffdf7153cac` |
| `drawable-night-nodpi/nome_header_logo.png` | 700 x 285, 8-bit RGBA | `4b346151e26cc03d48c0c8d8d99957e1ab3d8514afb150dc50d4ba379ed863e9` |

The light lockup keeps the canonical wordmark geometry and uses the approved transparent mark to preserve its white internal detail without a white plate. The night lockup uses the approved dark-lockup shapes on transparency. Both exports share a 700:285 canvas and are rendered with `ContentScale.Fit`, so width-only call sites cannot stretch them.

## Final header presentation

After user review on the protected device screens, the P07 and P23 header call sites were increased from 66 dp to 84 dp wide. The 700:285 renderer derives a height of approximately 34.2 dp; there is no fixed-height stretch. P07 UI hierarchy measured 221 x 90 px on V2048A at 420 dpi and 252 x 103 px on V2047A at 480 dpi. Existing onboarding (104 dp), About (112 dp), and lock-screen (68 dp) presentation was intentionally left unchanged.

## Evidence limit

Static inspection proves alpha, dimensions, source lineage, day/night resource selection, and preserved aspect ratio. Current device screenshot/recording protection was kept enabled, so final pixel-level sharpness and edge quality on both Vivo panels require user-visible confirmation.
