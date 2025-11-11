package io.novafoundation.nova.feature_gift_impl.presentation.amount

import io.novafoundation.nova.feature_gift_impl.domain.CreateGiftInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.common.MinAmountProvider
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class GiftMinAmountProviderFactory(
    private val createGiftInteractor: CreateGiftInteractor,
) {
    fun create(
        chainAssetFlow: Flow<Chain.Asset>
    ): MinAmountProvider {
        return GiftMinAmountProvider(
            createGiftInteractor,
            chainAssetFlow
        )
    }
}

class GiftMinAmountProvider(
    private val createGiftInteractor: CreateGiftInteractor,
    private val chainAssetFlow: Flow<Chain.Asset>
) : MinAmountProvider {

    override fun provideMinAmount(): Flow<BigDecimal> {
        return chainAssetFlow
            .map { createGiftInteractor.getExistentialDeposit(it) }
    }
}
