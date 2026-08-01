import re

with open('app/src/androidMain/kotlin/com/material/downloader/ui/DownloaderScreen.kt', 'r') as f:
    text = f.read()

target = """            .padding(top = 16.dp + contentPadding.calculateTopPadding(), bottom = 24.dp + contentPadding.calculateBottomPadding()),"""
replacement = """            .padding(top = 48.dp + contentPadding.calculateTopPadding(), bottom = 24.dp + contentPadding.calculateBottomPadding()),"""

text = text.replace(target, replacement, 1) # only first occurrence, which is MainDownloaderTab

with open('app/src/androidMain/kotlin/com/material/downloader/ui/DownloaderScreen.kt', 'w') as f:
    f.write(text)

print("Home tab top padding patched!")
