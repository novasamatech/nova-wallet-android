package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorValidatorFilters
import kotlinx.parcelize.Parcelize

interface BittensorValidatorFiltersCommunicator :
    InterScreenCommunicator<BittensorValidatorFiltersCommunicator.Request, BittensorValidatorFiltersCommunicator.Response> {

    /** [tokenSymbol] names the unit of the "Total staked" sort, when known. */
    @Parcelize
    data class Request(val filters: BittensorValidatorFilters, val tokenSymbol: String?) : Parcelable

    /** Sent only on Apply; Back leaves the validator list filters unchanged. */
    @Parcelize
    data class Response(val filters: BittensorValidatorFilters) : Parcelable
}
