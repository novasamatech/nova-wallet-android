package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards

import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import java.math.BigInteger

data class TinderGovCardRvItem(
    val id: BigInteger,
    val summary: ExtendedLoadingState<String>,
    val requestedAmount: ExtendedLoadingState<AmountModel>?,
    val descriptiveButtonState: DescriptiveButtonState,
    @DrawableRes val backgroundRes: Int
)
