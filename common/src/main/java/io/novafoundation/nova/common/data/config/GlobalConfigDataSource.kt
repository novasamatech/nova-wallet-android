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

    override suspend fun getGlobalConfig(): GlobalConfig {
        if (globalConfig != null) return globalConfig!!

        mutex.withLock {
            if (globalConfig != null) return globalConfig!!

            val remoteConfig = try {
                api.getGlobalConfig().also {
                    preferences.putString(cacheKey(), gson.toJson(it))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val saved = preferences.getString(cacheKey())?.let { raw ->
                    runCatching {
                        requireNotNull(gson.fromJson(raw, GlobalConfigRemote::class.java)).also { it.toDomain() }
                    }.getOrNull()
                } ?: throw e
                logger.log("[GlobalConfig] Using saved configuration after refresh failure")
                logger.log(e)
                saved
            }
            globalConfig = remoteConfig.toDomain()
        }

        return globalConfig!!
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
