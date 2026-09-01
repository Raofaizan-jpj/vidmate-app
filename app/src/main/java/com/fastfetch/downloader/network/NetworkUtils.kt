package com.fastfetch.downloader.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.webkit.MimeTypeMap
import com.fastfetch.downloader.model.UrlPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

object NetworkUtils {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isValidUrl(urlStr: String): Boolean {
        if (urlStr.isBlank()) return false
        val trimmed = urlStr.trim()
        return (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) &&
                trimmed.length > 8
    }

    fun getDomainHost(urlStr: String): String {
        return try {
            val uri = URI(urlStr.trim())
            uri.host ?: "unknown domain"
        } catch (e: Exception) {
            "unknown domain"
        }
    }

    fun deriveFileName(urlStr: String, contentDisposition: String?, contentType: String?): String {
        // Try content disposition first
        if (!contentDisposition.isNullOrBlank()) {
            val cd = contentDisposition
            if (cd.contains("filename*=", ignoreCase = true)) {
                val index = cd.indexOf("filename*=", ignoreCase = true)
                val raw = cd.substring(index + 10).trim()
                val value = raw.split("''").lastOrNull()?.replace("\"", "") ?: raw
                if (value.isNotBlank()) return cleanFileName(URLDecoder.decode(value, "UTF-8"))
            }
            if (cd.contains("filename=", ignoreCase = true)) {
                val index = cd.indexOf("filename=", ignoreCase = true)
                val raw = cd.substring(index + 9).split(";")[0].trim().replace("\"", "")
                if (raw.isNotBlank()) return cleanFileName(raw)
            }
        }

        // Try extracting from URL path
        try {
            val uri = URI(urlStr.trim())
            val path = uri.path
            if (!path.isNullOrEmpty() && path.contains("/")) {
                val nameFromPath = path.substringAfterLast("/")
                if (nameFromPath.isNotBlank() && nameFromPath.contains(".")) {
                    return cleanFileName(URLDecoder.decode(nameFromPath, "UTF-8"))
                }
            }
        } catch (e: Exception) {
            // Ignore URI parsing fallback
        }

        // Extension from content type fallback
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType) ?: "file"
        return "download_${System.currentTimeMillis()}.$ext"
    }

    private fun cleanFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    fun getMimeType(fileName: String, headerContentType: String?): String {
        if (!headerContentType.isNullOrBlank() && headerContentType != "application/octet-stream") {
            return headerContentType.split(";")[0].trim()
        }
        val ext = fileName.substringAfterLast(".", "").lowercase()
        if (ext.isNotEmpty()) {
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            if (mime != null) return mime
        }
        return headerContentType?.split("; ")?.firstOrNull() ?: "*/*"
    }

    suspend fun previewUrl(context: Context, urlStr: String): UrlPreview = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable(context)) {
            return@withContext UrlPreview(
                url = urlStr,
                fileName = "file",
                mimeType = "*/*",
                contentLength = -1L,
                domainHost = getDomainHost(urlStr),
                isReachable = false,
                errorMessage = "No internet connection. Please check your network and try again."
            )
        }

        if (!isValidUrl(urlStr)) {
            return@withContext UrlPreview(
                url = urlStr,
                fileName = "file",
                mimeType = "*/*",
                contentLength = -1L,
                domainHost = getDomainHost(urlStr),
                isReachable = false,
                errorMessage = "Please enter a valid HTTP or HTTPS URL."
            )
        }

        try {
            val request = Request.Builder()
                .url(urlStr)
                .head()
                .header("User-Agent", "FastFetchDownloader/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful && response.code != 405) {
                // If HEAD fails or is forbidden, try a short GET request range 0-0
                val getRequest = Request.Builder()
                    .url(urlStr)
                    .header("Range", "bytes=0-0")
                    .header("User-Agent", "FastFetchDownloader/1.0")
                    .build()
                val getResponse = okHttpClient.newCall(getRequest).execute()
                if (!getResponse.isSuccessful) {
                    return@withContext UrlPreview(
                        url = urlStr,
                        fileName = "file",
                        mimeType = "*/*",
                        contentLength = -1L,
                        domainHost = getDomainHost(urlStr),
                        isReachable = false,
                        errorMessage = "The server could not be reached (HTTP ${getResponse.code})."
                    )
                }
                val cd = getResponse.header("Content-Disposition")
                val ct = getResponse.header("Content-Type")
                val cl = getResponse.header("Content-Range")?.split("/")?.lastOrNull()?.toLongOrNull() ?: getResponse.body?.contentLength() ?: -1L
                val name = deriveFileName(urlStr, cd, ct)
                val mime = getMimeType(name, ct)
                getResponse.close()

                return@withContext UrlPreview(
                    url = urlStr,
                    fileName = name,
                    mimeType = mime,
                    contentLength = cl,
                    domainHost = getDomainHost(urlStr),
                    isReachable = true
                )
            }

            val cd = response.header("Content-Disposition")
            val ct = response.header("Content-Type")
            val cl = response.header("Content-Length")?.toLongOrNull() ?: -1L
            val name = deriveFileName(urlStr, cd, ct)
            val mime = getMimeType(name, ct)
            response.close()

            UrlPreview(
                url = urlStr,
                fileName = name,
                mimeType = mime,
                contentLength = cl,
                domainHost = getDomainHost(urlStr),
                isReachable = true
            )
        } catch (e: Exception) {
            UrlPreview(
                url = urlStr,
                fileName = "file",
                mimeType = "*/*",
                contentLength = -1L,
                domainHost = getDomainHost(urlStr),
                isReachable = false,
                errorMessage = "Failed to connect to server: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
