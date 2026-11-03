package io.novafoundation.nova.common.data.config

import com.google.gson.Gson
import io.novafoundation.nova.common.BuildConfig
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.domain.config.GlobalConfig
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface GlobalConfigDataSource {

    suspend fun getGlobalConfig(): GlobalConfig
}

class RealGlobalConfigDataSource(
    private val api: GlobalConfigApi,
    private val preferences: Preferences,
    private val gson: Gson,
    private val logger: Logger
) : GlobalConfigDataSource {

    private var globalConfig: GlobalConfig? = null
    private val mutex = Mutex()

    override suspend fun getGlobalConfig(): GlobalConfig = mutex.withLock {
        globalConfig?.let { return@withLock it }

        val config = runCatching {
            val response = api.getGlobalConfig()
            val config = response.toDomain()
            preferences.putString(cacheKey(), gson.toJson(response))
            config
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            val saved = savedConfig() ?: throw error
            logger.log("[GlobalConfig] Using saved configuration after refresh failure")
            logger.log(error)
            saved
        }
        config.also { globalConfig = it }
    }

    private fun savedConfig(): GlobalConfig? = preferences.getString(cacheKey())?.let { raw ->
        runCatching { requireNotNull(gson.fromJson(raw, GlobalConfigRemote::class.java)).toDomain() }.getOrNull()
    }

    // Different build configurations must never inherit each other's infrastructure origin.
    private fun cacheKey() = "global_config_${BuildConfig.GLOBAL_CONFIG_URL}"

    private fun GlobalConfigRemote.toDomain() = GlobalConfig(
        multisigsApiUrl = multisigsApiUrl,
        proxyApiUrl = proxyApiUrl,
        multiStakingApiUrl = multiStakingApiUrl,
        infraUrl = infraUrl
    )
}
