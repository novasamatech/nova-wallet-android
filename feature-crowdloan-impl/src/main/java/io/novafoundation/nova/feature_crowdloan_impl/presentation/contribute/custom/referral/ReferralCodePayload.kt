package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.referral

import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload

interface ReferralCodePayload : BonusPayload {

    val referralCode: String
}
