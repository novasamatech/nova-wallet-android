package io.novafoundation.nova.feature_staking_impl.data.notices.repository

import android.content.SharedPreferences
import com.google.gson.Gson
import io.novafoundation.nova.feature_staking_impl.data.notices.cache.StakingNoticesDiskCache
import io.novafoundation.nova.feature_staking_impl.data.notices.model.StakingNotice
import io.novafoundation.nova.feature_staking_impl.data.notices.model.StakingNoticeDto
import io.novafoundation.nova.feature_staking_impl.data.notices.network.StakingNoticesApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

class RealStakingNoticesRepository(
    private val api: StakingNoticesApi,
    private val diskCache: StakingNoticesDiskCache,
    private val gson: Gson,
    private val sharedPreferences: SharedPreferences,
    private val sharedPreferencesLanguageKey: String,
    private val remoteUrl: String,
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
) : StakingNoticesRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val flow = MutableSharedFlow<Map<ChainId, StakingNotice>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @Volatile
    private var lastSuccessfulSyncAt: Long = 0L

    private val syncMutex = Mutex()

    private val languageChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == sharedPreferencesLanguageKey) {
            scope.launch { decodeFromDiskAndEmit() }
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(languageChangeListener)
        scope.launch { decodeFromDiskAndEmit() }
    }

    override fun observeStakingNotices(): Flow<Map<ChainId, StakingNotice>> = flow

    override suspend fun syncStakingNotices() {
        syncMutex.withLock {
            if (timeProvider() - lastSuccessfulSyncAt < DEBOUNCE_MILLIS) return
            val rawJson = runCatching { api.fetchStakingNotices(remoteUrl) }.getOrNull() ?: return
            runCatching { diskCache.write(rawJson) }   // best effort
            val map = decode(rawJson) ?: return
            flow.emit(map)
            lastSuccessfulSyncAt = timeProvider()
        }
    }

    private suspend fun decodeFromDiskAndEmit() {
        val raw = diskCache.read() ?: run { flow.emit(emptyMap()); return }
        val map = decode(raw) ?: return     // bad cache - keep prior emission
        flow.emit(map)
    }

    private fun decode(rawJson: String): Map<ChainId, StakingNotice>? {
        return runCatching {
            val dtos = gson.fromJson(rawJson, Array<StakingNoticeDto>::class.java) ?: emptyArray()
            val locale = Locale.getDefault().toLanguageTag()
            dtos.mapNotNull { it.toDomain(locale) }.associateBy { it.chainId }
        }.getOrNull()
    }

    companion object {
        private const val DEBOUNCE_MILLIS = 5 * 60 * 1000L
    }
}
