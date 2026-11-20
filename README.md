# Awkward-Free

A native Android app for making informal payment reminders a little less awkward. Built with Kotlin and Jetpack Compose for friends and roommates.

## Features

- Dashboard, all dues, add/edit due, due details, settled tabs and payment settings.
- Instagram-inspired bottom navigation and the violet/indigo V2 design.
- Friendly, playful and direct messages that can be edited before opening WhatsApp.
- Optional WhatsApp number and UPI ID; reminders contain a UPI payment URI and the ID as a fallback.
- Manual settlement and reopen actions, delete confirmation, and the time WhatsApp was opened for a reminder.
- Record partial payments; outstanding balances and UPI links use the remaining amount.
- Optional daily on-device due-date reminders, sent only to you.
- Versioned local JSON backup export and confirmed restore via the Android file picker.
- On-device storage; existing entries from the original version are preserved.

The app does not send messages automatically, confirm WhatsApp delivery, process payments, or verify a UPI transfer. Confirm payment before marking a tab settled. UPI link handling depends on the recipient's messaging and payment apps. Data is local to the device and is removed if app data is cleared or the app is uninstalled.

## Build

Open in Android Studio with Android SDK 35 and JDK 17. The repository currently has no Gradle wrapper: with Gradle 8.9 installed, run `gradle wrapper --gradle-version 8.9`, then sync and run the `app` configuration on Android 8.0 or newer. GitHub Actions also builds the debug app using Gradle 8.9.

Plus Jakarta Sans, the typeface used in the Figma design, is bundled under the SIL Open Font License; see `licenses/PlusJakartaSans-OFL.txt`.

## Code structure

The Kotlin source is organized under `app/src/main/java/com/punith/awkwardfree/`:

- `ui/screens/` — Home, dues, add/edit, details, nudge, settled and settings screens.
- `ui/components/` — reusable UI elements, such as due cards and buttons.
- `ui/theme/` — colours and typography.
- `navigation/` — screen routing, app state and bottom navigation.
- `model/` — the `Due` data class.
- `data/` — local SharedPreferences storage.
- `util/` — formatting, reminder messages and UPI links.

`MainActivity.kt` sets up the app theme and launches the Compose UI. This remains a lightweight Compose state-based architecture; it does not use ViewModels or Room yet.

## Reminders and backups

Enable due-date reminders in Settings. Android may delay the approximately 9 AM notification; it summarizes due and overdue tabs without sending WhatsApp messages. Android 13+ requires notification permission.

Settings can export a JSON backup or restore one after confirmation. Restore replaces the current dues, name and UPI ID; it does not change your reminder setting. Backup files include personal and financial details, so store them privately.
