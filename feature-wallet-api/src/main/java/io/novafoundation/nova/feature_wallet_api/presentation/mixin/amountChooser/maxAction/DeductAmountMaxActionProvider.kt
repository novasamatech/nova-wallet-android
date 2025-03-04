package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class DeductAmountMaxActionProvider(
    amount: Flow<Balance>,
    inner: MaxActionProvider,
) : MaxActionProvider {

    override val maxAvailableBalance: Flow<MaxAvailableBalance> = combine(
        inner.maxAvailableBalance,
        amount
    ) { maxAvailable, lastRecordedAmount ->
        val actualAvailableBalance = (maxAvailable.actualBalance - lastRecordedAmount).atLeastZero()

        maxAvailable.copy(
            displayedBalance = actualAvailableBalance,
            actualBalance = actualAvailableBalance
        )
    }.distinctUntilChanged()
}
