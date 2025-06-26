package io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class StakeTargetDetailsParcelModel(
    val accountIdHex: String,
    val isSlashed: Boolean,
    val stake: StakeTargetStakeParcelModel,
    val identity: IdentityParcelModel?,
) : Parcelable
