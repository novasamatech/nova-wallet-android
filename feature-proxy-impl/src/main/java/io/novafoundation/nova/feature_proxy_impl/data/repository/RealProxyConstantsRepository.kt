package io.novafoundation.nova.feature_proxy_impl.data.repository

import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.proxy
import io.novafoundation.nova.feature_proxy_api.data.common.DepositBaseAndFactor
import io.novafoundation.nova.feature_proxy_api.data.repository.ProxyConstantsRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime

class RealProxyConstantsRepository(
    private val chainRegestry: ChainRegistry
) : ProxyConstantsRepository {

    override suspend fun getDepositConstants(chainId: ChainId): DepositBaseAndFactor {
        val runtime = chainRegestry.getRuntime(chainId)
        val constantQuery = runtime.metadata.proxy()
        return DepositBaseAndFactor(
            baseAmount = constantQuery.numberConstant("ProxyDepositBase", runtime),
            factorAmount = constantQuery.numberConstant("ProxyDepositFactor", runtime)
        )
    }
}
