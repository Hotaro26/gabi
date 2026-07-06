import re

with open("app/src/main/java/com/material/downloader/ui/DownloaderViewModel.kt", "r") as f:
    content = f.read()

# Fix 1: replace resolvedEngine == "gallery-dl" with isGallery
content = content.replace(
    'val extension = result.ext ?: (if (resolvedEngine == "gallery-dl") "jpg" else "mp4")',
    'val extension = result.ext ?: (if (isGallery) "jpg" else "mp4")'
)
content = content.replace(
    'val title = result.title ?: (if (resolvedEngine == "gallery-dl") "image" else "video")',
    'val title = result.title ?: (if (isGallery) "image" else "video")'
)
content = content.replace(
    'val targetPath = if ((resolvedEngine == "gallery-dl" || isGallery) && downloadPath.value == "Movies/Gabi") "Pictures/Gabi" else downloadPath.value',
    'val targetPath = if (isGallery && downloadPath.value == "Movies/Gabi") "Pictures/Gabi" else downloadPath.value'
)
content = content.replace(
    'val errorMsg = result.message ?: "Extraction failed"',
    'val errorMsg = result?.message ?: "Extraction failed"'
)

with open("app/src/main/java/com/material/downloader/ui/DownloaderViewModel.kt", "w") as f:
    f.write(content)

