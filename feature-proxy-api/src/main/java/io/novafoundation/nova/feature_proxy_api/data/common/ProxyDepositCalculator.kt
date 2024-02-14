package io.novafoundation.nova.feature_proxy_api.data.common

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

interface ProxyDepositCalculator {

    fun calculateProxyDepositForQuantity(baseAndFactor: DepositBaseAndFactor, proxiesCount: Int): BigInteger

    suspend fun calculateProxyDepositForQuantity(chainId: ChainId, proxiesCount: Int): BigInteger
}
