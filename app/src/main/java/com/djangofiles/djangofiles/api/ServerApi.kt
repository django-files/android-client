package com.djangofiles.djangofiles.api

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.net.Uri
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
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.InputStream
import java.net.URLConnection


class ServerApi(private val context: Context, host: String) {
    val api: ApiService

    init {
        api = createRetrofit(host).create(ApiService::class.java)
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences("AppPreferences", MODE_PRIVATE)

    private lateinit var cookieJar: SimpleCookieJar
    private lateinit var client: OkHttpClient

    suspend fun upload(uri: Uri, fileName: String): FileResponse? {
        Log.d("upload", "uri: $uri")
        Log.d("upload", "fileName: $fileName")
        val authToken = preferences.getString("auth_token", null)
        Log.d("upload", "authToken: $authToken")
        val inputStream = context.contentResolver.openInputStream(uri)
        if (authToken == null || inputStream == null) {
            Log.e("upload", "inputStream/ziplineToken is null")
            return null
        }
        val multiPart: MultipartBody.Part = inputStreamToMultipart(inputStream, fileName)
        return try {
            val response: FileResponse = api.postUpload(authToken, multiPart)
            Log.e("upload", "response: $response")
            response
        } catch (e: HttpException) {
            Log.e("upload", "HttpException: ${e.message}")
            val response = e.response()?.errorBody()?.string()
            Log.d("upload", "e.response: $response")
            Log.d("upload", "e.code: ${e.code()}")
            if (e.code() == 401) {
                Log.w("upload", "AUTH FAILED")
            }
            null
        } catch (e: Exception) {
            Log.e("upload", "Exception: ${e.message}")
            null
        }
    }

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
        ): FileResponse
    }

    data class FileResponse(
        val url: String,
        val raw: String,
        val name: String,
        val size: Long
    )

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
