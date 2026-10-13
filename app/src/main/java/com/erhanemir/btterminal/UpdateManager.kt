package com.erhanemir.btterminal

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** GitHub release'leri uzerinden uygulama ici guncelleme. */
object UpdateManager {

    private const val API = "https://api.github.com/repos/ErhanEmir/BT-Terminal/releases/latest"
    private const val PREF_DL_ID = "update_dl_id"
    const val APK_NAME = "BT-Terminal-update.apk"

    data class Update(val tag: String, val url: String)

    fun currentVersion(ctx: Context): String =
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }

    fun isNewer(latest: String, current: String): Boolean {
        fun parts(v: String) =
            v.trim().trimStart('v', 'V').split(".", "-", "_").map { it.toIntOrNull() ?: 0 }
        val a = parts(latest)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** silent=true ise guncelleme yoksa/hata varsa sessiz kalir. Sonuc UI thread'de gelir. */
    fun check(ctx: Context, silent: Boolean, cb: (update: Update?, error: String?) -> Unit) {
        Thread {
            var upd: Update? = null
            var err: String? = null
            try {
                val c = (URL(API).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/vnd.github+json")
                }
                val body = c.inputStream.bufferedReader().readText()
                val root = JSONObject(body)
                val tag = root.getString("tag_name")
                val assets = root.getJSONArray("assets")
                var url: String? = null
                for (i in 0 until assets.length()) {
                    val o = assets.getJSONObject(i)
                    if (o.getString("name").endsWith(".apk", ignoreCase = true)) {
                        url = o.getString("browser_download_url")
                        break
                    }
                }
                if (url != null && isNewer(tag, currentVersion(ctx))) {
                    upd = Update(tag, url)
                }
            } catch (e: Exception) {
                err = e.message
            }
            val u = upd
            val e = err
            Handler(Looper.getMainLooper()).post {
                if (u == null && e == null && silent) return@post
                cb(u, e)
            }
        }.start()
    }

    fun download(ctx: Context, url: String) {
        try {
            val dm = ctx.getSystemService(DownloadManager::class.java) ?: return
            val req = DownloadManager.Request(Uri.parse(url))
                .setTitle("BT Terminal guncelleniyor")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setMimeType("application/vnd.android.package-archive")
                .setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_DOWNLOADS, APK_NAME)
            val id = dm.enqueue(req)
            ctx.getSharedPreferences("btterm", Context.MODE_PRIVATE)
                .edit().putLong(PREF_DL_ID, id).apply()
            Toast.makeText(ctx, "Indirme basladi, bitince kurulum acilacak", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(ctx, "Indirme baslatilamadi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun install(ctx: Context) {
        val file = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_NAME)
        if (!file.exists()) {
            Toast.makeText(ctx, "APK bulunamadi", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            ctx.startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Kurulum acilamadi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    class UpdateReceiver : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            val saved = ctx.getSharedPreferences("btterm", Context.MODE_PRIVATE)
                .getLong(PREF_DL_ID, -2)
            if (id != saved) return
            val dm = ctx.getSystemService(DownloadManager::class.java) ?: return
            dm.query(DownloadManager.Query().setFilterById(id))?.use { cur ->
                if (cur.moveToFirst() &&
                    cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) ==
                    DownloadManager.STATUS_SUCCESSFUL
                ) {
                    install(ctx)
                } else {
                    Toast.makeText(ctx, "Indirme basarisiz", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
