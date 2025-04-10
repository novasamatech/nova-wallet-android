package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class CustomValidatorsPayload(
    val flowType: FlowType
) : Parcelable {

    enum class FlowType {
        SETUP_STAKING_VALIDATORS,
        CHANGE_STAKING_VALIDATORS
    }
}
