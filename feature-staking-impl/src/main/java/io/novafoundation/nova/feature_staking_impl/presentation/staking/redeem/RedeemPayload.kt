package io.novafoundation.nova.feature_staking_impl.presentation.staking.redeem

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.PendingNavigationAction
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import kotlinx.android.parcel.Parcelize

@Parcelize
class RedeemPayload(val overrideFinishAction: PendingNavigationAction<StakingRouter>?) : Parcelable
