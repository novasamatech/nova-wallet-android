package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.asPerQuintill
import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.executeAsync
import io.novasama.substrate_sdk_android.wsrpc.mappers.nonNull
import io.novasama.substrate_sdk_android.wsrpc.mappers.pojo
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest
import java.math.BigInteger

interface VaraRepository {

    suspend fun getVaraInflation(chainId: ChainId): Perbill
}

class RealVaraRepository(
    private val chainRegistry: ChainRegistry
) : VaraRepository {

    override suspend fun getVaraInflation(chainId: ChainId): Perbill {
        return chainRegistry.getSocket(chainId).inflationInfo().inflation.asPerQuintill()
    }

    private suspend fun SocketService.inflationInfo(): InflationInfo {
        return executeAsync(InflationInfoRequest(), mapper = pojo<InflationInfo>().nonNull())
    }

    private class InflationInfoRequest : RuntimeRequest(
        method = "stakingRewards_inflationInfo",
        params = emptyList()
    )

    private class InflationInfo(val inflation: BigInteger)
}
