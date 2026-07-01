package com.example.trae624.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.trae624.R
import com.example.trae624.TraeApp
import com.example.trae624.data.model.PracticeRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val GITHUB_OWNER = "mooliii"
        private const val GITHUB_REPO = "trae624"
        private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        private const val REQUEST_EXPORT = 1001
        private const val REQUEST_IMPORT = 1002
    }

    private val repo by lazy { (application as TraeApp).repository }
    private var currentVersionCode = 0
    private var latestVersionInfo: UpdateInfo? = null
    private var isCheckingUpdate = false
    private lateinit var badgeUpdate: TextView
    private lateinit var tvUpdateStatus: TextView

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val downloadUrl: String,
        val changelog: String,
        val mirrorDownloadUrl: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "设置"

        val pkgInfo = try { packageManager.getPackageInfo(packageName, 0) } catch (e: Exception) { null }
        val versionName = pkgInfo?.versionName ?: "1.0.0"
        currentVersionCode = parseVersionCode(versionName)

        findViewById<TextView>(R.id.tvVersion).text = "v$versionName"
        badgeUpdate = findViewById(R.id.badgeUpdate)
        tvUpdateStatus = findViewById(R.id.tvUpdateStatus)

        setupButtons()
        // 进入设置时自动检查更新（静默模式，只更新 UI 状态）
        checkUpdate(interactive = false)
    }

    private fun setupButtons() {
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardExport)?.setOnClickListener {
            exportData()
        }
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardImport)?.setOnClickListener {
            importData()
        }
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardUpdate)?.setOnClickListener {
            checkUpdate(interactive = true)
        }
    }

    private fun exportData() {
        lifecycleScope.launch {
            try {
                val records = withContext(Dispatchers.IO) { repo.getAnsweredRecords() }
                val json = JSONArray()
                records.forEach { r ->
                    val obj = JSONObject().apply {
                        put("questionId", r.questionId)
                        put("isCorrect", r.isCorrect)
                        put("isAnswered", r.isAnswered)
                        put("isFavorite", r.isFavorite)
                        put("isConquered", r.isConquered)
                        put("userAnswer", r.userAnswer)
                    }
                    json.put(obj)
                }

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "trae624_backup.json")
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                startActivityForResult(intent, REQUEST_EXPORT)
                pendingExportData = json.toString()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "导出失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var pendingExportData: String? = null

    private fun importData() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_IMPORT)
    }

    @Deprecated("Override forActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            REQUEST_EXPORT -> {
                data?.data?.let { uri ->
                    pendingExportData?.let { jsonData ->
                        lifecycleScope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    contentResolver.openOutputStream(uri)?.use { os ->
                                        OutputStreamWriter(os).use { it.write(jsonData) }
                                    }
                                }
                                Toast.makeText(this@SettingsActivity, "导出成功", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(this@SettingsActivity, "导出失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            REQUEST_IMPORT -> {
                data?.data?.let { uri -> importDataFrom(uri) }
            }
        }
    }

    private fun importDataFrom(uri: Uri) {
        lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        BufferedReader(InputStreamReader(input)).readText()
                    } ?: return@withContext null
                } ?: return@launch

                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val record = PracticeRecord(
                        questionId = obj.getInt("questionId"),
                        isCorrect = obj.getBoolean("isCorrect"),
                        isAnswered = obj.getBoolean("isAnswered"),
                        isFavorite = obj.getBoolean("isFavorite"),
                        isConquered = obj.getBoolean("isConquered"),
                        userAnswer = obj.optString("userAnswer", "")
                    )
                    repo.saveRecord(record)
                }
                Toast.makeText(this@SettingsActivity, "导入成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "导入失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUpdate(interactive: Boolean = true) {
        if (isCheckingUpdate) return
        isCheckingUpdate = true
        lifecycleScope.launch {
            try {
                val info = fetchLatestRelease()
                if (info == null) {
                    if (interactive) {
                        Toast.makeText(this@SettingsActivity, "检查失败，请稍后重试", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                latestVersionInfo = info
                if (info.versionCode > currentVersionCode) {
                    badgeUpdate.visibility = android.view.View.VISIBLE
                    badgeUpdate.setBackgroundResource(R.drawable.circle_red)
                    tvUpdateStatus.visibility = android.view.View.VISIBLE
                    tvUpdateStatus.text = "v${info.versionName} 可更新"
                    tvUpdateStatus.setTextColor(0xFFE53935.toInt())
                    if (interactive) {
                        showUpdateDialog(info)
                    }
                } else {
                    badgeUpdate.visibility = android.view.View.VISIBLE
                    badgeUpdate.setBackgroundResource(R.drawable.circle_green)
                    tvUpdateStatus.visibility = android.view.View.VISIBLE
                    tvUpdateStatus.text = "已是最新版本"
                    tvUpdateStatus.setTextColor(0xFF4CAF50.toInt())
                    if (interactive) {
                        Toast.makeText(this@SettingsActivity, "已是最新版本", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                if (interactive) {
                    Toast.makeText(this@SettingsActivity, "检查失败，请稍后重试", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isCheckingUpdate = false
            }
        }
    }

    private fun showUpdateDialog(info: UpdateInfo) {
        AlertDialog.Builder(this)
            .setTitle("发现新版本 v${info.versionName}")
            .setMessage(
                SpannableString(info.changelog).apply {
                    setSpan(
                        AbsoluteSizeSpan(12, true), // 12sp字体大小
                        0,
                        info.changelog.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            )
            .setPositiveButton("下载更新") { _, _ -> downloadAndInstall(info) }
            .setNegativeButton("稍后再说", null)
            .show()
    }

    private fun downloadAndInstall(info: UpdateInfo) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_download, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val tvStatus = dialogView.findViewById<TextView>(R.id.tvStatus)

        val dialog = AlertDialog.Builder(this)
            .setTitle("下载更新")
            .setView(dialogView)
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            try {
                val apkFile = downloadFile(info, progressBar, tvStatus)
                dialog.dismiss()
                // 检查下载的 APK 版本，与当前版本一致则不安装
                val apkVersionCode = getApkVersionCode(apkFile)
                if (apkVersionCode > 0 && apkVersionCode <= currentVersionCode) {
                    apkFile.delete()
                    Toast.makeText(this@SettingsActivity, "已是最新版本，无需更新", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                installApk(apkFile)
            } catch (e: Exception) {
                dialog.dismiss()
                val msg = when {
                    e.message?.contains("Unable to resolve host") == true -> "网络连接失败，请检查网络"
                    e.message?.contains("timeout") == true -> "下载超时，请稍后重试"
                    e.message?.contains("403") == true -> "下载受限，请稍后重试"
                    e.message?.contains("failed to connect") == true -> "网络连接失败，无法访问更新服务器"
                    else -> "下载失败：${e.message}"
                }
                Toast.makeText(this@SettingsActivity, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun downloadFile(
        info: UpdateInfo,
        progressBar: ProgressBar,
        tvStatus: TextView
    ): java.io.File = withContext(Dispatchers.IO) {
        val urlsToTry = mutableListOf(info.downloadUrl)
        if (info.mirrorDownloadUrl.isNotBlank() && info.mirrorDownloadUrl != info.downloadUrl) {
            urlsToTry.add(0, info.mirrorDownloadUrl) // 镜像优先
        }

        var lastException: Exception? = null
        for (urlStr in urlsToTry) {
            try {
                return@withContext downloadFromUrl(urlStr, progressBar, tvStatus)
            } catch (e: Exception) {
                lastException = e
                // 镜像失败则继续尝试主地址，否则直接抛出
                if (urlStr == info.mirrorDownloadUrl && info.mirrorDownloadUrl.isNotBlank()) {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "镜像下载失败，尝试主服务器..."
                    }
                }
            }
        }
        throw lastException ?: Exception("所有下载地址均不可用")
    }

    private suspend fun downloadFromUrl(
        urlStr: String,
        progressBar: ProgressBar,
        tvStatus: TextView
    ): java.io.File = withContext(Dispatchers.IO) {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "trae624")
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        val input = conn.inputStream
        val total = conn.contentLengthLong
        val file = java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "trae624_update.apk")
        if (file.exists()) file.delete()
        val output = java.io.FileOutputStream(file)
        val buffer = ByteArray(65536)
        var downloaded = 0L

        withContext(Dispatchers.Main) {
            if (total > 0) {
                progressBar.max = 100
                progressBar.isIndeterminate = false
            } else {
                progressBar.isIndeterminate = true
            }
            tvStatus.text = "正在下载..."
        }

        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            downloaded += read
            if (total > 0) {
                val percent = ((downloaded * 100) / total).toInt()
                withContext(Dispatchers.Main) {
                    progressBar.progress = percent
                    tvStatus.text = "正在下载... $percent%"
                }
            }
        }
        output.close()
        input.close()
        file
    }

    private fun getApkVersionCode(apkFile: java.io.File): Int {
        return try {
            val pkgInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            val vName = pkgInfo?.versionName ?: return -1
            parseVersionCode(vName)
        } catch (e: Exception) { -1 }
    }

    private fun installApk(file: java.io.File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", file
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(installIntent)
    }

    private suspend fun fetchLatestRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "trae624")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            if (conn.responseCode != 200) return@withContext null

            val response = BufferedReader(InputStreamReader(conn.inputStream)).readText()
            val json = JSONObject(response)
            val tagName = json.optString("tag_name", "").removePrefix("v")
            val body = json.optString("body", "暂无更新说明")

            val assets = json.optJSONArray("assets") ?: return@withContext null
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name", "").endsWith(".apk")) {
                    downloadUrl = asset.optString("browser_download_url")
                    break
                }
            }
            if (downloadUrl == null) return@withContext null

            // 从更新说明中解析镜像下载地址，格式：镜像下载:https://...
            var mirrorUrl = ""
            body.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("镜像下载:", ignoreCase = true) ||
                    trimmed.startsWith("Mirror:", ignoreCase = true)) {
                    mirrorUrl = trimmed.substringAfter(":").trim()
                }
            }

            UpdateInfo(
                versionCode = parseVersionCode(tagName),
                versionName = tagName,
                downloadUrl = downloadUrl,
                changelog = body,
                mirrorDownloadUrl = mirrorUrl
            )
        } catch (e: Exception) { null }
    }

    private fun parseVersionCode(versionName: String): Int {
        return versionName.split(".").mapIndexed { i, s ->
            (s.toIntOrNull() ?: 0) * when (i) { 0 -> 10000; 1 -> 100; else -> 1 }
        }.sum()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
