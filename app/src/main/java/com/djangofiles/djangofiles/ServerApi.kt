package com.djangofiles.djangofiles

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcelable
import android.util.Log
import android.webkit.CookieManager
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import java.io.InputStream
import java.net.URLConnection

// TODO: Pass preferences instead of context since context is not used
class ServerApi(context: Context, host: String) {
    val api: ApiService
    val hostname: String = host
    val authToken: String
    val preferences: SharedPreferences =
        context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

    private lateinit var cookieJar: SimpleCookieJar
    private lateinit var client: OkHttpClient

    init {
        api = createRetrofit().create(ApiService::class.java)
        authToken = preferences.getString("auth_token", null) ?: ""
        Log.d("ServerApi", "authToken: $authToken")
        Log.d("ServerApi", "hostname: $hostname")
    }

    suspend fun login(user: String, pass: String): String? {
        Log.d("Api[login]", "user/pass: ${user}/${pass}")

        return try {
            val loginResponse = api.login(user, pass)
            Log.d("Api[login]", "loginResponse: $loginResponse")
            if (loginResponse.isSuccessful) {
                val token = api.getToken().token
                Log.d("Api[login]", "token: $token")
                val cookies = cookieJar.loadForRequest(hostname.toHttpUrl())
                val cookieManager = CookieManager.getInstance()
                for (cookie in cookies) {
                    //Log.d("Api[login]", "setCookie: $cookie")
                    //cookieManager.setCookie(host, cookie.toString())
                    if (cookie.name == "sessionid") {
                        Log.i("Api[login]", "ADDING: ${cookie.name}")
                        cookieManager.setCookie(hostname, cookie.toString()) {
                            Log.i("Api[login]", "cookieManager.flush")
                            cookieManager.flush()
                        }
                    }
                }
                Log.i("Api[login]", "SUCCESS")
                token
            } else {
                Log.e("Api[login]", "Error: ${loginResponse.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("Api[login]", "Exception: ${e.message}")
            null
        }
    }

    suspend fun methods(): MethodsResponse {
        Log.d("Api[methods]", "getMethods")
        return api.getMethods()
    }

    suspend fun upload(fileName: String, inputStream: InputStream): Response<FileResponse> {
        Log.d("Api[upload]", "fileName: $fileName")
        val multiPart: MultipartBody.Part = inputStreamToMultipart(inputStream, fileName)
        return api.postUpload(authToken, multiPart)
    }

    suspend fun shorten(url: String, vanity: String?): Response<ShortResponse> {
        Log.d("Api[shorten]", "url: $url")
        Log.d("Api[shorten]", "vanity: $vanity")
        return api.postShort(authToken, url, vanity)
    }

    // TODO: Use VersionResponse
    suspend fun version(version: String): Response<ResponseBody> {
        Log.d("Api[version]", "version: $version")
        return api.postVersion(VersionRequest(version))
    }

    suspend fun recent(amount: Int, start: Int = 0): Response<List<RecentResponse>> {
        Log.d("Api[recent]", "amount: $amount - start: $start")
        return api.getRecent(authToken, amount, start)
    }

    interface ApiService {
        @FormUrlEncoded
        @POST("oauth/")
        suspend fun login(
            @Field("username") username: String,
            @Field("password") password: String
        ): Response<ResponseBody>

        @GET("auth/methods/")
        suspend fun getMethods(): MethodsResponse

        @POST("auth/token/")
        suspend fun getToken(): TokenResponse

        @Multipart
        @POST("upload/")
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

        @POST("shorten/")
        suspend fun postShort(
            @Header("Authorization") token: String,
            @Header("URL") url: String,
            @Header("Vanity") vanity: String? = null,
            @Header("Max-Views") maxViews: Int? = null,
        ): Response<ShortResponse>

        @GET("recent/")
        suspend fun getRecent(
            @Header("Authorization") token: String,
            @Query("amount") amount: Int,
            @Query("start") start: Int,
        ): Response<List<RecentResponse>>

        // TODO: Use VersionResponse
        @POST("version/")
        suspend fun postVersion(
            @Body version: VersionRequest
        ): Response<ResponseBody>
    }

    data class TokenResponse(
        val token: String
    )

    data class MethodsResponse(
        val authMethods: List<Methods>,
        val siteName: String,
    )

    data class Methods(
        val name: String,
        val url: String,
    )

    data class FileResponse(
        val url: String,
        val raw: String,
        val name: String,
        val size: Long,
    )

    data class ShortResponse(
        val url: String,
        val vanity: String,
        @SerializedName("max-views") val maxViews: Int,
    )

    data class VersionRequest(val version: String)
    data class VersionResponse(
        val version: String,
        val valid: Boolean,
    )

    @Parcelize
    data class RecentResponse(
        val id: Int,
        val user: Int,
        val size: Int,
        val mime: String,
        val name: String,
        val info: String,
        val expr: String,
        val view: Int,
        val maxv: Int,
        @SerializedName("meta_preview") val metaPreview: Boolean,
        val password: String,
        val `private`: Boolean,
        val avatar: Boolean,
        val url: String,
        val thumb: String,
        val raw: String,
        val date: String,
    ) : Parcelable

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

    private fun createRetrofit(): Retrofit {
        val baseUrl = "${hostname}/api/"
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