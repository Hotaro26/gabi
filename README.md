<p align="center">
<img width="300" height="300" alt="gabi11" src="https://github.com/user-attachments/assets/c1435762-4f08-431b-b485-202ffae1800a" />


</p>

## Gabi

[Gabi](https://hotaro26.github.io/gabi/) is a minimal, fast, and secure media downloader for Android. Built with a modern Material 3 interface, it allows you to download videos, audio, and image galleries from over 1000+ supported sites with ease. Right now, it has 3 engines, **yt-dlp**, **gallery-dl** **newpipe extractor** and **cobalt api**!!

---
> PLEASE CONSIDER GIVING A ⭐STAR TO THIS PROJECT! (to support a teenage dev)
 
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

<table>
  <tr>
    <td><img width="430" alt="Screenshot_20260630-102122_Gabi" src="https://github.com/user-attachments/assets/85727742-fc5c-44dc-8f56-6f9056605cb5" /></td>
    <td><img width="430" alt="Screenshot_20260630-102131_Gabi" src="https://github.com/user-attachments/assets/80a94b9f-a480-4e95-b333-14b5befe287d" /></td>
    <td><img width="430" alt="Screenshot_20260630-105536_Gabi" src="https://github.com/user-attachments/assets/7cae2796-a4d2-42a0-8b28-ed07d7f5a324" /></td>
  </tr>
</table>



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

Gabi is built with ❤️ by **hotaro**. If you find the app useful, consider supporting the development via UPI (found in the app's settings).

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

<a href="https://www.star-history.com/?repos=Hotaro26%2Fgabi&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=Hotaro26/gabi&type=date&theme=dark&legend=top-left&sealed_token=vBaJLd7cVa5eqo_uk972owipwGjs6zMU_0vVyPAY9ACBSIi8qz8en2-XvFkQyExOpSAmSiuMBeWCbYGSVAW_X6LOL_1rmMlvVO0Q3lhTr2lxPiHXvmp94VJVOE32CxN1HuRo204CJ-2r-_-EMJ3OhxIwp7dKJD5AReCCuwkQikV-3ItegHpZWRvDBdQ2" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=Hotaro26/gabi&type=date&legend=top-left&sealed_token=vBaJLd7cVa5eqo_uk972owipwGjs6zMU_0vVyPAY9ACBSIi8qz8en2-XvFkQyExOpSAmSiuMBeWCbYGSVAW_X6LOL_1rmMlvVO0Q3lhTr2lxPiHXvmp94VJVOE32CxN1HuRo204CJ-2r-_-EMJ3OhxIwp7dKJD5AReCCuwkQikV-3ItegHpZWRvDBdQ2" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=Hotaro26/gabi&type=date&legend=top-left&sealed_token=vBaJLd7cVa5eqo_uk972owipwGjs6zMU_0vVyPAY9ACBSIi8qz8en2-XvFkQyExOpSAmSiuMBeWCbYGSVAW_X6LOL_1rmMlvVO0Q3lhTr2lxPiHXvmp94VJVOE32CxN1HuRo204CJ-2r-_-EMJ3OhxIwp7dKJD5AReCCuwkQikV-3ItegHpZWRvDBdQ2" />
 </picture>
</a>

