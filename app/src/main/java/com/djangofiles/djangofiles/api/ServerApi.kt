package com.djangofiles.djangofiles.api

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.InputStream
import java.net.URLConnection


class ServerApi(context: Context, host: String) {
    val api: ApiService
    val authToken: String
    val preferences: SharedPreferences = context.getSharedPreferences("AppPreferences", MODE_PRIVATE)

    init {
        api = createRetrofit(host).create(ApiService::class.java)
        authToken = preferences.getString("auth_token", null) ?: ""
    }

    private lateinit var cookieJar: SimpleCookieJar
    private lateinit var client: OkHttpClient

    suspend fun upload(fileName: String, inputStream: InputStream): Response<FileResponse> {
        Log.d("upload", "fileName: $fileName")
        val multiPart: MultipartBody.Part = inputStreamToMultipart(inputStream, fileName)
        return api.postUpload(authToken, multiPart)
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
    }

    data class FileResponse(
        val url: String,
        val raw: String,
        val name: String,
        val size: Long
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

        fun setCookie(url: HttpUrl, rawCookie: String) {
            val cookies = Cookie.parseAll(url, Headers.headersOf("Set-Cookie", rawCookie))
            cookieStore[url.host] = cookies
        }
    }
}
