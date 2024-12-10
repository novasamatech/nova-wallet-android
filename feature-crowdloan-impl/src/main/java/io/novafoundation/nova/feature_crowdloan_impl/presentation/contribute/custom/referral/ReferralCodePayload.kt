package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.referral

import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.utils.fractionToPercentage
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

interface ReferralCodePayload : BonusPayload {

    val referralCode: String
}

@Parcelize
class DefaultReferralCodePayload(
    override val referralCode: String,
    private val referralBonus: BigDecimal,
    private val rewardTokenSymbol: String,
    private val rewardRate: BigDecimal?,
) : ReferralCodePayload {

    override fun bonusText(amount: BigDecimal): String {
        return if (rewardRate == null) {
            referralBonus.fractionToPercentage().formatAsPercentage()
        } else {
            val bonusReward = amount * rewardRate * referralBonus

            bonusReward.formatTokenAmount(rewardTokenSymbol)
        }
    }
}
