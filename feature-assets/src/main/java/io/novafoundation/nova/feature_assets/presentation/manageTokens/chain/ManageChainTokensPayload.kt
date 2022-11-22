package io.novafoundation.nova.feature_assets.presentation.manageTokens.chain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ManageChainTokensPayload(
    val multiChainTokenId: String
) : Parcelable
