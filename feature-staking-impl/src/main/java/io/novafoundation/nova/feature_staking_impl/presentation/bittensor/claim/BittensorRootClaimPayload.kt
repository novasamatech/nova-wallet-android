package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BittensorRootClaimPayload(val metaId: Long) : Parcelable
