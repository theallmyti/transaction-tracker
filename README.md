# Transaction Tracker

[![Download APK](https://img.shields.io/badge/Download-APK-success?style=for-the-badge&logo=android)](https://github.com/theallmyti/transaction-tracker/releases/download/app/Transaction-Tracker.apk)

Transaction Tracker is a modern Android application designed to help you manage your finances with ease. Built with Jetpack Compose and Kotlin, it offers a beautiful, glassmorphic UI and automated transaction tracking via SMS parsing.

## Features

*   **Automated Tracking**: Automatically parses transaction SMS messages to populate your expense history.
*   **Dual Account View**: clear breakdown of "Main Account" (e.g., Bank) and "Slice Account" spending.
*   **Manual Entry**: Easily add cash or other manual transactions.
*   **Visual Analytics**: Interactive graphs to visualize spending trends over time.
*   **Glassmorphic Design**: A premium, modern dark-themed UI with blur effects and vibrant gradients.
*   **Privacy Focused**: All data is stored locally on your device using Room Database.

## Tech Stack

*   **Language**: Kotlin
*   **UI Toolkit**: Jetpack Compose
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **Database**: Room Persistence Library
*   **Charting**: Vico
*   **Build System**: Gradle (Kotlin DSL)

## Getting Started

1.  Clone the repository:
    ```bash
    git clone https://github.com/theallmyti/transaction-tracker.git
    ```
2.  Open the project in Android Studio.
3.  Sync Gradle files.
4.  Run the application on an emulator or physical device.
    *   *Note: Creating the app on an Emulator requires manually sending SMS messages to test the parsing logic.*

## Permissions

The app requires `READ_SMS` permission to function correctly for automated tracking. This permission is requested upon first launch.

## License

This project is open source.
