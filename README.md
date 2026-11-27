<p align="center">
  <img src="https://github.com/user-attachments/assets/c1435762-4f08-431b-b485-202ffae1800a" width="220" height="220" alt="Gabi Logo" style="border-radius: 44px;" />
</p>

<h1 align="center">Gabi</h1>

<p align="center">
  <strong>Minimal, Fast & Secure Media Downloader for Android</strong>
</p>

<p align="center">
  <a href="https://github.com/Hotaro26/gabi/releases"><img src="https://img.shields.io/github/v/release/Hotaro26/gabi?color=E55B2B&style=flat-square" alt="Latest Release" /></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin 2.0+" />
  <img src="https://img.shields.io/badge/UI-Material%203-4285F4?style=flat-square" alt="Material 3" />
  <a href="LICENSE-MIT"><img src="https://img.shields.io/badge/License-MIT-green.svg?style=flat-square" alt="License: MIT" /></a>
  <a href="https://github.com/Hotaro26/gabi/stargazers"><img src="https://img.shields.io/github/stars/Hotaro26/gabi?style=flat-square&color=yellow" alt="Stars" /></a>
</p>

<p align="center">
  <a href="#overview">Overview</a> &bull;
  <a href="#download-engines">Engines</a> &bull;
  <a href="#features">Features</a> &bull;
  <a href="#screenshots">Screenshots</a> &bull;
  <a href="#how-to-use">How to Use</a> &bull;
  <a href="#technical-specifications">Tech Stack</a> &bull;
  <a href="#building-from-source">Building</a>
</p>

---

## Overview

[**Gabi**](https://hotaro26.github.io/gabi/) is a minimal, fast, and privacy-respecting media downloader engineered for Android. Built with modern Jetpack Compose and Material 3 design, Gabi empowers you to effortlessly download videos, extract high-bitrate audio, and grab full image galleries from **over 1,000+ supported websites**.

Under the hood, Gabi orchestrates four distinct extraction engines&mdash;**`yt-dlp`**, **`gallery-dl`**, **`NewPipe Extractor`**, and **`Cobalt API`**&mdash;giving you the versatility to fetch content cleanly without ads, bloated wrappers, or tracking.

> [!NOTE]
> Gabi executes Python extraction engines (`yt-dlp` and `gallery-dl`) directly **on-device** using the Chaquopy bridge. Your downloads, links, and search queries remain entirely private to your device.

> [!TIP]
> **Pre-release builds:** Refer to the [Releases](https://github.com/Hotaro26/gabi/releases) section for experimental features, pre-release APKs, and cutting-edge engine patches.

---

## Download Engines

Gabi automatically pairs each link with the most efficient extraction engine:

| Engine | Execution Environment | Primary Platform Targets | Key Capabilities |
| :--- | :--- | :--- | :--- |
| **yt-dlp** | On-Device Python (`Chaquopy`) | YouTube, Twitch, SoundCloud, Vimeo, Facebook, 1000+ sites | Up to 1080p/4K video, audio extraction, playlist fetching |
| **gallery-dl** | On-Device Python (`Chaquopy`) | Pixiv, Pinterest, Reddit, Danbooru, Instagram | Full image set & gallery archiving, metadata preservation |
| **NewPipe Extractor** | Native Java / Kotlin | YouTube, SoundCloud, PeerTube, Bandcamp | Lightweight, zero-dependency streaming metadata parsing |
| **Cobalt API** | High-Speed Web API | TikTok, Twitter / X, Instagram, YouTube | Fast cloud extraction without heavy local compute |

---

## Features

### Media Flexibility
* **High-Definition Video**: Download videos in multiple quality streams up to 1080p and source maximum resolution.
* **Audio Extraction**: Strip and convert audio streams into standalone high-quality MP3s with full ID3 metadata.
* **Full Image Galleries**: Batch-download entire image sets and albums from photo-sharing and art communities.

### Smart Metadata Preview
* Inspect video titles, author channels, cover thumbnails, durations, and estimated file sizes *before* committing to a download.

### Instant Clipboard Action
* **Bolt Action (Instant Download)**: Tap the lightning icon to instantly fetch and process whatever URL is currently in your clipboard.
* **Android Share Target**: Share links directly from YouTube, Twitter/X, Reddit, or your browser directly into Gabi for immediate extraction.

### Material 3 Customization
* **Dynamic Material You**: Harmonizes application accent tones with your device's Android wallpaper palette.
* **Curated Themes**: Choose between *Lavender*, *Forest*, *Midnight*, *Rose*, *Monochrome*, or *Dynamic*.
* **True Dark / Light Mode**: Full system-synchronized dark and light modes, including a themed splash screen.

### Storage & History Management
* **SAF Integration**: Select your custom download directory using the native Android Storage Access Framework.
* **Download Logs**: Review, inspect, and manage your complete historical download ledger in the *Logs* tab.

---

## Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center"><img src="https://github.com/user-attachments/assets/61de43f7-8d98-4f78-bd7c-499fdbe9caf6" width="180" alt="Home Screen" /></td>
      <td align="center"><img src="https://github.com/user-attachments/assets/088cfadf-cb43-44a0-8387-aa33e428419a" width="180" alt="Metadata Preview" /></td>
      <td align="center"><img src="https://github.com/user-attachments/assets/ac151180-b4be-4018-8a30-dc2ff46167a8" width="180" alt="Download Progress" /></td>
      <td align="center"><img src="https://github.com/user-attachments/assets/adeaf938-cbfd-4a4c-bf34-988e51b2087b" width="180" alt="Format Selection" /></td>
    </tr>
    <tr>
      <td align="center"><img src="https://github.com/user-attachments/assets/f71b11c0-90be-4912-a1f4-d06fd02611d9" width="180" alt="Settings & Themes" /></td>
      <td align="center"><img src="https://github.com/user-attachments/assets/c4513fe2-b71a-4fe0-8b9a-370779714cc7" width="180" alt="Gallery Preview" /></td>
      <td align="center"><img src="https://github.com/user-attachments/assets/67db7c74-f38a-4abd-a1b7-45afafbd6863" width="180" alt="Download Logs" /></td>
      <td align="center"><img src="https://github.com/user-attachments/assets/245a40ce-37b3-49de-a7f6-4e80333e3fde" width="180" alt="Engines Configuration" /></td>
    </tr>
  </table>

  <br />

  <table>
    <tr>
      <td><img width="420" alt="Tablet Landscape 1" src="https://github.com/user-attachments/assets/2a75481c-b350-41c5-811c-7d89e4818ca1" /></td>
      <td><img width="420" alt="Tablet Landscape 2" src="https://github.com/user-attachments/assets/f135ce83-a2c5-44f0-9b49-b04c49fd49fd" /></td>
    </tr>
    <tr>
      <td colspan="2" align="center"><img width="420" alt="Tablet Landscape 3" src="https://github.com/user-attachments/assets/9fac9ea2-6060-4465-8be5-2501844d0379" /></td>
    </tr>
  </table>
</div>

<details>
  <summary><b>View Historical Screenshots</b></summary>
  <br />
  <div align="center">
    <table>
      <tr>
        <td><img width="260" src="https://github.com/user-attachments/assets/8dc91593-854e-4c7f-b41f-3b2bf470f3b0" alt="Legacy UI 1" /></td>
        <td><img width="260" src="https://github.com/user-attachments/assets/fd34b92a-5d9c-4273-ad41-4d890f76bf0d" alt="Legacy UI 2" /></td>
      </tr>
      <tr>
        <td><img width="260" src="https://github.com/user-attachments/assets/15d170e5-e465-49fa-bbdd-ed6335527fe5" alt="Legacy UI 3" /></td>
        <td><img width="260" src="https://github.com/user-attachments/assets/65daa539-6e07-466a-b88d-5a740de0abbf" alt="Legacy UI 4" /></td>
      </tr>
    </table>
  </div>
</details>

---

## How to Use

```
[ Copy Link / Share ] ──► [ Paste / Tap Bolt ] ──► [ Fetch Metadata ] ──► [ Choose Format ] ──► [ Download ]
```

1. **Paste & Fetch**: Paste any media link into the input bar or tap the **Bolt icon** to grab from your clipboard.
2. **Configure Stream**: Choose between **Video**, **Audio (MP3)**, or **Gallery**, then select your resolution.
3. **Download**: Hit **Download Now**. Track progress via Android notifications or the in-app progress indicator.
4. **Access Downloads**: Open your configured storage folder or browse completed items in the **Logs** tab.

> [!WARNING]
> **Storage Access Framework (SAF):** Ensure you grant directory access when configuring your destination folder in Settings so Gabi can write media files directly to your storage without sandbox restrictions.

---

## Technical Specifications

| Category | Technology | Details |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0+ | Modern idiomatic Kotlin with Coroutines & StateFlow |
| **UI Framework** | Jetpack Compose | Material 3 design system with dynamic theming |
| **Python Runtime** | Chaquopy Bridge | Embedded Python 3 runtime executing on-device extraction scripts |
| **Extraction Engines** | `yt-dlp`, `gallery-dl`, `NewPipe`, `Cobalt` | Hybrid multi-engine architecture |
| **Networking** | Ktor HTTP Client | Asynchronous stream handling and API communication |
| **Local Persistence** | Room Database | SQLite-backed download history and configuration persistence |
| **Image Loading** | Coil Compose | Asynchronous image loading and thumbnail caching |

---

## Building from Source

### Prerequisites
* **JDK 17** or higher
* **Android SDK 34** (installed and configured via `ANDROID_HOME`)

### Build Steps (CLI)
1. **Clone the repository:**
   ```bash
   git clone https://github.com/Hotaro26/gabi.git
   cd gabi
   ```

2. **Assemble Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   *(Chaquopy will automatically download and bundle the Python wheels for `yt-dlp` and `gallery-dl` on the initial build).*

3. **Install onto a connected device:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Support & Contributing

Gabi is crafted by **Hotaro**. If this project helps you save media, consider starring the repository or supporting ongoing development:

* **GitHub:** [@Hotaro26](https://github.com/Hotaro26)
* **Discord Community:** `flawed_mango`
* **Buy Me a Coffee:** [buymeacoffee.com/oi.hotaro](https://www.buymeacoffee.com/oi.hotaro)

<p align="center">
  <a href="https://www.buymeacoffee.com/oi.hotaro">
    <img src="https://github.com/user-attachments/assets/f62638ee-9ad9-42c6-b275-a620b4cabe25" width="160" alt="Buy Me A Coffee" />
  </a>
</p>

---

## License

This project is open-source under the **[MIT License](LICENSE-MIT)**.

Included third-party extraction tools and libraries are licensed under their respective terms:
* **yt-dlp**: The Unlicense
* **gallery-dl**: GNU General Public License v2.0 (GPLv2)
* **Chaquopy**: BSD 3-Clause
* **Cobalt API**: Open Source (AGPL-3.0)
* **NewPipe Extractor**: GPLv3

---

## Star History

<p align="center">
  <a href="https://www.star-history.com/?repos=Hotaro26%2Fgabi&type=timeline&legend=top-left">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=Hotaro26/gabi&type=timeline&theme=dark&legend=top-left" />
      <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=Hotaro26/gabi&type=timeline&legend=top-left" />
      <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=Hotaro26/gabi&type=timeline&legend=top-left" />
    </picture>
  </a>
</p>
