package io.novafoundation.nova.runtime.storage

import com.google.gson.Gson
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.updaters.SampledBlockTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

interface SampledBlockTimeStorage {

    suspend fun get(chainId: ChainId): SampledBlockTime

    fun observe(chainId: ChainId): Flow<SampledBlockTime>

    suspend fun put(chainId: ChainId, sampledBlockTime: SampledBlockTime)
}

private const val KEY = "SampledBlockTime"

internal class PrefsSampledBlockTimeStorage(
    private val gson: Gson,
    private val sharedPreferences: Preferences,
) : SampledBlockTimeStorage {

    override suspend fun get(chainId: ChainId): SampledBlockTime {
        val raw = sharedPreferences.getString(key(chainId)) ?: return initial()

        return gson.fromJson(raw)
    }

    override fun observe(chainId: ChainId): Flow<SampledBlockTime> {
        return sharedPreferences.stringFlow(key(chainId)).map { raw ->
            raw?.let(gson::fromJson) ?: initial()
        }
    }

    override suspend fun put(chainId: ChainId, sampledBlockTime: SampledBlockTime) {
        val raw = gson.toJson(sampledBlockTime)

        sharedPreferences.putString(key(chainId), raw)
    }

    private fun key(chainId: ChainId) = "$KEY::$chainId"

    private fun initial() = SampledBlockTime(sampleSize = BigInteger.ZERO, averageBlockTime = BigInteger.ZERO)
}
