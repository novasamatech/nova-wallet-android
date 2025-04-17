package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.model

import android.os.Parcelable
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.parcelize.Parcelize

@Parcelize
class ParachainStakingRebondPayload(
    val collatorId: AccountId
) : Parcelable
