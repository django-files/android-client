package com.djangofiles.djangofiles.api

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.InputStream
import java.net.URLConnection


// TODO: Pass preferences instead of context since context is not used
class ServerApi(context: Context, host: String) {
    val api: ApiService
    val authToken: String
    val preferences: SharedPreferences =
        context.getSharedPreferences("AppPreferences", MODE_PRIVATE)

    private lateinit var cookieJar: SimpleCookieJar
    private lateinit var client: OkHttpClient

    init {
        api = createRetrofit(host).create(ApiService::class.java)
        authToken = preferences.getString("auth_token", null) ?: ""
    }

    suspend fun upload(fileName: String, inputStream: InputStream): Response<FileResponse> {
        Log.d("upload", "fileName: $fileName")
        val multiPart: MultipartBody.Part = inputStreamToMultipart(inputStream, fileName)
        return api.postUpload(authToken, multiPart)
    }

    suspend fun shorten(url: String): Response<ShortResponse> {
        Log.d("shorten", "url: $url")
        return api.postShort(authToken, url)
    }

    // TODO: Use VersionResponse
    suspend fun version(version: String): Response<ResponseBody> {
        Log.d("version", "version: $version")
        return api.postVersion(authToken, VersionRequest(version))
    }

    interface ApiService {
        @Multipart
        @POST("upload")
        suspend fun postUpload(
            @Header("Authorization") token: String,
            @Part file: MultipartBody.Part,
            @Header("Format") format: String? = null,
            @Header("Expires-At") expiresAt: String? = null,
            @Header("Strip-GPS") stripGps: String? = null,
            @Header("Strip-EXIF") stripExif: String? = null,
            @Header("Private") private: String? = null,
            @Header("Password") password: String? = null,
        ): Response<FileResponse>

        @POST("shorten")
        suspend fun postShort(
            @Header("Authorization") token: String,
            @Header("URL") url: String? = null,
            @Header("Vanity") vanity: String? = null,
            @Header("Max-Views") maxViews: Number? = null,
        ): Response<ShortResponse>

        // TODO: Use VersionResponse
        @POST("version")
        suspend fun postVersion(
            @Header("Authorization") token: String,
            @Body version: VersionRequest
        ): Response<ResponseBody>
    }

    data class FileResponse(
        val url: String,
        val raw: String,
        val name: String,
        val size: Long
    )

    data class ShortResponse(
        val url: String,
        val vanity: String,
        @SerializedName("max-views") val maxViews: Number,
    )

    data class VersionRequest(val version: String)
    data class VersionResponse(
        val version: String,
        val valid: Boolean,
    )

    private suspend fun inputStreamToMultipart(
        file: InputStream,
        fileName: String
    ): MultipartBody.Part {
        val contentType =
            URLConnection.guessContentTypeFromName(fileName) ?: "application/octet-stream"
        Log.d("inputStreamToMultipart", "contentType: $contentType")
        val bytes = withContext(Dispatchers.IO) { file.readBytes() }
        val requestBody = bytes.toRequestBody(contentType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("file", fileName, requestBody)
    }

    private fun createRetrofit(host: String): Retrofit {
        val baseUrl = "${host}/api/"
        Log.d("createRetrofit", "baseUrl: $baseUrl")
        cookieJar = SimpleCookieJar()
        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    inner class SimpleCookieJar : CookieJar {
        private val cookieStore = mutableMapOf<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }

        //fun setCookie(url: HttpUrl, rawCookie: String) {
        //    val cookies = Cookie.parseAll(url, Headers.headersOf("Set-Cookie", rawCookie))
        //    cookieStore[url.host] = cookies
        //}
    }
}
