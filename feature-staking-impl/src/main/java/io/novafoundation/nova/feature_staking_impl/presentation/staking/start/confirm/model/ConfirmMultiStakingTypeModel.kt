package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.model

import io.novafoundation.nova.common.utils.images.Icon

class ConfirmMultiStakingTypeModel(
    val stakingTypeValue: String,
    val stakingTypeDetails: TypeDetails
) {

    class TypeDetails(
        val label: String,
        val value: String,
        val icon: Icon?,
    )
}
