# XISO-Extractor-Android

Android ARM64 application for extracting Xbox XISO/XDVDFS images using the upstream [antangelo/xdvdfs](https://github.com/antangelo/xdvdfs) CLI.

## Features
- Select an image with Android's system file picker.
- Select an output folder with Android's Storage Access Framework.
- Extract locally on the device; no game image is bundled in the APK.
- GitHub Actions builds a debug APK and uploads it as an artifact.

## Build and download
1. Open the **Actions** tab.
2. Select **Build Android APK** and choose **Run workflow**, or push a commit to `main`.
3. Open the completed run and download the `XISO-Extractor-Android-debug` artifact ZIP.
4. Extract `app-debug.apk` from the ZIP and install it on an ARM64 Android device.

## Notes
- The workflow compiles the upstream Rust CLI for Android ARM64 and embeds it in the APK.
- The app temporarily copies the selected image and extracted files into app cache, so ensure enough free storage.
- This supports XISO/XDVDFS images; not every standard ISO-9660 image is compatible.
- Engine source and license: https://github.com/antangelo/xdvdfs
