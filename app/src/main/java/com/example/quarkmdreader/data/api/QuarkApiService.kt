package com.example.quarkmdreader.data.api

import android.content.Context
import android.content.SharedPreferences
import com.example.quarkmdreader.data.model.QuarkFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit

class QuarkApiService(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("quark_auth_prefs", Context.MODE_PRIVATE)

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getCookie(): String {
        return prefs.getString("cookie", "") ?: ""
    }

    fun saveCookie(cookie: String) {
        prefs.edit().putString("cookie", cookie).apply()
    }

    fun isLoggedIn(): Boolean {
        return getCookie().isNotEmpty()
    }

    fun clearLogin() {
        prefs.edit().remove("cookie").apply()
    }

    /**
     * 获取指定目录下的文件列表
     * pdirFid: "0" 表示根目录
     */
    suspend fun getFileList(pdirFid: String = "0"): Result<List<QuarkFile>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://drive.quark.cn/1/clouddrive/file/sort" +
                    "?pdir_fid=$pdirFid&_page=1&_size=200&_sort=file_type:asc,updated_at:desc"
            val request = Request.Builder()
                .url(url)
                .header("Cookie", getCookie())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "https://pan.quark.cn/")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("空响应"))
            val json = JSONObject(body)

            if (json.optInt("code", -1) != 0 && json.optInt("status", -1) != 200) {
                val msg = json.optString("message", "获取文件列表失败")
                return@withContext Result.failure(Exception(msg))
            }

            val dataObj = json.optJSONObject("data")
            val listArray = dataObj?.optJSONArray("list") ?: JSONArray()
            val files = mutableListOf<QuarkFile>()

            for (i in 0 until listArray.length()) {
                val item = listArray.getJSONObject(i)
                files.add(
                    QuarkFile(
                        fid = item.optString("fid"),
                        fileName = item.optString("file_name"),
                        pdirFid = item.optString("pdir_fid"),
                        isDir = item.optInt("file_type") == 0,
                        size = item.optLong("size", 0L),
                        updatedAt = item.optLong("updated_at", 0L),
                        formatType = item.optString("format_type", "")
                    )
                )
            }
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取文件在线下载临时直链
     */
    suspend fun getDownloadUrl(fid: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://drive.quark.cn/1/clouddrive/file/download"
            val jsonBody = JSONObject().apply {
                put("fids", JSONArray().put(fid))
            }
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Cookie", getCookie())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "https://pan.quark.cn/")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("空响应"))
            val json = JSONObject(body)

            val dataArray = json.optJSONArray("data")
            if (dataArray != null && dataArray.length() > 0) {
                val downloadUrl = dataArray.getJSONObject(0).optString("download_url")
                if (downloadUrl.isNotEmpty()) {
                    return@withContext Result.success(downloadUrl)
                }
            }
            Result.failure(Exception("获取下载链接失败: $body"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 在线流式读取 Markdown 文件的文本内容（纯内存读取，不写磁盘）
     */
    suspend fun readMarkdownContent(fid: String): Result<String> = withContext(Dispatchers.IO) {
        val downloadUrlRes = getDownloadUrl(fid)
        if (downloadUrlRes.isFailure) {
            return@withContext Result.failure(downloadUrlRes.exceptionOrNull()!!)
        }
        val downloadUrl = downloadUrlRes.getOrNull()!!

        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("Cookie", getCookie())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val content = response.body?.string() ?: ""
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据当前目录和相对路径递归查找图片文件的 fid
     * 例如 relativePath: "images/screenshot.png"
     */
    suspend fun findFileFidByRelativePath(currentDirFid: String, relativePath: String): String? {
        val parts = relativePath.split("/").filter { it.isNotEmpty() && it != "." }
        var currFid = currentDirFid

        for (i in parts.indices) {
            val isLast = i == parts.size - 1
            val segmentName = parts[i]

            val fileListResult = getFileList(currFid)
            if (fileListResult.isFailure) return null

            val files = fileListResult.getOrNull() ?: return null
            val match = files.find { it.fileName.equals(segmentName, ignoreCase = true) } ?: return null

            if (isLast) {
                return match.fid
            } else {
                if (!match.isDir) return null
                currFid = match.fid
            }
        }
        return null
    }

    /**
     * 获取图片文件的流（用于 Coil 内存解码，避免写入本地存储）
     */
    suspend fun getFileInputStream(fid: String): InputStream? = withContext(Dispatchers.IO) {
        val downloadUrlRes = getDownloadUrl(fid)
        if (downloadUrlRes.isFailure) return@withContext null
        val downloadUrl = downloadUrlRes.getOrNull() ?: return@withContext null

        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("Cookie", getCookie())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            response.body?.byteStream()
        } catch (e: Exception) {
            null
        }
    }
}
