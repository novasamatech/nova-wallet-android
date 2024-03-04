package io.novafoundation.nova.feature_proxy_api.data.repository

import io.novafoundation.nova.feature_proxy_api.data.common.DepositBaseAndFactor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface ProxyConstantsRepository {

    suspend fun getDepositConstants(chainId: ChainId): DepositBaseAndFactor
}
