package io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.parcelize.Parcelize

@Parcelize
class AddTokenEnterInfoPayload(val chainId: ChainId) : Parcelable
