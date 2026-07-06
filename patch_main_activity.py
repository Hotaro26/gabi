import re

with open("app/src/main/java/com/material/downloader/MainActivity.kt", "r") as f:
    content = f.read()

update_logic = """
            var updateUrl by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
            var updateVersion by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val url = java.net.URL("https://api.github.com/repos/Hotaro26/gabi/releases/latest")
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "GET"
                        if (connection.responseCode == 200) {
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            val json = org.json.JSONObject(response)
                            val tagName = json.getString("tag_name")
                            val htmlUrl = json.getString("html_url")
                            val currentVersion = "v${BuildConfig.VERSION_NAME}"
                            if (tagName != currentVersion) {
                                updateVersion = tagName
                                updateUrl = htmlUrl
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }

            if (updateUrl != null && updateVersion != null) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { updateUrl = null },
                    title = { androidx.compose.material3.Text("Update Available") },
                    text = { androidx.compose.material3.Text("A new version ($updateVersion) of Gabi is available. Would you like to update?") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(updateUrl))
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            updateUrl = null
                        }) {
                            androidx.compose.material3.Text("Update")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { updateUrl = null }) {
                            androidx.compose.material3.Text("Dismiss")
                        }
                    }
                )
            }
"""

# We'll inject it inside setContent { ... DownloaderScreen(...) }
# Wait, let's see how setContent is structured
