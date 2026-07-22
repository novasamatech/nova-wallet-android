package io.novafoundation.nova.common.data.analytics

import android.util.Log
import io.novafoundation.nova.common.BuildConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

class PostHogAnalyticsService(
    private val okHttpClient: OkHttpClient,
    private val apiKey: String,
    private val host: String
) : AnalyticsService {

    companion object {
        private const val TAG = "PostHogAnalytics"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    @Volatile
    private var sessionId: String = UUID.randomUUID().toString()

    @Volatile
    override var isEnabled: Boolean = false
        set(value) {
            field = value
            if (!value) {
                // Rotate session ID on opt-out to prevent cross-session linking
                sessionId = UUID.randomUUID().toString()
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, if (value) "Analytics enabled" else "Analytics disabled and reset")
            }
        }

    fun initialize() {
        sessionId = UUID.randomUUID().toString()

        if (apiKey.isEmpty()) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Analytics API key not configured")
            }
            return
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "PostHog analytics initialized (enabled: $isEnabled)")
        }
    }

    override fun track(event: AnalyticsEvent) {
        if (!isEnabled) return
        if (apiKey.isEmpty()) return

        val props = event.properties.toMutableMap()
        props["session_id"] = sessionId

        // Suppress PII: prevent person profile creation and geo lookup
        props["\$process_person_profile"] = false
        props["\$geoip_disable"] = true
        props["\$ip"] = false
        // Suppress device fingerprinting properties
        props["\$device_id"] = ""
        props["\$device_type"] = ""
        props["\$os"] = ""
        props["\$os_version"] = ""
        props["\$screen_height"] = ""
        props["\$screen_width"] = ""
        props["\$browser"] = ""
        props["\$browser_version"] = ""
        props["\$locale"] = ""
        props["\$timezone"] = ""
        props["\$app_version"] = ""
        props["\$lib"] = ""
        props["\$lib_version"] = ""

        val body = JSONObject().apply {
            put("api_key", apiKey)
            put("event", event.name)
            put("distinct_id", sessionId)
            put("properties", JSONObject(props as Map<*, *>))
            put("ip", "")
        }

        val request = Request.Builder()
            .url("$host/capture/")
            .header("X-Forwarded-For", "0.0.0.0")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        // Fire and forget with async call
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Failed to send event: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Event delivered: ${event.name} (status: ${response.code})")
                }
            }
        })

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Event queued: ${event.name} properties: $props")
        }
    }
}
