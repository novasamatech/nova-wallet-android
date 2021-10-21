package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.bifrost

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.bifrost.BifrostContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.referral.ReferralCodePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.referral.ReferralContributeViewState

class BifrostContributeViewState(
    interactor: BifrostContributeInteractor,
    customContributePayload: CustomContributePayload,
    resourceManager: ResourceManager,
    termsLink: String,
    private val bifrostInteractor: BifrostContributeInteractor
) : ReferralContributeViewState(
    customContributePayload = customContributePayload,
    resourceManager = resourceManager,
    defaultReferralCode = interactor.novaReferralCode,
    bonusPercentage = BIFROST_BONUS_MULTIPLIER,
    termsUrl = termsLink
) {

    override fun createBonusPayload(referralCode: String): ReferralCodePayload {
        return BifrostBonusPayload(
            referralCode,
            customContributePayload.paraId,
            customContributePayload.parachainMetadata.rewardRate
        )
    }

    override suspend fun validatePayload(payload: ReferralCodePayload) {
        if (bifrostInteractor.isCodeValid(payload.referralCode).not()) {
            throw IllegalArgumentException(resourceManager.getString(R.string.crowdloan_referral_code_invalid))
        }
    }
}
