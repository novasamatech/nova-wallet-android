package io.novafoundation.nova.feature_wallet_api.presentation.common

import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface MinAmountProvider {
    fun provideMinAmount(): Flow<BigDecimal>
}
