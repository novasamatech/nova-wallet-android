package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.PendingNavigationAction
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import kotlinx.android.parcel.Parcelize

@Parcelize
class SelectBondMorePayload(val overrideFinishAction: PendingNavigationAction<StakingRouter>?) : Parcelable
