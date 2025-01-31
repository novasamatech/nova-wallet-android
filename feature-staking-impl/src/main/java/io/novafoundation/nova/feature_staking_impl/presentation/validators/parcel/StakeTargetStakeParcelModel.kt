package io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.BigInteger

sealed class StakeTargetStakeParcelModel : Parcelable {

    @Parcelize
    object Inactive : StakeTargetStakeParcelModel()

    @Parcelize
    class Active(
        val totalStake: BigInteger,
        val ownStake: BigInteger,
        val minimumStake: BigInteger?, // null in case there is no separate min stake for this stake target
        val stakers: List<StakerParcelModel>,
        val rewards: BigDecimal,
        val isOversubscribed: Boolean,
        val userStakeInfo: UserStakeInfo? = null
    ) : StakeTargetStakeParcelModel() {

        @Parcelize
        class UserStakeInfo(val willBeRewarded: Boolean) : Parcelable
    }
}
