import re

with open('app/src/androidMain/kotlin/com/material/downloader/ui/DownloaderScreen.kt', 'r') as f:
    text = f.read()

text = text.replace("RoundedCornerShape(12.dp)", "RoundedCornerShape(24.dp)")

with open('app/src/androidMain/kotlin/com/material/downloader/ui/DownloaderScreen.kt', 'w') as f:
    f.write(text)

print("Radius changed to 24.dp")
