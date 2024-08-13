package io.novafoundation.nova.feature_wallet_impl.domain.fee

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.toFeePaymentCurrency
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.fee.CustomFeeInteractor
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class RealCustomFeeInteractor(
    private val feePaymentProviderRegistry: FeePaymentProviderRegistry,
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val assetSourceRegistry: AssetSourceRegistry
) : CustomFeeInteractor {

    override suspend fun availableCommissionAssetFor(chainAsset: Chain.Asset, coroutineScope: CoroutineScope): List<Chain.Asset> {
        val chain = chainRegistry.getChain(chainAsset.chainId)
        val feePaymentCurrency = chainAsset.toFeePaymentCurrency()
        return feePaymentProviderRegistry.providerFor(chain)
            .feePaymentFor(feePaymentCurrency, coroutineScope)
            .availableCustomFeeAssets()
    }

    override suspend fun assetFlow(asset: Chain.Asset): Flow<Asset> {
        val selectedMetaAccount = accountRepository.getSelectedMetaAccount()
        return walletRepository.assetFlow(selectedMetaAccount.id, asset)
    }

    override suspend fun hasEnoughBalanceToPayFee(commissionAsset: Asset, feeAmount: BigInteger): Boolean {
        val assetSource = assetSourceRegistry.sourceFor(commissionAsset.token.configuration)
        val assetBalance = assetSource.balance

        val chain = chainRegistry.getChain(commissionAsset.token.configuration.chainId)
        val existentialDeposit = assetBalance.existentialDeposit(chain, commissionAsset.token.configuration)

        return commissionAsset.transferableInPlanks - feeAmount >= existentialDeposit
    }
}
