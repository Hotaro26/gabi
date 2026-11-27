package com.material.downloader.util

import android.app.Application
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unified diagnostic logger for Gabi.
 * Uses a single prominent tag 'GabiApp' that can be reliably filtered via ADB:
 *   adb logcat -v time -s GabiApp AndroidRuntime:E
 */
object GabiLogger {
    const val TAG = "GabiApp"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var appFilesDir: File? = null

    fun installCrashHandler(application: Application) {
        appFilesDir = application.filesDir
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTrace = sw.toString()

                val report = buildString {
                    appendLine("=================================================================")
                    appendLine("                     GABI CRASH REPORT                           ")
                    appendLine("=================================================================")
                    appendLine("Time:        ${dateFormat.format(Date())}")
                    appendLine("Device:      ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                    appendLine("Android OS:  API ${Build.VERSION.SDK_INT} (Android ${Build.VERSION.RELEASE})")
                    appendLine("Thread:      ${thread.name} (id: ${thread.id})")
                    appendLine("Exception:   ${throwable.javaClass.name}: ${throwable.message}")
                    appendLine("-----------------------------------------------------------------")
                    appendLine("Stack Trace:")
                    appendLine(stackTrace)
                    appendLine("=================================================================")
                }

                // Log prominently to logcat under GabiApp tag
                Log.e(TAG, report)

                // Write report to crash_dump.txt in internal storage
                try {
                    val file = File(application.filesDir, "crash_dump.txt")
                    file.writeText(report)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write crash dump file: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in crash handler: ${e.message}")
            } finally {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
        i("CrashHandler", "Gabi crash handler successfully installed (SDK ${Build.VERSION.SDK_INT})")
    }

    fun d(subtag: String, message: String) {
        Log.d(TAG, "[$subtag] $message")
    }

    fun i(subtag: String, message: String) {
        Log.i(TAG, "[$subtag] $message")
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(subtag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(TAG, "[$subtag] $message\n${Log.getStackTraceString(throwable)}")
        } else {
            Log.w(TAG, "[$subtag] $message")
        }
    }

    fun e(subtag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, "[$subtag] ERROR: $message\n${Log.getStackTraceString(throwable)}")
        } else {
            Log.e(TAG, "[$subtag] ERROR: $message")
        }
    }

    fun getCrashDump(): String? {
        val dir = appFilesDir ?: return null
        val file = File(dir, "crash_dump.txt")
        return if (file.exists()) file.readText() else null
    }
}
