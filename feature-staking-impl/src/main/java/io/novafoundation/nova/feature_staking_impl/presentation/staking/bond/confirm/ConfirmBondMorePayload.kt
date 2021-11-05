package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.PendingNavigationAction
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class ConfirmBondMorePayload(
    val amount: BigDecimal,
    val fee: BigDecimal,
    val stashAddress: String,
    val overrideFinishAction: PendingNavigationAction<StakingRouter>?
) : Parcelable
