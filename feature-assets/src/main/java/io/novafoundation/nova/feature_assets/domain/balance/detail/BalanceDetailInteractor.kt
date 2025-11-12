package io.novafoundation.nova.feature_assets.domain.balance.detail

import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency.Asset.Companion.toFeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.isSelfSufficientAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ScreenScope
class BalanceDetailInteractor @Inject constructor(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val feePaymentFacade: CustomFeeCapabilityFacade,
) {

    suspend fun isGiftingEnabled(chainAsset: Chain.Asset): Boolean {
        return withContext(Dispatchers.Default) {
            val canPayFee = feePaymentFacade.canPayFeeInCurrency(chainAsset.toFeePaymentCurrency())
            val isSelfSufficient = assetSourceRegistry.isSelfSufficientAsset(chainAsset)

            canPayFee && isSelfSufficient
        }
    }
}
