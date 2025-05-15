package com.djangofiles.djangofiles

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

class DiscordApi(
    val context: Context,
    webhookUrl: String = "",
) {
    val api: ApiService
    val webhook: String = webhookUrl

    private lateinit var cookieJar: SimpleCookieJar
    private lateinit var client: OkHttpClient

    init {
        api = createRetrofit().create(ApiService::class.java)
    }

    suspend fun sendMessage(messageText: String): Response<Unit> {
        val message = Message(content = messageText)
        return api.postWebhook(webhook, message)
    }

    data class Message(
        val content: String
    )

    interface ApiService {
        @POST
        suspend fun postWebhook(
            @Url url: String,
            @Body message: Message
        ): Response<Unit>
    }

    private fun createRetrofit(): Retrofit {
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        Log.d("createRetrofit", "versionName: $versionName")
        val userAgent = "DjangoFiles Android/${versionName}"
        Log.d("createRetrofit", "userAgent: $userAgent")

        cookieJar = SimpleCookieJar()
        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .build()
                chain.proceed(request)
            }
            .build()

        val gson = GsonBuilder().create()
        return Retrofit.Builder()
            .baseUrl("https://discord.com/api/")
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

        //fun setCookie(url: HttpUrl, rawCookie: String) {
        //    val cookies = Cookie.parseAll(url, Headers.headersOf("Set-Cookie", rawCookie))
        //    cookieStore[url.host] = cookies
        //}
    }
}
