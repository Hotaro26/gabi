# Gabi 🎧

**Gabi** is a crisp, fast, and secure media downloader for Android. Built with a modern Material 3 interface, it allows you to download videos, audio, and image galleries from over 1000+ supported sites with ease.

---

## ✨ Features

-   **Wide Support**: Powered by `yt-dlp` and `gallery-dl`, Gabi supports YouTube, TikTok, Instagram, Twitter/X, Reddit, Facebook, Twitch, SoundCloud, Pixiv, Pinterest, and many more.
-   **Media Flexibility**: 
    -   **Video**: Download in various qualities up to 1080p/Max.
    -   **Audio**: Extract high-quality MP3s from any video source.
    -   **Galleries**: Download full image sets from supported platforms.
-   **Smart Preview**: See metadata like title, author, thumbnail, and estimated file size before you hit download.
-   **Instant Action**: 
    -   **One-Tap Download**: Use the "Instant" button to automatically fetch and download links from your clipboard.
    -   **Share to Gabi**: Share links directly from other apps (like YouTube or Twitter) to Gabi for immediate processing.
-   **Customization**:
    -   **Themes**: Choose from Lavender, Forest, Midnight, Rose, Monochrome, or Dynamic (Material You) themes.
    -   **Dark/Light Mode**: Full support for system-wide dark and light modes, including a themed splash screen.
-   **File Management**: 
    -   Select your preferred download folder using the Android Storage Access Framework.
    -   View and manage your download history in the "Logs" tab.
-   **Modern UI**: Built entirely with Jetpack Compose for a smooth, responsive experience.

---

## 🛠️ Tech Stack

-   **Language**: Kotlin
-   **UI Framework**: Jetpack Compose (Material 3)
-   **Backend Logic**: Python (integrated via [Chaquopy](https://chaquo.com/))
-   **Download Engines**: `yt-dlp`, `gallery-dl`
-   **Networking**: Ktor Client
-   **Database**: Room (for download history)
-   **Image Loading**: Coil

---

## 🚀 How to Use

1.  **Paste & Fetch**: Paste a link in the home screen and wait for the metadata preview.
2.  **Configure**: Choose between "Media" (Video/Audio) or "Gallery" mode, then select your desired quality.
3.  **Download**: Hit "Download Now" and track the progress via the notification bar or the in-app progress indicator.
4.  **Instant**: Click the Bolt icon to instantly download whatever is currently in your clipboard.
5.  **Settings**: Customize your theme and set your preferred download directory in the Settings tab.

---

## 📦 Building from Source

To build Gabi yourself:

1.  Clone the repository:
    ```bash
    git clone https://github.com/Hotaro26/gabi.git
    ```
2.  Open the project in **Android Studio Jellyfish** (or newer).
3.  Ensure you have the **Android SDK 34** installed.
4.  Build the project. Chaquopy will automatically handle the Python dependencies (`yt-dlp`, `gallery-dl`) during the first build.

---

## 🤝 Support

Gabi is built with ❤️ by **hotaro**. If you find the app useful, consider supporting the development via UPI (found in the app's settings).

-   **GitHub**: [Hotaro26](https://github.com/Hotaro26)
-   **Discord**: `oi.hotaro`

---

## ⚖️ License

Gabi uses several open-source libraries. Please check the "Licenses" section in the app settings for full details. 
- `yt-dlp`: Unlicense
- `gallery-dl`: GPLv2
- `Chaquopy`: BSD 3-Clause
