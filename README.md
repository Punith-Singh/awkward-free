# Awkward-Free

A native Android app for making informal payment reminders a little less awkward. Built with Kotlin and Jetpack Compose for friends and roommates.

## Features

- Dashboard, all dues, add/edit due, due details, settled tabs and payment settings.
- Instagram-inspired bottom navigation and the violet/indigo V2 design.
- Friendly, playful and direct messages that can be edited before opening WhatsApp.
- Optional WhatsApp number and UPI ID; reminders contain a UPI payment URI and the ID as a fallback.
- Manual settlement and reopen actions, delete confirmation, and the time WhatsApp was opened for a reminder.
- On-device storage; existing entries from the original version are preserved.

The app does not send messages automatically, confirm WhatsApp delivery, process payments, or verify a UPI transfer. Confirm payment before marking a tab settled. UPI link handling depends on the recipient's messaging and payment apps. Data is local to the device and is removed if app data is cleared or the app is uninstalled.

## Build

Open in Android Studio with Android SDK 35 and JDK 17. The repository currently has no Gradle wrapper: with Gradle 8.9 installed, run `gradle wrapper --gradle-version 8.9`, then sync and run the `app` configuration on Android 8.0 or newer. GitHub Actions also builds the debug app using Gradle 8.9.

The design uses Plus Jakarta Sans as the Figma fallback for Gilroy. Neither font is bundled in this build; Android's system sans-serif is used until a licensed font is supplied and added to the project.
