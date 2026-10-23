package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorSorting
import kotlinx.parcelize.Parcelize

@Parcelize
data class BittensorValidatorFilters(
    val sorting: BittensorValidatorSorting = BittensorValidatorSorting.APY,
    val hideUnusualApy: Boolean = true
) : Parcelable {

    val isDefault: Boolean
        get() = this == BittensorValidatorFilters()
}
