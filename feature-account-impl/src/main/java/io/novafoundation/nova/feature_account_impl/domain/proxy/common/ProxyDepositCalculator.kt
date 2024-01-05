package io.novafoundation.nova.feature_account_impl.domain.proxy.common

import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.proxy
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class DepositBaseAndFactor(
    val baseAmount: Balance,
    val factorAmount: Balance
)

class ProxyDepositCalculator(
    private val chainRegestry: ChainRegistry
) {

    suspend fun getDepositConstants(chain: Chain): DepositBaseAndFactor {
        val runtime = chainRegestry.getRuntime(chain.id)
        val constantQuery = runtime.metadata.proxy()
        return DepositBaseAndFactor(
            baseAmount = constantQuery.numberConstant("ProxyDepositBase", runtime),
            factorAmount = constantQuery.numberConstant("ProxyDepositFactor", runtime)
        )
    }

    fun calculateProxyDeposit(baseAndFactor: DepositBaseAndFactor, proxiesCount: Int): Balance {
        return if (proxiesCount == 0) {
            Balance.ZERO
        } else {
            baseAndFactor.baseAmount + baseAndFactor.factorAmount * proxiesCount.toBigInteger()
        }
    }
}
