package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import androidx.lifecycle.asFlow
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider.MaxAvailableForAction
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProviderDsl.deductFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProviderDsl.providingMaxOf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigInteger

interface MaxActionProvider {

    class MaxAvailableForAction(val balance: Balance, val chainAsset: Chain.Asset)

    val maxAvailableForDisplay: Flow<Balance?>

    val maxAvailableForAction: Flow<MaxAvailableForAction?>
}

class SimpleMaxActionProvider(
    override val maxAvailableForDisplay: Flow<Balance>,
    override val maxAvailableForAction: Flow<MaxAvailableForAction?>
) : MaxActionProvider

class AssetMaxActionProvider(
    private val assetFlow: Flow<Asset?>,
    private val assetField: (Asset) -> Balance,
    private val allowMaxAction: Boolean
) : MaxActionProvider {

    override val maxAvailableForDisplay: Flow<Balance?> = assetFlow.map { it?.let(assetField) }

    override val maxAvailableForAction: Flow<MaxAvailableForAction?> = assetFlow.map { asset ->
        if (allowMaxAction && asset != null) {
            val extractedBalance = assetField(asset)

            MaxAvailableForAction(extractedBalance, asset.token.configuration)
        } else {
            null
        }
    }
}

class FeeAwareMaxActionProvider<F : GenericFee>(
    feeInputMixin: GenericFeeLoaderMixin<F>,
    private val extractTotalFee: (F) -> Balance,
    inner: MaxActionProvider,
) : MaxActionProvider {

    // Fee is not deducted for display
    override val maxAvailableForDisplay: Flow<Balance?> = inner.maxAvailableForDisplay

    override val maxAvailableForAction: Flow<MaxAvailableForAction?> = combine(
        inner.maxAvailableForAction,
        feeInputMixin.feeLiveData.asFlow()
    ) { maxAvailable, newFeeStatus ->
        if (maxAvailable == null) return@combine null

        when (newFeeStatus) {
            // do not block in case there is no fee or fee is not yet present
            FeeStatus.Error, FeeStatus.NoFee -> maxAvailable

            is FeeStatus.Loaded -> {
                val feeAsset = newFeeStatus.feeModel.chainAsset
                val amountAsset = maxAvailable.chainAsset

                if (feeAsset.fullId == amountAsset.fullId) {
                    val genericFee = newFeeStatus.feeModel.decimalFee.genericFee
                    val extractedFee = extractTotalFee(genericFee)

                    maxAvailable - extractedFee
                } else {
                    maxAvailable
                }
            }

            FeeStatus.Loading -> null
        }
    }

    infix operator fun MaxAvailableForAction?.minus(other: BigInteger?): MaxAvailableForAction? {
        if (this == null || other == null) return null

        val difference = balance - other

        return MaxAvailableForAction(difference.atLeastZero(), chainAsset)
    }
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
}

fun <F : GenericFee> Flow<Asset?>.provideMaxWithFeeDeducted(
    field: (Asset) -> Balance,
    feeLoaderMixin: GenericFeeLoaderMixin<F>,
    extractTotalFee: (F) -> Balance,
    allowMaxAction: Boolean = true
): MaxActionProvider {
    return providingMaxOf(field, allowMaxAction)
        .deductFee(feeLoaderMixin, extractTotalFee)
}
