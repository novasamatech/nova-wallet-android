package io.novafoundation.nova.feature_swap_impl.presentation.main

import android.os.Parcelable
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import kotlinx.parcelize.Parcelize

@Parcelize
class SwapTabPayload(val requestId: String, val settings: SwapSettingsPayload) : Parcelable
