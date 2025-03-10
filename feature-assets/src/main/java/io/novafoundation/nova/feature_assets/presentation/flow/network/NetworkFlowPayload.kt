package io.novafoundation.nova.feature_assets.presentation.flow.network

import android.os.Parcelable
import io.novafoundation.nova.common.utils.TokenSymbol
import kotlinx.android.parcel.Parcelize

@Parcelize
class NetworkFlowPayload(val tokenSymbol: String) : Parcelable

fun NetworkFlowPayload.asTokenSymbol() = TokenSymbol(tokenSymbol)
