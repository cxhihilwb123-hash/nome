# Nome Dark Token v1 contrast report

The API 35 Compose test evaluates opaque token pairs with the WCAG relative-luminance formula. Text/control pairs require at least 4.5:1; the secondary control outline requires at least 3:1. All measured pairs pass.

## Light

| Pair | Ratio |
|---|---:|
| primary text / background | 17.31:1 |
| secondary text / background | 4.97:1 |
| tertiary text / background | 4.93:1 |
| tertiary text / subtle surface | 4.60:1 |
| tertiary text / container surface | 4.71:1 |
| primary action | 4.58:1 |
| accent action | 4.53:1 |
| information state | 5.85:1 |
| success state | 5.40:1 |
| warning state | 5.20:1 |
| danger state | 6.05:1 |
| destructive action | 6.57:1 |
| secondary control outline | 4.93:1 |

## Dark

| Pair | Ratio |
|---|---:|
| primary text / background | 16.41:1 |
| primary text / surface | 15.71:1 |
| secondary text / background | 9.40:1 |
| tertiary text / background | 6.07:1 |
| tertiary text / subtle surface | 5.81:1 |
| tertiary text / container surface | 5.29:1 |
| primary action | 4.58:1 |
| accent action | 4.53:1 |
| information state | 5.89:1 |
| success state | 7.87:1 |
| warning state | 8.19:1 |
| danger state | 6.94:1 |
| destructive action | 7.75:1 |
| secondary control outline | 5.81:1 |

The native dark screenshots also keep icon + localized title/description/stateDescription cues for loading, offline, error, permission, and danger; color is not the sole state signal. This report validates foundation token pairs, not arbitrary future imagery, overlays, or production-page combinations.

