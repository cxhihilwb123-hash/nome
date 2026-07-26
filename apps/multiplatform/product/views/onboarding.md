# Onboarding

> **Related spec:** [spec/client/navigation.md](../../spec/client/navigation.md)

## Purpose

First-time setup flow for new users. Guides through the Nome introduction, profile creation, database passphrase setup (Desktop), official-service selection, a local-use commitment, Nome address creation, and notification configuration (Android). Also provides an entry point for device migration.

## Route / Navigation

- **Entry point**: App launch when `onboardingStage` is not `OnboardingComplete`
- **Presented by**: `OnboardingView` renders the appropriate step based on `OnboardingStage` enum
- **Flow direction**: Linear progression controlled by `appPrefs.onboardingStage`
- **Completion**: Sets `onboardingStage` to `OnboardingComplete`

## Onboarding Stages

The `OnboardingStage` enum defines the flow:

| Stage | Description |
|---|---|
| `Step1_SimpleXInfo` | Welcome screen with app introduction |
| `Step2_CreateProfile` | Create first user profile |
| `LinkAMobile` | Desktop-only: link a mobile device |
| `Step2_5_SetupDatabasePassphrase` | Desktop-only: set database encryption passphrase |
| `Step3_ChooseServerOperators` | Confirm the Nome official service and local-use commitment |
| `Step3_CreateSimpleXAddress` | Create a Nome contact address |
| `Step4_SetNotificationsMode` | Android-only: configure notification mode |
| `OnboardingComplete` | Onboarding finished |

## Page Sections

### Step 1: Welcome / Nome Info (`SimpleXInfo`)

**Stage**: `Step1_SimpleXInfo`

| Element | Description |
|---|---|
| Logo | `SimpleXLogo` -- compatibility-named renderer that presents the transparent Nome mark |
| Info button | `OnboardingInformationButton` -- "The next generation of private messaging"; taps open `HowItWorks` fullscreen modal |
| Privacy redefined | `InfoRow` with privacy icon: "No user identifiers" |
| Immune to spam | `InfoRow` with shield icon: "You decide who can connect" |
| Decentralized | `InfoRow` with decentralized icon: "Anybody can host servers" |
| **Create your profile** button | `OnboardingActionButton` -- primary action; advances to profile creation |
| **Migrate from another device** button | `TextButtonBelowOnboardingButton` -- opens `MigrateToDeviceView` fullscreen modal |

Android keeps the bounded shared flow. Nome macOS uses `PlatformNomeWelcomePage` and the neutral
desktop onboarding shell: no legacy blue gradient panel, a transparent mark, white content
surfaces, green primary actions, and step-specific footer controls.

### Step 2: Create Profile

**Stage**: `Step2_CreateProfile`

| Element | Description |
|---|---|
| Display name field | Required text input; auto-focused |
| Validation | Name validation with `mkValidName` check |
| Create button | Creates profile via API; advances to next step |

Profile is stored locally and only shared with contacts.

### Step 2.5: Setup Database Passphrase (Desktop only)

**Stage**: `Step2_5_SetupDatabasePassphrase`

| Element | Description |
|---|---|
| Passphrase field | Secure text input for database encryption key |
| Confirm field | Passphrase confirmation |
| Set button | Encrypts database with passphrase |

### Link a Mobile (Desktop only)

**Stage**: `LinkAMobile`

| Element | Description |
|---|---|
| Instructions | How to connect mobile device to desktop |
| QR code | Connection QR code for mobile scanning |
| Skip button | Skip this step |

### Step 3: Choose Nome Service

**Stage**: `Step3_ChooseServerOperators`

| Element | Description |
|---|---|
| Operator list | The enabled Nome official message and file service |
| Commitment | Local privacy, connection-safety, and data-recovery reminders |
| Continue button | Keep the selected service and complete onboarding |

Managed by `ChooseServerOperators.kt`. Nome macOS does not show or accept the embedded upstream
operator conditions; its commitment is local product guidance and completion only persists the
selected Nome operator configuration.

### Step 3b: Create Nome Address

**Stage**: `Step3_CreateSimpleXAddress`

| Element | Description |
|---|---|
| Address creation | Auto-creates a Nome contact address |
| QR code | Displays the created address as QR code |
| Share button | Share address link |
| Skip button | Skip address creation |

### Step 4: Set Notifications Mode (Android only)

**Stage**: `Step4_SetNotificationsMode`

| Element | Description |
|---|---|
| Notification options | Instant (background service) / Periodic (every 10 min) / Off |
| Description | Explains battery impact and notification behavior for each mode |
| Continue button | Saves selection and completes onboarding |

Managed by `SetNotificationsMode.kt`.

### What's New (`WhatsNewView`)

Shown after onboarding or when triggered from Settings:

| Element | Description |
|---|---|
| Version highlights | New features and changes in the current version |
| Updated conditions | Notice about updated server operator conditions (if applicable) |
| Close button | Dismisses the view |

`StartPartOfScreen` runs `ChatListNoticeEffect` above the platform home seam. Android retains the
upstream-compatible release/conditions notice behavior. Nome macOS exits the effect before opening
those upstream-owned surfaces.

## Source Files

| File | Path |
|---|---|
| `OnboardingView.kt` | `views/onboarding/OnboardingView.kt` |
| `SimpleXInfo.kt` | `views/onboarding/SimpleXInfo.kt` |
| `HowItWorks.kt` | `views/onboarding/HowItWorks.kt` |
| `SetupDatabasePassphrase.kt` | `views/onboarding/SetupDatabasePassphrase.kt` |
| `SetNotificationsMode.kt` | `views/onboarding/SetNotificationsMode.kt` |
| `ChooseServerOperators.kt` | `views/onboarding/ChooseServerOperators.kt` |
| `WhatsNewView.kt` | `views/onboarding/WhatsNewView.kt` |
| `PlatformNomeOnboardingPages.desktop.kt` | `desktopMain/views/onboarding/PlatformNomeOnboardingPages.desktop.kt` |
| `PlatformOnboardingBrand.desktop.kt` | `desktopMain/views/onboarding/PlatformOnboardingBrand.desktop.kt` |
| `LinkAMobileView.kt` | `views/onboarding/LinkAMobileView.kt` |
