package com.material.downloader

import android.os.Bundle
import android.content.Intent
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.material.downloader.ui.DownloaderScreen
import com.material.downloader.ui.DownloaderViewModel
import com.material.downloader.ui.theme.ExpressiveTheme
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        CoroutineScope(Dispatchers.IO).launch {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this@MainActivity))
                val py = Python.getInstance()
                val customPath = java.io.File(filesDir, "python_packages").absolutePath
                py.getModule("sys").get("path")?.callAttr("insert", 0, customPath)
            }
        }
        
        org.schabi.newpipe.extractor.NewPipe.init(com.material.downloader.api.OkHttpDownloader())

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
        
        val imageLoader = coil.ImageLoader.Builder(this)
            .components {
                add(coil.decode.VideoFrameDecoder.Factory())
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)

        enableEdgeToEdge()
        setContent {
            val viewModel: DownloaderViewModel = viewModel()
            
            // Handle shared intent on start
            LaunchedEffect(intent) {
                handleIntent(intent, viewModel)
            }


            val darkTheme = when (viewModel.themeMode.intValue) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            ExpressiveTheme(
                darkTheme = darkTheme,
                theme = viewModel.selectedTheme.value
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DownloaderScreen(viewModel)
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?, viewModel: DownloaderViewModel) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                val urlRegex = "(https?://[^\\s]+)".toRegex()
                val match = urlRegex.find(sharedText)
                val extractedUrl = match?.value ?: sharedText
                viewModel.handleSharedUrl(extractedUrl)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
