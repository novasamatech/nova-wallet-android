package io.novafoundation.nova.feature_proxy_api.data.common

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class DepositBaseAndFactor(
    val baseAmount: BigInteger,
    val factorAmount: BigInteger
)

interface ProxyDepositCalculator {

    suspend fun getDepositConstants(chain: Chain): DepositBaseAndFactor

    fun calculateProxyDepositForQuantity(baseAndFactor: DepositBaseAndFactor, proxiesCount: Int): BigInteger
}
