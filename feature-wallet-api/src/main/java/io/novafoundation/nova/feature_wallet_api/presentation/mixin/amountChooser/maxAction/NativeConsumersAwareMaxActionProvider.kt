package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDeposit
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.hash.isPositive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class NativeConsumersAwareMaxActionProvider(
    assetSourceRegistry: AssetSourceRegistry,
    chainRegistry: ChainRegistry,
    accountInfoFlow: Flow<AccountInfo>,
    inner: MaxActionProvider
) : MaxActionProvider {

    // Fee is not deducted for display
    override val maxAvailableForDisplay: Flow<Balance?> = inner.maxAvailableForDisplay

    override val maxAvailableForAction: Flow<MaxActionProvider.MaxAvailableForAction?> = combine(
        inner.maxAvailableForAction,
        accountInfoFlow
    ) { maxAvailable, accountInfo ->
        if (maxAvailable == null) return@combine null

        // Asset out is non sufficient
        if (maxAvailable.chainAsset.isCommissionAsset && accountInfo.consumers.isPositive()) {
            val chain = chainRegistry.getChain(maxAvailable.chainAsset.chainId)
            val existentialDeposit = assetSourceRegistry.existentialDepositInPlanks(chain, maxAvailable.chainAsset)
            maxAvailable - existentialDeposit
        } else {
            maxAvailable
        }
    }
}
