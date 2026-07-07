import os
import re

# 1. Create DownloadLogRepository in commonMain
repo_interface = """package com.material.downloader.db

import com.material.downloader.model.DownloadLog
import kotlinx.coroutines.flow.Flow

interface DownloadLogRepository {
    fun getAllLogs(): Flow<List<DownloadLog>>
    suspend fun insertLog(log: DownloadLog)
    suspend fun deleteLog(log: DownloadLog)
    suspend fun deleteAllLogs()
}
"""
os.makedirs("app/src/commonMain/kotlin/com/material/downloader/db", exist_ok=True)
with open("app/src/commonMain/kotlin/com/material/downloader/db/DownloadLogRepository.kt", "w") as f:
    f.write(repo_interface)

# 2. Create AndroidDownloadLogRepository in androidMain
android_repo = """package com.material.downloader.db

import com.material.downloader.model.DownloadLog
import kotlinx.coroutines.flow.Flow

class AndroidDownloadLogRepository(private val dao: DownloadLogDao) : DownloadLogRepository {
    override fun getAllLogs(): Flow<List<DownloadLog>> = dao.getAllLogs()
    override suspend fun insertLog(log: DownloadLog) = dao.insertLog(log)
    override suspend fun deleteLog(log: DownloadLog) = dao.deleteLog(log)
    override suspend fun deleteAllLogs() = dao.deleteAllLogs()
}
"""
os.makedirs("app/src/androidMain/kotlin/com/material/downloader/db", exist_ok=True)
with open("app/src/androidMain/kotlin/com/material/downloader/db/AndroidDownloadLogRepository.kt", "w") as f:
    f.write(android_repo)

# 3. Create DesktopDownloadLogRepository in desktopMain
desktop_repo = """package com.material.downloader.db

import com.material.downloader.model.DownloadLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DesktopDownloadLogRepository : DownloadLogRepository {
    private val logs = MutableStateFlow<List<DownloadLog>>(emptyList())
    override fun getAllLogs(): Flow<List<DownloadLog>> = logs
    override suspend fun insertLog(log: DownloadLog) {
        val current = logs.value.toMutableList()
        current.add(log)
        logs.value = current
    }
    override suspend fun deleteLog(log: DownloadLog) {
        val current = logs.value.toMutableList()
        current.remove(log)
        logs.value = current
    }
    override suspend fun deleteAllLogs() {
        logs.value = emptyList()
    }
}
"""
os.makedirs("app/src/desktopMain/kotlin/com/material/downloader/db", exist_ok=True)
with open("app/src/desktopMain/kotlin/com/material/downloader/db/DesktopDownloadLogRepository.kt", "w") as f:
    f.write(desktop_repo)

# 4. Refactor DownloaderViewModel and move to commonMain
with open("app/src/androidMain/kotlin/com/material/downloader/ui/DownloaderViewModel.kt", "r") as f:
    vm_code = f.read()

# Replace Android specific imports and dependencies
vm_code = re.sub(r'import android\.app\.Application.*?\n', '', vm_code)
vm_code = re.sub(r'import android\.content\.Intent.*?\n', '', vm_code)
vm_code = re.sub(r'import android\.net\.Uri.*?\n', '', vm_code)
vm_code = re.sub(r'import android\.os\.Build.*?\n', '', vm_code)
vm_code = re.sub(r'import android\.os\.Environment.*?\n', '', vm_code)
vm_code = re.sub(r'import androidx\.lifecycle\.AndroidViewModel.*?\n', 'import androidx.lifecycle.ViewModel\n', vm_code)
vm_code = re.sub(r'import androidx\.room\.Room.*?\n', '', vm_code)
vm_code = re.sub(r'import com\.material\.downloader\.util\.NotificationHelper.*?\n', '', vm_code)
vm_code = re.sub(r'import com\.material\.downloader\.api\.PythonExtractor.*?\n', '', vm_code)
vm_code = re.sub(r'import com\.material\.downloader\.util\.FileDownloader.*?\n', '', vm_code)
vm_code = re.sub(r'import com\.material\.downloader\.db\.AppDatabase.*?\n', 'import com.material.downloader.db.DownloadLogRepository\nimport com.material.downloader.util.PlatformBridge\n', vm_code)

# Replace class signature
vm_code = re.sub(r'class DownloaderViewModel\(application: Application\) : AndroidViewModel\(application\)', 
                 'class DownloaderViewModel(private val platform: PlatformBridge, private val logRepository: DownloadLogRepository) : ViewModel()', vm_code)

# Replace dependencies initialization
vm_code = re.sub(r'private val extractor = PythonExtractor\(\).*?\n', '', vm_code)
vm_code = re.sub(r'private val db = Room\.databaseBuilder.*?\n', '', vm_code)
vm_code = re.sub(r'private val logDao = db\.downloadLogDao\(\).*?\n', '', vm_code)
vm_code = re.sub(r'private val notificationHelper = NotificationHelper\(application\).*?\n', '', vm_code)
vm_code = re.sub(r'private val prefs = application\.getSharedPreferences.*?\n', '', vm_code)
vm_code = re.sub(r'private val downloader = FileDownloader\(application, client\).*?\n', '', vm_code)

# Replace preference usages
vm_code = vm_code.replace('prefs.getString("terminal_theme", TerminalTheme.PINK.name)', 'platform.getSetting("terminal_theme", TerminalTheme.PINK.name)')
vm_code = vm_code.replace('prefs.edit().putString("terminal_theme", theme.name).apply()', 'platform.saveSetting("terminal_theme", theme.name)')

# Replace logDao usages
vm_code = vm_code.replace('logDao.', 'logRepository.')

# Replace downloadMedia logic
vm_code = re.sub(r'downloader\.downloadFile\((.*?)\)\.collect \{ state ->', r'/* Download logic moved to PlatformBridge */', vm_code, flags=re.DOTALL)
# It's too complex to regex replace the entire flow collection logic properly. We will rewrite the download block.

# Wait, this is very dangerous to regex. We should just replace the file with a known good KMP structure.
print("Prepared files.")
