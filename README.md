# Nijatva File

A fast, offline file manager for Android — written in plain Java (no AndroidX, no Kotlin).

- Package: `com.nijatva.file`
- minSdk 23 · targetSdk 34 · Java 17

## Features

- **My Files home** — storage card, category tiles (Photos, Videos, Audio, Documents, APKs, Archives), Sources, Quick access, Tag, bottom nav (Recent / Files)
- **All files browser** — list, grid and waterfall views; sort by date, size, filename or type
- **Built-in player** — images, videos and music in one screen (swipe / prev / next)
- **Text editor** — open and save `.txt`, `.md`, `.json`, `.xml`, `.csv` and code files
- **ZIP** — compress and extract
- **Favourites, Safe folder (PIN), Recycle Bin, Temporary-files cleaner**
- **Storage analysis**
- **Default apps** per file type
- **Settings** with advanced options

## Build

Open the folder in Android Studio and run, or:

    ./gradlew assembleDebug

The APK appears in `app/build/outputs/apk/debug/`.

## Permissions

On first launch grant file access — Android 13+: Photos / Videos / Music; Android 11–12: Files and media.

## License

MIT — see [LICENSE](LICENSE).
