package io.novafoundation.nova.feature_gift_impl.presentation.amount

import io.novafoundation.nova.feature_gift_impl.domain.CreateGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.amount.fee.GiftFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.common.MinAmountProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class GiftMinAmountProviderFactory(
    private val createGiftInteractor: CreateGiftInteractor,
) {
    fun create(
        feeMixin: GiftFeeLoaderMixin
    ): MinAmountProvider {
        return GiftMinAmountProvider(
            createGiftInteractor,
            feeMixin
        )
    }
}

class GiftMinAmountProvider(
    private val createGiftInteractor: CreateGiftInteractor,
    private val feeMixin: GiftFeeLoaderMixin,
) : MinAmountProvider {

    override fun provideMinAmount(): Flow<BigDecimal> {
        val feeFlow = feeMixin.fee
            .mapNotNull { it.loadedFeeOrNull() }

        val chainAssetWithEDFlow = feeMixin.feeChainAssetFlow
            .map { it to createGiftInteractor.getExistentialDeposit(it) }

        return combine(
            feeFlow,
            chainAssetWithEDFlow
        ) { fee, chainAssetWithEd ->
            val chainAsset = chainAssetWithEd.first
            val existentialDeposit = chainAssetWithEd.second
            chainAsset.amountFromPlanks(fee.amount) + existentialDeposit
        }
    }
}
