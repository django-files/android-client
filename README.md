[![GitHub Downloads](https://img.shields.io/github/downloads/django-files/android-client/total?logo=github)](https://github.com/django-files/android-client/releases/latest/download/django-files.apk)
[![GitHub Release Version](https://img.shields.io/github/v/release/django-files/android-client?logo=github)](https://github.com/django-files/android-client/releases/latest)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/django-files/android-client?logo=github&label=updated)](https://github.com/django-files/android-client/graphs/commit-activity)
[![GitHub Top Language](https://img.shields.io/github/languages/top/django-files/android-client?logo=htmx)](https://github.com/django-files/android-client)
[![GitHub Repo Size](https://img.shields.io/github/repo-size/django-files/android-client?logo=bookstack&logoColor=white&label=repo%20size)](https://github.com/django-files/android-client)
[![GitHub Discussions](https://img.shields.io/github/discussions/django-files/android-client)](https://github.com/django-files/android-client/discussions)
[![GitHub Forks](https://img.shields.io/github/forks/django-files/android-client?style=flat&logo=github)](https://github.com/django-files/android-client/forks)
[![GitHub Repo Stars](https://img.shields.io/github/stars/django-files/android-client?style=flat&logo=github)](https://github.com/django-files/android-client/stargazers)
[![GitHub Org Stars](https://img.shields.io/github/stars/django-files?style=flat&logo=github&label=org%20stars)](https://django-files.github.io/)
[![Discord](https://img.shields.io/discord/899171661457293343?logo=discord&logoColor=white&label=discord&color=7289da)](https://discord.gg/wXy6m2X8wY)

# Django Files Android App

- [Install](#Install)
  - [Setup](#Setup)
- [Features](#Features)
  - [Planned](#Planned)
  - [Known Issues](#Known-Issues)
- [Building](#Building)
  - [Android Studio](#Android-Studio)
  - [Command Line](#Command-Line)
- [Development](#Development)
- [Support](#Support)
- [Contributing](#Contributing)

Allows you to Share or Open any file with your Django Files server.
The URL to the file is automatically copied to the clipboard and the preview is shown in the app.

| Resource | Resource&nbsp;Link                           |
| -------- | :------------------------------------------- |
| Website  | https://django-files.github.io/              |
| GitHub   | https://github.com/django-files              |
| Server   | https://github.com/django-files/django-files |
| iOS App  | https://github.com/django-files/ios-client   |

# Install

> Django Files is now signed starting at version 0.0.3

- [Download Latest Release](https://github.com/django-files/android-client/releases/latest/download/django-files.apk)

_Until the app is published on the play store, you must allow installation of apps from unknown sources._

1. Go to your device settings.
2. Search for "Install unknown apps" or similar.
3. Choose the app you will install the apk file from.
   - Select your web browser to install directly from it.
   - Select your file manager to open it, locate the APK and install from there.
4. [Download the Latest Release](https://github.com/django-files/android-client/releases/latest/download/django-files.apk).
5. Open the download in the app you selected in step #3.
6. Choose Install and Accept Play Protect notifications.
7. The app is now installed, proceed to [Setup](#Setup).

## Setup

1. [Install](#Install) and open the app.
2. Enter the URL to your Django Files server.
3. Log in as you normally would on the website.
4. Done! You can now share any file to your Django Files server...

# Features

- Share or Open any file and automatically copy the URL to the clipboard.
- Ability to manually change servers by entering a new URL from the Server List menu.
- Supports Local Login, GitHub OAuth, Google OAuth, Discord OAuth (w/o passkeys).

## Planned

- Ability to save multiple servers and switch between them automatically in the Server List menu.
- Ability for the app to log you in if your session is expired or when switching servers.

## Known Issues

- Login with Discord OAuth passkeys does not work.
- Login with Google OAuth gives an error; however, if you wait ~30 seconds it will succeed.
- The app gets logged out if the session expires; however, sharing continues to work.

# Building

Covers Android Studio and Command Line.

## Android Studio

1. Download and Install Android Studio.

https://developer.android.com/studio

2. Ensure that usb or wifi debugging is enabled in the Android developer settings and verify.

3. Then build or run the app on your device.
   - Import the Project
   - Run Gradle Sync

To Run. Select your device and press Play ▶️

To Build.

- Select the Build Variant (debug or release)
- Build > Generate App Bundles or APK > Generate APKs

## Command Line

You will need to have [ADB](https://developer.android.com/tools/adb) installed.

_Note: This section is a WIP and contains building instructions for the command line._

<details><summary>Click Here to Download and Install a Release</summary>

```shell
$ wget https://github.com/django-files/android-client/releases/latest/download/django-files.apk
$ ls
django-files.apk

$ which adb
C:\Users\Shane\Android\sdk\platform-tools\adb.EXE

$ adb devices
List of devices attached
RF9M33Z1Q0M     device

$ adb -s RF9M33Z1Q0M install django-files.apk
Performing Incremental Install
Serving...
All files should be loaded. Notifying the device.
Success
Install command complete in 917 ms
```

See below for more details...

</details>

1. Download and Install the Android SDK Platform Tools.

https://developer.android.com/tools/releases/platform-tools#downloads

Ensure that `adb` is in your PATH.

2. List and verify the device is connected with:

```shell
$ adb devices
List of devices attached
RF9M33Z1Q0M     device
```

3. Build a debug or release apk.

```shell
./gradlew assemble
./gradlew assembleRelease
```

_Note: Use `gradlew.bat` for Windows._

4. Then install the apk to your device with adb.

```shell
$ cd app/build/outputs/apk/debug
$ adb -s RF9M33Z1Q0M install app-debug.apk
```

```shell
$ cd app/build/outputs/apk/release
$ adb -s RF9M33Z1Q0M install app-release-unsigned.apk
```

_Note: you may have to uninstall before installing due to different certificate signatures._

For more details, see the [ADB Documentation](https://developer.android.com/tools/adb#move).

# Development

Android Studio: https://developer.android.com/studio

For now see [Install](#Install).

# Support

For general help or to request a feature, see:

- Q&A Discussion: https://github.com/django-files/android-client/discussions/categories/q-a
- Request a Feature: https://github.com/django-files/android-client/discussions/categories/feature-requests

If you are experiencing an issue/bug or getting unexpected results, you can:

- Report an Issue: https://github.com/django-files/android-client/issues
- Chat with us on Discord: https://discord.gg/wXy6m2X8wY
- Provide General Feedback: [https://cssnr.github.io/feedback/](https://cssnr.github.io/feedback/?app=Django%20Files%20Android%20App)

# Contributing

Currently, the best way to contribute to this project is to star this project on GitHub.

You can also support other related projects:

- [Django Files Server](https://github.com/django-files/django-files)
- [Django Files iOS App](https://github.com/django-files/ios-client)
- [Django Files Android App](https://github.com/django-files/android-client)
