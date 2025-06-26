package io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ManageChainTokensPayload(
    val multiChainTokenId: String
) : Parcelable
