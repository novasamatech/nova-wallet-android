package io.novafoundation.nova.feature_wallet_impl.domain.fee

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.feature_account_api.data.fee.toFeePaymentCurrency
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.domain.fee.FeeInteractor
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeInspector
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class RealFeeInteractor(
    private val feePaymentProviderRegistry: FeePaymentProviderRegistry,
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val customFeeCapabilityFacade: CustomFeeCapabilityFacade,
) : FeeInteractor {

    override suspend fun canPayFeeInNonUtilityAsset(chainAsset: Chain.Asset, coroutineScope: CoroutineScope): Boolean {
        val feePaymentCurrency = chainAsset.toFeePaymentCurrency()

        val feePayment = feePaymentProviderRegistry.providerFor(chainAsset.chainId)
            .feePaymentFor(feePaymentCurrency, coroutineScope)

        return customFeeCapabilityFacade.canPayFeeInNonUtilityToken(chainAsset, feePayment)
    }

    override suspend fun assetFlow(asset: Chain.Asset): Flow<Asset> {
        val selectedMetaAccount = accountRepository.getSelectedMetaAccount()
        return walletRepository.assetFlow(selectedMetaAccount.id, asset)
    }

    override suspend fun hasEnoughBalanceToPayFee(feeAsset: Asset, inspectedFeeAmount: FeeInspector.InspectedFeeAmount): Boolean {
        val feeChainAsset = feeAsset.token.configuration
        val chain = chainRegistry.getChain(feeChainAsset.chainId)

        val existentialBalance = assetSourceRegistry.existentialDepositInPlanks(chain, feeChainAsset)
        val passEdFeeCheck = feeAsset.balanceCountedTowardsEDInPlanks - inspectedFeeAmount.checkedAgainstMinimumBalance >= existentialBalance

        val hasEnoughTransferable = feeAsset.transferableInPlanks >= inspectedFeeAmount.deductedFromTransferable

        return passEdFeeCheck && hasEnoughTransferable
    }

    override suspend fun getToken(chainAsset: Chain.Asset): Token {
        return tokenRepository.getToken(chainAsset)
    }
}
