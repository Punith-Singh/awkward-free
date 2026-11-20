# Awkward-Free

A Kotlin and Jetpack Compose Android app for the little amounts friends owe each other. Add a due, choose a reminder tone, and share a WhatsApp message with a UPI payment link.

## First version

- Track who owes you, the amount, reason and due date; mark entries settled or reopen them.
- Store records and your UPI ID on the device. No account, server, bank access or automatic messaging.
- Preview and edit a friendly, funny or direct reminder before opening WhatsApp. The last-nudged time records when WhatsApp opens, not confirmed delivery.
- UPI links request payment; the app does not verify or process transactions. Mark a record settled after confirming payment yourself. UPI link handling varies by messaging and payment app; the reminder also includes the UPI ID as a fallback.

## Open in Android Studio

Open this folder in a current Android Studio installation with Android SDK 35 and JDK 17. This repository does not yet include a Gradle wrapper. Install Gradle 8.9 locally and run `gradle wrapper --gradle-version 8.9` from the project root, then sync and run the `app` configuration on an Android 8.0+ device or emulator. A UPI app and WhatsApp are needed to exercise their respective flows.

Records are stored only on the device and are removed if the app's data is cleared or the app is uninstalled. Do not use real payment details for testing unless intended.
