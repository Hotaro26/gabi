import re

with open("app/src/main/java/com/material/downloader/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("var updateUrl by androidx.compose.runtime.remember", "val updateUrl = androidx.compose.runtime.remember")
content = content.replace("var updateVersion by androidx.compose.runtime.remember", "val updateVersion = androidx.compose.runtime.remember")
content = content.replace("updateVersion = tagName", "updateVersion.value = tagName")
content = content.replace("updateUrl = htmlUrl", "updateUrl.value = htmlUrl")

content = content.replace("if (updateUrl != null && updateVersion != null)", "if (updateUrl.value != null && updateVersion.value != null)")
content = content.replace("onDismissRequest = { updateUrl = null }", "onDismissRequest = { updateUrl.value = null }")
content = content.replace("A new version ($updateVersion) of Gabi", 'A new version (${updateVersion.value}) of Gabi')
content = content.replace("android.net.Uri.parse(updateUrl)", "android.net.Uri.parse(updateUrl.value)")
content = content.replace("updateUrl = null", "updateUrl.value = null")

content = content.replace("BuildConfig.VERSION_NAME", "com.material.downloader.BuildConfig.VERSION_NAME")

with open("app/src/main/java/com/material/downloader/MainActivity.kt", "w") as f:
    f.write(content)

