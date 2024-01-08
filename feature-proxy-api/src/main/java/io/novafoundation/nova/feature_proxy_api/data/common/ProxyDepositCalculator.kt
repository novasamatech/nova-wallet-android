package io.novafoundation.nova.feature_proxy_api.data.common

import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.proxy
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import java.math.BigInteger

class DepositBaseAndFactor(
    val baseAmount: BigInteger,
    val factorAmount: BigInteger
)

interface ProxyDepositCalculator {

    suspend fun getDepositConstants(chain: Chain): DepositBaseAndFactor

    fun calculateProxyDeposit(baseAndFactor: DepositBaseAndFactor, proxiesCount: Int): BigInteger
}
