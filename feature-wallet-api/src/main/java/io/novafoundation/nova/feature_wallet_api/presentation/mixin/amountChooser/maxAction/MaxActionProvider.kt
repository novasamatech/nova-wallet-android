package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import androidx.lifecycle.asFlow
import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProviderDsl.considerConsumers
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProviderDsl.deductFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProviderDsl.providingMaxOf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface MaxActionProvider {

    class MaxAvailableForAction(val balance: Balance, val chainAsset: Chain.Asset)

    val maxAvailableForDisplay: Flow<Balance?>

    val maxAvailableForAction: Flow<MaxAvailableForAction?>
}

object MaxActionProviderDsl {

    fun Flow<Asset?>.providingMaxOf(field: (Asset) -> Balance, allowMaxAction: Boolean = true): MaxActionProvider {
        return AssetMaxActionProvider(this, field, allowMaxAction)
    }

    fun <F : GenericFee> MaxActionProvider.deductFee(
        feeLoaderMixin: GenericFeeLoaderMixin<F>,
        extractTotalFee: (F) -> Balance
    ): MaxActionProvider {
        return FeeAwareMaxActionProvider(feeLoaderMixin, extractTotalFee, inner = this)
    }

    fun MaxActionProvider.considerConsumers(
        assetSourceRegistry: AssetSourceRegistry,
        chainRegistry: ChainRegistry,
        accountInfoFlow: Flow<AccountInfo>
    ): MaxActionProvider {
        return NativeConsumersAwareMaxActionProvider(assetSourceRegistry, chainRegistry, accountInfoFlow, this)
    }
}

class MaxActionProviderFactory(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainRegistry: ChainRegistry,
) {

    fun <F : GenericFee> create(
        assetFlow: Flow<Asset?>,
        field: (Asset) -> Balance,
        accountInfoFlow: Flow<AccountInfo>,
        feeLoaderMixin: GenericFeeLoaderMixin<F>,
        extractTotalFee: (F) -> Balance,
        allowMaxAction: Boolean = true
    ): MaxActionProvider {
        return assetFlow.providingMaxOf(field, allowMaxAction)
            .deductFee(feeLoaderMixin, extractTotalFee)
            .considerConsumers(assetSourceRegistry, chainRegistry, accountInfoFlow)
    }
}

infix operator fun MaxActionProvider.MaxAvailableForAction?.minus(other: BigInteger?): MaxActionProvider.MaxAvailableForAction? {
    if (this == null || other == null) return null

    val difference = balance - other

    return MaxActionProvider.MaxAvailableForAction(difference.atLeastZero(), chainAsset)
}
