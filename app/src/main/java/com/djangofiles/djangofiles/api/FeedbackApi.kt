package com.djangofiles.djangofiles.api

import android.content.Context
import android.os.Build
import android.util.Log
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.getUserAgent
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

const val HOOK_ID = "1376004891192856586"
const val RELAY_URL = "https://relay.cssnr.com/"

class FeedbackApi(val context: Context) {

    val api: ApiService

    init {
        api = createRetrofit().create(ApiService::class.java)
    }

    suspend fun sendFeedback(messageText: String): Response<Unit> {
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        Log.d("sendFeedback", "messageText: $messageText")
        val feedbackText = context.getString(
            R.string.feedback_message,
            context.getString(R.string.app_name),
            versionName,
            Build.VERSION.SDK_INT.toString(),
            messageText
        )
        Log.d("sendFeedback", "feedbackText: $feedbackText")
        val message = Message(content = feedbackText)
        Log.d("sendFeedback", "message: $message")
        return try {
            api.postDiscord(message)
        } catch (e: Exception) {
            val errorBody = e.toString().toResponseBody("text/plain".toMediaTypeOrNull())
            Response.error(520, errorBody)
        }
    }

    @JsonClass(generateAdapter = true)
    data class Message(
        @Json(name = "content")
        val content: String
    )

    interface ApiService {
        @POST(("discord/${HOOK_ID}"))
        suspend fun postDiscord(
            @Body message: Message
        ): Response<Unit>
    }

    private fun createRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", context.getUserAgent())
                    .build()
                chain.proceed(request)
            }
            .build()
        val moshi = Moshi.Builder().build()
        return Retrofit.Builder()
            .baseUrl(RELAY_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
    }
}
