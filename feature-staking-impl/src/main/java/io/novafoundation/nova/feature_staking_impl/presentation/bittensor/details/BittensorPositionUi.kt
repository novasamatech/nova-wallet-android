package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

data class BittensorPositionUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: AmountModel,
    val icon: Icon,
    val action: BittensorPositionAction,
    val iconSizeDp: Int = 40
)

sealed interface BittensorPositionAction {
    data object RootDetails : BittensorPositionAction
    data class ValidatorDetails(val address: String) : BittensorPositionAction
    data class SubnetDetails(val subnetId: BittensorNetuid.Subnet) : BittensorPositionAction
}
