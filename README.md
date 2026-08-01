<div align="center">
<img width="300" height="300" alt="gabi11" src="https://github.com/user-attachments/assets/c1435762-4f08-431b-b485-202ffae1800a" />

 ## Gabi

  <img alt="Kotlin" src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Kotlin/kotlin1.svg" />
  
  <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Android/android1.svg" />
</div>

</br>

[Gabi](https://hotaro26.github.io/gabi/) is a minimal, fast, and secure media downloader for Android. Built with a modern Material 3 interface, it allows you to download videos, audio, and image galleries from over 1000+ supported sites with ease. Right now, it has 3 engines, **yt-dlp**, **gallery-dl** **newpipe extractor** and **cobalt api**!!

---
> PLEASE CONSIDER GIVING A ⭐STAR TO THIS PROJECT IF THIS APP HAS BEEN USEFUL TO YOU! (also to support a teenage dev, stars motivates me to keep going for new future updates and features)
 
> [!TIP]
> Refer to [pre-release](https://github.com/Hotaro26/gabi/releases/tag/v3.4) section for experimental feature.

##  Features

-   **Wide Support**: Powered by `yt-dlp` and `gallery-dl` and `cobalt`Gabi supports YouTube, TikTok, Instagram, Twitter/X, Reddit, Facebook, Twitch, SoundCloud, Pixiv, Pinterest, and many more.
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
## Screenshots

## 📱 App Screenshots

| | | | |
|:---:|:---:|:---:|:---:|
| <img width="180" alt="Screenshot 1" src="https://github.com/user-attachments/assets/61de43f7-8d98-4f78-bd7c-499fdbe9caf6" /> | <img width="180" alt="Screenshot 2" src="https://github.com/user-attachments/assets/088cfadf-cb43-44a0-8387-aa33e428419a" /> | <img width="180" alt="Screenshot 4" src="https://github.com/user-attachments/assets/ac151180-b4be-4018-8a30-dc2ff46167a8" /> | <img width="180" alt="Screenshot 5" src="https://github.com/user-attachments/assets/adeaf938-cbfd-4a4c-bf34-988e51b2087b" /> |
| <img width="180" alt="Screenshot 6" src="https://github.com/user-attachments/assets/f71b11c0-90be-4912-a1f4-d06fd02611d9" /> | <img width="180" alt="Screenshot 7" src="https://github.com/user-attachments/assets/c4513fe2-b71a-4fe0-8b9a-370779714cc7" /> | <img width="180" alt="Screenshot 8" src="https://github.com/user-attachments/assets/67db7c74-f38a-4abd-a1b7-45afafbd6863" /> | <img width="180" alt="Screenshot 9" src="https://github.com/user-attachments/assets/245a40ce-37b3-49de-a7f6-4e80333e3fde" /> |

<table>
  <tr>
    <td><img width="430" alt="Screenshot_20260630-101533" src="https://github.com/user-attachments/assets/2a75481c-b350-41c5-811c-7d89e4818ca1" /></td>
    <td><img width="430" alt="Screenshot_20260630-101700" src="https://github.com/user-attachments/assets/f135ce83-a2c5-44f0-9b49-b04c49fd49fd" /></td>
  </tr>
  <tr>
    <td colspan="2" align="center"><img width="430" alt="Screenshot_20260630-103157" src="https://github.com/user-attachments/assets/9fac9ea2-6060-4465-8be5-2501844d0379" /></td>
  </tr>
</table>



<details>
  <summary>📸 Old Screenshots</summary>
  <br>

  <table>
    <tr>
      <td><img width="270" src="https://github.com/user-attachments/assets/8dc91593-854e-4c7f-b41f-3b2bf470f3b0"/></td>
      <td><img width="270" src="https://github.com/user-attachments/assets/fd34b92a-5d9c-4273-ad41-4d890f76bf0d"/></td>
    </tr>
    <tr>
      <td><img width="270" src="https://github.com/user-attachments/assets/15d170e5-e465-49fa-bbdd-ed6335527fe5"/></td>
      <td><img width="270" src="https://github.com/user-attachments/assets/65daa539-6e07-466a-b88d-5a740de0abbf"/></td>
    </tr>
  </table>
</details>

##  Tech Stack

-   **Language**: Kotlin
-   **UI Framework**: Jetpack Compose (Material 3)
-   **Backend Logic**: Python (integrated via [Chaquopy](https://chaquo.com/))
-   **Download Engines**: `yt-dlp`, `gallery-dl` `cobalt`
-   **Networking**: Ktor Client
-   **Database**: Room (for download history)
-   **Image Loading**: Coil

---

##  How to Use

1.  **Paste & Fetch**: Paste a link in the home screen and wait for the metadata preview.
2.  **Configure**: Choose between "Media" (Video/Audio) or "Gallery" mode, then select your desired quality.
3.  **Download**: Hit "Download Now" and track the progress via the notification bar or the in-app progress indicator.
4.  **Instant**: Click the Bolt icon to instantly download whatever is currently in your clipboard.
5.  **Settings**: Customize your theme and set your preferred download directory in the Settings tab.

---

##  Building from Source

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

Gabi is built with ❤️ by **hotaro**. If you find the app useful, consider supporting the development via UPI (Totally Optional).
or Just ⭐ star the project!

-   **GitHub**: [Hotaro26](https://github.com/Hotaro26)
-   **Discord**: `oi.hotaro`

---

##  License

Gabi uses several open-source libraries. Please check the "Licenses" section in the app settings for full details. 
- `yt-dlp`: Unlicense
- `gallery-dl`: GPLv2
- `Chaquopy`: BSD 3-Clause
- `cobalt` : open source

## Star History

<a href="https://www.star-history.com/?repos=Hotaro26%2Fgabi&type=timeline&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=Hotaro26/gabi&type=timeline&theme=dark&legend=top-left&sealed_token=1l72CIUSTiEMuTXUmfQa5OhtYxkKo-zSohhCTKfpwWYx_lqM_COLx3aGfhNqmudUEYojuOqPMcU_tx7EECp84m6CXzZGYUx4dbKn4Teft5fjq4LNlG3yovJb_fwqShUJXcotLvwPi-lgjfFLScWrLNlVPedgC9k2ykWd7geHUQ9baeqvuqT61ZVBKGzR" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=Hotaro26/gabi&type=timeline&legend=top-left&sealed_token=1l72CIUSTiEMuTXUmfQa5OhtYxkKo-zSohhCTKfpwWYx_lqM_COLx3aGfhNqmudUEYojuOqPMcU_tx7EECp84m6CXzZGYUx4dbKn4Teft5fjq4LNlG3yovJb_fwqShUJXcotLvwPi-lgjfFLScWrLNlVPedgC9k2ykWd7geHUQ9baeqvuqT61ZVBKGzR" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=Hotaro26/gabi&type=timeline&legend=top-left&sealed_token=1l72CIUSTiEMuTXUmfQa5OhtYxkKo-zSohhCTKfpwWYx_lqM_COLx3aGfhNqmudUEYojuOqPMcU_tx7EECp84m6CXzZGYUx4dbKn4Teft5fjq4LNlG3yovJb_fwqShUJXcotLvwPi-lgjfFLScWrLNlVPedgC9k2ykWd7geHUQ9baeqvuqT61ZVBKGzR" />
 </picture>
</a>
