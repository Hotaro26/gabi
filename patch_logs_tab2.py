import re

with open('app/src/androidMain/kotlin/com/material/downloader/ui/DownloaderScreen.kt', 'r') as f:
    text = f.read()

target = """                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {"""
replacement = """                            Card(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {"""

text = text.replace(target, replacement)

with open('app/src/androidMain/kotlin/com/material/downloader/ui/DownloaderScreen.kt', 'w') as f:
    f.write(text)

print("LogsTab patched again!")
