package com.djangofiles.djangofiles

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.webkit.CookieManager
import androidx.core.content.edit
import com.djangofiles.djangofiles.db.ServerDao
import com.djangofiles.djangofiles.db.ServerDatabase
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Headers
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
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.InputStream
import java.net.URLConnection

//import android.os.Parcelable
//import kotlinx.parcelize.Parcelize

class ServerApi(val context: Context, host: String) {
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
                addCookie(token)
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

    private fun addCookie(token: String) {
        Log.d("Api[addCookie]", "token: $token")
        val cookies = cookieJar.loadForRequest(hostname.toHttpUrl())
        val cookieManager = CookieManager.getInstance()
        for (cookie in cookies) {
            //Log.d("Api[login]", "setCookie: $cookie")
            //cookieManager.setCookie(host, cookie.toString())
            if (cookie.name == "sessionid") {
                Log.i("Api[addCookie]", "ADDING: ${cookie.name}")
                cookieManager.setCookie(hostname, cookie.toString()) {
                    Log.i("Api[addCookie]", "cookieManager.flush")
                    cookieManager.flush()
                }
            }
        }
    }

    private suspend fun reAuthenticate(): String? {
        return try {
            val cookies = CookieManager.getInstance().getCookie(hostname)
            Log.d("reAuthenticate", "cookies: $cookies")
            val httpUrl = hostname.toHttpUrl()
            cookieJar.setCookie(httpUrl, cookies)

            val tokenResponse = api.getToken()
            Log.d("reAuthenticate", "tokenResponse: $tokenResponse")

            preferences.edit { putString("auth_token", tokenResponse.token) }
            Log.d("reAuthenticate", "auth_token: ${tokenResponse.token}")
            val dao: ServerDao = ServerDatabase.getInstance(context).serverDao()
            withContext(Dispatchers.IO) {
                dao.setToken(hostname, tokenResponse.token)
            }
            tokenResponse.token
        } catch (e: Exception) {
            Log.e("reAuthenticate", "Exception: ${e.message}")
            null
        }
    }

    suspend fun authorize(signature: String): String? {
        Log.d("Api[authorize]", "signature: $signature")
        // TODO: Make a function from the code in login to handle this response...
        val tokenResponse = api.authorize(signature)
        Log.d("Api[authorize]", "tokenResponse: $tokenResponse")
        if (tokenResponse.isSuccessful) {
            val tokenData = tokenResponse.body()
            Log.d("Api[authorize]", "tokenData: $tokenData")
            if (tokenData != null) {
                Log.d("Api[authorize]", "token: ${tokenData.token}")
                addCookie(tokenData.token)
                return tokenData.token
            }
        }
        return null
    }

    suspend fun methods(): Response<MethodsResponse> {
        Log.d("Api[methods]", "getMethods")
        return api.getMethods()
    }

    suspend fun upload(
        fileName: String,
        inputStream: InputStream,
        editRequest: FileEditRequest,
    ): Response<UploadResponse> {
        Log.d("Api[upload]", "fileName: $fileName - $editRequest")
        val multiPart: MultipartBody.Part = inputStreamToMultipart(inputStream, fileName)
        var response = api.postUpload(
            authToken, multiPart,
            password = editRequest.password,
            private = editRequest.private,
            albums = editRequest.albums,
        )
        // TODO: Determine how to make this block a reusable function...
        Log.d("Api[upload]", "response.code: ${response.code()}")
        if (response.code() == 401) {
            val token = reAuthenticate()
            Log.d("Api[upload]", "token: $token")
            if (token != null) {
                response = api.postUpload(
                    token, multiPart,
                    password = editRequest.password,
                    private = editRequest.private,
                    albums = editRequest.albums,
                )
            }
        }
        return response
    }

    suspend fun edit(fileId: Int, data: FileEditRequest): Response<ResponseBody> {
        Log.d("Api[edit]", "fileId: $fileId")
        Log.d("Api[edit]", "data: $data")
        return api.fileEdit(authToken, fileId, data)
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

    suspend fun recent(amount: Int, start: Int = 0): Response<List<FileResponse>> {
        Log.d("Api[recent]", "amount: $amount - start: $start")
        return api.getRecent(authToken, amount, start)
    }

    suspend fun albums(): Response<AlbumResponse> {
        Log.d("Api[albums]", "albums")
        val response = api.getAlbums(authToken)
        return response
    }

    suspend fun current(): Response<StatsResponse> {
        Log.d("Api[current]", "current")
        val response = api.getStatsCurrent(authToken)
        return response
    }

    suspend fun deleteFile(fileId: Int): Response<ResponseBody> {
        Log.d("Api[deleteFile]", "fileId: $fileId")
        return api.fileDelete(authToken, fileId)
    }

    suspend fun filesEdit(data: FilesEditRequest): Response<ResponseBody> {
        Log.d("Api[filesEdit]", "data: $data")
        return api.filesEdit(authToken, data)
    }

    suspend fun filesDelete(data: FilesEditRequest): Response<ResponseBody> {
        Log.d("Api[filesDelete]", "data: $data")
        return api.filesDelete(authToken, data)
    }

    interface ApiService {
        @FormUrlEncoded
        @POST("oauth/")
        suspend fun login(
            @Field("username") username: String,
            @Field("password") password: String
        ): Response<ResponseBody>

        @FormUrlEncoded
        @POST("auth/application/")
        suspend fun authorize(
            @Field("signature") signature: String,
        ): Response<TokenResponse>

        @GET("auth/methods/")
        suspend fun getMethods(): Response<MethodsResponse>

        @POST("auth/token/")
        suspend fun getToken(): TokenResponse

        @Multipart
        @POST("upload/")
        suspend fun postUpload(
            @Header("Authorization") token: String,
            @Part file: MultipartBody.Part,
            @Header("Format") format: String? = null,
            @Header("Expires-At") expiresAt: String? = null,
            @Header("Strip-GPS") stripGps: Boolean? = null,
            @Header("Strip-EXIF") stripExif: Boolean? = null,
            @Header("Private") private: Boolean? = null,
            @Header("Password") password: String? = null,
            @Header("Albums") albums: List<Int>? = null,
        ): Response<UploadResponse>

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
            @Query("start") start: Int? = null,
            @Query("before") before: Int? = null,
            @Query("after") after: Int? = null,
        ): Response<List<FileResponse>>

        @GET("albums/")
        suspend fun getAlbums(
            @Header("Authorization") token: String,
        ): Response<AlbumResponse>

        @GET("stats/current/")
        suspend fun getStatsCurrent(
            @Header("Authorization") token: String,
        ): Response<StatsResponse>

        @POST("file/{id}")
        suspend fun fileEdit(
            @Header("Authorization") token: String,
            @Path("id") fileId: Int,
            @Body data: FileEditRequest
        ): Response<ResponseBody>

        @DELETE("file/{id}")
        suspend fun fileDelete(
            @Header("Authorization") token: String,
            @Path("id") fileId: Int,
        ): Response<ResponseBody>

        @POST("files/edit/")
        suspend fun filesEdit(
            @Header("Authorization") token: String,
            @Body data: FilesEditRequest
        ): Response<ResponseBody>

        //@DELETE("files/")
        //@HTTP(method = "DELETE", path = "files/", hasBody = true)
        @POST("files/delete/")
        suspend fun filesDelete(
            @Header("Authorization") token: String,
            @Body data: FilesEditRequest
        ): Response<ResponseBody>

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

    data class UploadResponse(
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

    data class StatsResponse(
        val size: Long,
        val count: Int,
        val shorts: Int,
        @SerializedName("human_size") val humanSize: String
    )


    data class VersionRequest(val version: String)
    data class VersionResponse(
        val version: String,
        val valid: Boolean,
    )

    data class FileEditRequest(
        @SerializedName("id") val id: Int? = null,
        @SerializedName("name") var name: String? = null,
        @SerializedName("info") var info: String? = null,
        @SerializedName("expr") var expr: String? = null,
        @SerializedName("maxv") var maxv: Int? = null,
        @SerializedName("meta_preview") var metaPreview: Boolean? = null,
        @SerializedName("password") var password: String? = null,
        @SerializedName("private") var private: Boolean? = null,
        @SerializedName("albums") var albums: List<Int>? = null,
    )

    data class FilesEditRequest(
        @SerializedName("ids") val ids: List<Int>,
        @SerializedName("name") val name: String? = null,
        @SerializedName("info") val info: String? = null,
        @SerializedName("expr") val expr: String? = null,
        @SerializedName("maxv") val maxv: Int? = null,
        @SerializedName("meta_preview") val metaPreview: Boolean? = null,
        @SerializedName("password") val password: String? = null,
        @SerializedName("private") val private: Boolean? = null,
        @SerializedName("albums") val albums: List<Int>? = null,
    )

    data class FileResponse(
        val id: Int,
        val user: Int,
        val size: Int,
        val mime: String,
        var name: String,
        var info: String,
        var expr: String,
        val view: Int,
        var maxv: Int,
        @SerializedName("meta_preview") var metaPreview: Boolean,
        var password: String,
        @SerializedName("private") var private: Boolean,
        val avatar: Boolean,
        val url: String,
        val thumb: String,
        val raw: String,
        val date: String,
        var albums: List<Int>,
    )

    data class AlbumResponse(
        val albums: List<AlbumData>,
        val next: Int,
        val count: Int,
    )

    data class AlbumData(
        @SerializedName("id") val id: Int,
        @SerializedName("user") val user: Int,
        @SerializedName("name") val name: String,
        @SerializedName("password") val password: String,
        @SerializedName("private") val private: Boolean,
        @SerializedName("info") val info: String,
        @SerializedName("view") val view: Int,
        @SerializedName("maxv") val maxv: Int,
        @SerializedName("expr") val expr: String,
        @SerializedName("date") val date: String,
        @SerializedName("url") val url: String,
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

    private fun createRetrofit(): Retrofit {
        val baseUrl = "${hostname}/api/"
        Log.d("createRetrofit", "baseUrl: $baseUrl")

        cookieJar = SimpleCookieJar()
        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", context.getUserAgent())
                    .build()
                chain.proceed(request)
            }
            .build()

        val gson = GsonBuilder().create()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
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

fun Context.getUserAgent(): String {
    val versionName = this.packageManager.getPackageInfo(this.packageName, 0).versionName
    Log.d("getUserAgent", "versionName: $versionName")
    val userAgent = "DjangoFiles Android/${versionName}"
    Log.d("getUserAgent", "userAgent: $userAgent")
    return userAgent
}
