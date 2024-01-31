package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.rpc

import io.novafoundation.nova.common.utils.asGsonParsedNumber
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import jp.co.soramitsu.fearless_utils.wsrpc.executeAsync
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.RuntimeRequest

interface DockRpc {

    suspend fun stakingRewardYearlyEmission(chainId: ChainId, totalStaked: Balance, totalIssuance: Balance): Balance
}

class RealDockRpc(
    private val chainRegistry: ChainRegistry,
): DockRpc {

    override suspend fun stakingRewardYearlyEmission(chainId: ChainId, totalStaked: Balance, totalIssuance: Balance): Balance {
        val request = YearlyEmissionRequest(totalStaked, totalIssuance)

        val response = chainRegistry.getSocket(chainId).executeAsync(request)

        return response.result.asGsonParsedNumber()
    }
}

private class YearlyEmissionRequest(
    totalStaked: Balance,
    totalIssuance: Balance
): RuntimeRequest(
    method = "staking_rewards_yearlyEmission",
    params = listOf(totalStaked, totalIssuance)
)
