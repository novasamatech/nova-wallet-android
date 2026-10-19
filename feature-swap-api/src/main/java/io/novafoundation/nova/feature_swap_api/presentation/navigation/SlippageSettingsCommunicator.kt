package io.novafoundation.nova.feature_swap_api.presentation.navigation

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenCommunicator
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import kotlinx.parcelize.Parcelize

interface SlippageSettingsCommunicator : InterScreenCommunicator<SlippageSettingsPayload, SlippageSettingsCommunicator.Response> {

    @Parcelize
    data class Response(val fraction: Double) : Parcelable
}
