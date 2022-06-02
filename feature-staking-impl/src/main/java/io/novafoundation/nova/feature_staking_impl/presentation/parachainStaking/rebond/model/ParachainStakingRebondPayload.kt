package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.model

import android.os.Parcelable
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize

@Parcelize
class ParachainStakingRebondPayload(
    val collatorId: AccountId
) : Parcelable
