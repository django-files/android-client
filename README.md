[![GitHub Downloads](https://img.shields.io/github/downloads/django-files/android-client/total?logo=github)](https://github.com/django-files/android-client/releases/latest/download/app-release.apk)
[![GitHub Release Version](https://img.shields.io/github/v/release/django-files/android-client?logo=github)](https://github.com/django-files/android-client/releases/latest)
[![Lint](https://img.shields.io/github/actions/workflow/status/django-files/android-client/lint.yaml?logo=github&logoColor=white&label=lint)](https://github.com/django-files/android-client/actions/workflows/lint.yaml)
[![GitHub Top Language](https://img.shields.io/github/languages/top/django-files/android-client?logo=htmx)](https://github.com/django-files/android-client)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/django-files/android-client?logo=github&label=updated)](https://github.com/django-files/android-client/graphs/commit-activity)
[![GitHub Repo Size](https://img.shields.io/github/repo-size/django-files/android-client?logo=bookstack&logoColor=white&label=repo%20size)](https://github.com/django-files/android-client)
[![GitHub Discussions](https://img.shields.io/github/discussions/django-files/android-client)](https://github.com/django-files/android-client/discussions)
[![GitHub Forks](https://img.shields.io/github/forks/django-files/android-client?style=flat&logo=github)](https://github.com/django-files/android-client/forks)
[![GitHub Repo Stars](https://img.shields.io/github/stars/django-files/android-client?style=flat&logo=github)](https://github.com/django-files/android-client/stargazers)
[![GitHub Org Stars](https://img.shields.io/github/stars/django-files?style=flat&logo=github&label=org%20stars)](https://django-files.github.io/)
[![Discord](https://img.shields.io/discord/899171661457293343?logo=discord&logoColor=white&label=discord&color=7289da)](https://discord.gg/wXy6m2X8wY)

# Django Files Android App

[![GitHub Release](https://img.shields.io/github/v/release/django-files/android-client?style=for-the-badge&logo=android&label=Download%20Android%20APK&color=A4C639)](https://github.com/django-files/android-client/releases/latest/download/app-release.apk)

- [Install](#Install)
  - [Setup](#Setup)
  - [Usage](#Usage)
- [Features](#Features)
  - [Planned](#Planned)
  - [Known Issues](#Known-Issues)
  - [Troubleshooting](#Troubleshooting)
- [Screenshots](#Screenshots)
- [Support](#Support)
- [Development](#Development)
  - [Android Studio](#Android-Studio)
  - [Command Line](#Command-Line)
- [Contributing](#Contributing)

Allows you to Share or Open any file with your Django Files server.
The URL to the file is automatically copied to the clipboard and the preview is shown in the app.

Additional screenshots can be found on the website: https://django-files.github.io/android/

| Django&nbsp;Files | Link                                          |
| ----------------- | :-------------------------------------------- |
| Website           | https://django-files.github.io/               |
| GitHub            | https://github.com/django-files               |
| Server            | https://github.com/django-files/django-files  |
| iOS App           | https://github.com/django-files/ios-client    |
| Web Extension     | https://github.com/django-files/web-extension |

## Install

> [!IMPORTANT]  
> Google Play is starting Closed Testing. To be included contact us on [Discord](https://discord.gg/wXy6m2X8wY).

_If you are unsure how to install, [Obtainium](https://github.com/ImranR98/Obtainium) is highly recommended..._

[![Get on GitHub](https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/android/get80/github.png)](https://github.com/django-files/android-client/releases/latest/download/app-release.apk)
[![Get on Obtainium](https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/android/get80/obtainium.png)](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/django-files/android-client)

<details><summary>📲 Click to View QR Codes 📸</summary>

[![QR Code GitHub](https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/qr-code-github.png)](https://github.com/django-files/android-client/releases/latest/download/app-release.apk)

[![QR Code Obtainium](https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/qr-code-obtainium.png)](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/django-files/android-client)

</details>

_Note: Until published on the play store, you may need to allow installation of apps from unknown sources._

- Supports Android 8 (API 26) 2017 +

Downloading and Installing the [apk](https://github.com/django-files/android-client/releases/latest/download/app-release.apk)
should take you to the settings area to allow installation if not already enabled.
For more information, see [Release through a website](https://developer.android.com/studio/publish#publishing-website).

<details><summary>View Manual Steps to Install from Unknown Sources</summary>

1. Go to your device settings.
2. Search for "Install unknown apps" or similar.
3. Choose the app you will install the apk file from.
   - Select your web browser to install directly from it.
   - Select your file manager to open it, locate the apk and install from there.
4. Download the [Latest Release](https://github.com/django-files/android-client/releases/latest/download/app-release.apk).
5. Open the download apk in the app you selected in step #3.
6. Choose Install and Accept any Play Protect notifications.
7. The app is now installed. Proceed to the [Setup](#Setup) section below.

</details>

### Setup

You can log in via password, OAuth, or QR Code.

#### QR Code Authentication

1. [Install](#Install) and open the app on your device.
2. Go to the Django Files User Settings, and
   - Scan the QR Code with your Phone
   - Click the link from your Phone
3. Done.

#### Normal Login

1. [Install](#Install) and open the app on your device.
2. Enter the URL to your Django Files server.
3. Log in as you normally would on the website.
4. Done.

### Usage

To use, share or open any file and choose the Django Files app.
The app will then upload the file to your Django Files server.
Additionally, the URL is copied to the clipboard and the preview show in the app.

> [!TIP]  
> Swipe from the left to access the Android menu.

## Features

- Share or Open any file(s) and automatically copy the URL to the clipboard.
- Upload Previews for single media and scrolling selection for multiple files.
- Native File List with Options, Infinite Scroll, and Multi-Select Options.
- Supports Native Local Login, GitHub OAuth, Google OAuth, and Discord OAuth.
- Ability to add multiple servers and switch on the fly from the Server List menu.
- Widget with Stats, Upload Shortcut, Custom Background Update Interval, and More.
- Automatic Authentication from Authenticated Sessions with QR Code or Deep Link.

### Planned

- File Upload
  - Add Custom Upload Options
  - Add Default Upload Options
- File List
  - Download Manager
  - Album Filter Dropdown
  - List or Grid Display Option
  - Response Caching for Infinite Scroll
- File List Preview
  - Media Swiper
  - File Options
  - PDF Previews

### Known Issues

- The app gets logged out if the session expires; however, sharing continues to work.
- Uploading files from the website works; however, taking picture/recording does not.
- Logging out and deleting servers may have some unexpected results, but should work.

### Troubleshooting

Try these steps in order:

1. Fully close the app and re-open it again.
2. Clear the cache storage in App Settings.
3. Clear the App Data or re-install.

For more planned features you can check out the internal [TODO.md](TODO.md).

## Screenshots

<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/1.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/1.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/2.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/2.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/3.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/3.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/4.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/4.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/5.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/5.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/6.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/6.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/7.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/7.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/8.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/8.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/9.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/9.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/10.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/10.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/11.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/11.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/12.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/12.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/13.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/13.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/14.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/14.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/15.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/15.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/16.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/16.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/17.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/17.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/18.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/django-files/android/screenshots/18.jpg"></a>

## Support

For general help or to request a feature, see:

- Q&A Discussion: https://github.com/django-files/android-client/discussions/categories/q-a
- Request a Feature: https://github.com/django-files/android-client/discussions/categories/feature-requests

If you are experiencing an issue/bug or getting unexpected results, you can:

- Report an Issue: https://github.com/django-files/android-client/issues
- Chat with us on Discord: https://discord.gg/wXy6m2X8wY
- Provide General Feedback: [https://cssnr.github.io/feedback/](https://cssnr.github.io/feedback/?app=Django%20Files%20Android%20App)

# Development

This section briefly covers running and building in [Android Studio](#Android-Studio) and the [Command Line](#Command-Line).

> [!NOTE]  
> Building requires an `app/google-services.json` file.
> See [Google Services](#Google-Services) for more info...

## Android Studio

1. Download and Install Android Studio.

   https://developer.android.com/studio

2. Ensure that usb or wifi debugging is enabled in the Android developer settings and verify.

3. Then build or run the app on your device.
   - Import the Project
   - Run Gradle Sync

To Run: Select device and press Play ▶️

To Build:

- Select the Build Variant (debug or release)
- Build > Generate App Bundles or APK > Generate APKs

> [!NOTE]  
> Text/Code Previews use highlight.js; to install this run:  
> `bash .github/scripts/prepare.sh`

## Command Line

_Note: This section is a WIP! For more details see the [release.yaml](.github/workflows/release.yaml)._

You will need to have [ADB](https://developer.android.com/tools/adb) installed.

<details><summary>Click Here to Download and Install a Release</summary>

```shell
$ wget https://github.com/django-files/android-client/releases/latest/download/app-release.apk
$ ls
app-release.apk

$ which adb
C:\Users\Shane\Android\sdk\platform-tools\adb.EXE

$ adb devices
List of devices attached
RF9M33Z1Q0M     device

$ adb -s RF9M33Z1Q0M install app-release.apk
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

## Google Services

This app uses Firebase Google Services. Building requires a valid `google-services.json` file in the `app` directory.  
You must add `com.djangofiles.djangofiles.dev` to a Firebase campaign here: https://firebase.google.com/

To enable/disable Firebase DebugView use the following commands:

```shell
# set
adb shell setprop debug.firebase.analytics.app com.djangofiles.djangofiles.dev

# unset
adb shell setprop debug.firebase.analytics.app .none.

# check
adb shell getprop debug.firebase.analytics.app
```

Only 1 app can be in debug mode at a time and this must be set every restart.

> [!NOTE]  
> Note: To disable/enable Analytics or Crashlytics set the `manifestPlaceholders`
> in the [build.gradle.kts](app/build.gradle.kts) file to the respective values.

# Contributing

Currently, the best way to contribute to this project is to star this project on GitHub.

You can also support other related projects:

- [Django Files Server](https://github.com/django-files/django-files)
- [Django Files iOS App](https://github.com/django-files/ios-client)
- [Django Files Android App](https://github.com/django-files/android-client)
- [Django Files Web Extension](https://github.com/django-files/web-extension)
