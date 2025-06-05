package io.novafoundation.nova.feature_proxy_impl.data.common

import io.novafoundation.nova.feature_proxy_api.data.common.DepositBaseAndFactor
import io.novafoundation.nova.feature_proxy_api.data.common.ProxyDepositCalculator
import io.novafoundation.nova.feature_proxy_api.data.repository.ProxyConstantsRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

class RealProxyDepositCalculator(
    private val proxyConstantsRepository: ProxyConstantsRepository
) : ProxyDepositCalculator {

    override fun calculateProxyDepositForQuantity(baseAndFactor: DepositBaseAndFactor, proxiesCount: Int): BigInteger {
        return if (proxiesCount == 0) {
            BigInteger.ZERO
        } else {
            baseAndFactor.baseAmount + baseAndFactor.factorAmount * proxiesCount.toBigInteger()
        }
    }

    override suspend fun calculateProxyDepositForQuantity(chainId: ChainId, proxiesCount: Int): BigInteger {
        val depositAndFactor = proxyConstantsRepository.getDepositConstants(chainId)

        return calculateProxyDepositForQuantity(depositAndFactor, proxiesCount)
    }
}
