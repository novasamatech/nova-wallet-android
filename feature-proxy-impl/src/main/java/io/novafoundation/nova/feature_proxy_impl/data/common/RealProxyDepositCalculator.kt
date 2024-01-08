package io.novafoundation.nova.feature_proxy_impl.data.common

import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.proxy
import io.novafoundation.nova.feature_proxy_api.data.common.DepositBaseAndFactor
import io.novafoundation.nova.feature_proxy_api.data.common.ProxyDepositCalculator
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import java.math.BigInteger

class RealProxyDepositCalculator(
    private val chainRegestry: ChainRegistry
) : ProxyDepositCalculator {

    override suspend fun getDepositConstants(chain: Chain): DepositBaseAndFactor {
        val runtime = chainRegestry.getRuntime(chain.id)
        val constantQuery = runtime.metadata.proxy()
        return DepositBaseAndFactor(
            baseAmount = constantQuery.numberConstant("ProxyDepositBase", runtime),
            factorAmount = constantQuery.numberConstant("ProxyDepositFactor", runtime)
        )
    }

    override fun calculateProxyDeposit(baseAndFactor: DepositBaseAndFactor, proxiesCount: Int): BigInteger {
        return if (proxiesCount == 0) {
            BigInteger.ZERO
        } else {
            baseAndFactor.baseAmount + baseAndFactor.factorAmount * proxiesCount.toBigInteger()
        }
    }
}
