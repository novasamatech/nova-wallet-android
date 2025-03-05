package io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel

import android.os.Parcelable
import io.novafoundation.nova.common.utils.orZero
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.math.BigInteger

sealed class StakeTargetStakeParcelModel : Parcelable {

    @Parcelize
    object Inactive : StakeTargetStakeParcelModel()

    @Parcelize
    class Active(
        val totalStake: BigInteger,
        val ownStake: BigInteger?, // null in case unknown
        val minimumStake: BigInteger?, // null in case there is no separate min stake for this stake target
        val stakers: List<StakerParcelModel>?, // null in case unknown
        val stakersCount: Int = stakers?.size.orZero(),
        val rewards: BigDecimal,
        val isOversubscribed: Boolean,
        val userStakeInfo: UserStakeInfo? = null
    ) : StakeTargetStakeParcelModel() {

        @Parcelize
        class UserStakeInfo(val willBeRewarded: Boolean) : Parcelable
    }
}
