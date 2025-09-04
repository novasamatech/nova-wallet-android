package io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.maxAction.MaxAvailableDeduction
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProviderDsl
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

enum class MaxBalanceType {
    TRANSFERABLE, TOTAL, FREE
}

class MaxActionProviderFactory(
    private val assetSourceRegistry: AssetSourceRegistry
) {

    fun <F : MaxAvailableDeduction> create(
        viewModelScope: CoroutineScope,
        assetInFlow: Flow<Asset>,
        feeLoaderMixin: FeeLoaderMixinV2<F, *>,
        balance: suspend (Asset) -> BigInteger,
        deductEd: Flow<Boolean> = flowOf(false)
    ): MaxActionProvider {
        return MaxActionProvider.create(viewModelScope) {
            assetInFlow.providingMaxOf(balance)
                .deductFee(feeLoaderMixin)
                .deductEd(assetSourceRegistry, deductEd)
        }
    }

    fun createCustom(
        viewModelScope: CoroutineScope,
        builder: MaxActionProviderDsl.() -> MaxActionProvider
    ): MaxActionProvider {
        return MaxActionProvider.create(viewModelScope) {
            builder()
        }
    }
}

fun <F : MaxAvailableDeduction> MaxActionProviderFactory.create(
    viewModelScope: CoroutineScope,
    assetInFlow: Flow<Asset>,
    feeLoaderMixin: FeeLoaderMixinV2<F, *>,
    maxBalanceType: MaxBalanceType = MaxBalanceType.TRANSFERABLE,
    deductEd: Flow<Boolean> = flowOf(false)
): MaxActionProvider {
    return create(
        viewModelScope = viewModelScope,
        assetInFlow = assetInFlow,
        feeLoaderMixin = feeLoaderMixin,
        balance = when (maxBalanceType) {
            MaxBalanceType.TRANSFERABLE -> Asset::transferableInPlanks
            MaxBalanceType.TOTAL -> Asset::totalInPlanks
            MaxBalanceType.FREE -> Asset::freeInPlanks
        },
        deductEd = deductEd
    )
}
