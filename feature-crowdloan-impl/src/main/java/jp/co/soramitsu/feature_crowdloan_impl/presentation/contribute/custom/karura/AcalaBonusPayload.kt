package jp.co.soramitsu.feature_crowdloan_impl.presentation.contribute.custom.karura

import jp.co.soramitsu.feature_crowdloan_impl.presentation.contribute.custom.referral.ReferralCodePayload
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class AcalaBonusPayload(
    override val referralCode: String,
    private val referralBonus: BigDecimal,
    private val rewardRate: BigDecimal?
) : ReferralCodePayload {

    override fun calculateBonus(amount: BigDecimal): BigDecimal? {
        return rewardRate?.let { amount * rewardRate * referralBonus }
    }
}
