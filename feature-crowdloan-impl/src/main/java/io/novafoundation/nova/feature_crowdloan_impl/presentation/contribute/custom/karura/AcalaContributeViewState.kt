package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.karura

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.karura.AcalaContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.referral.ReferralCodePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.referral.ReferralContributeViewState
import java.math.BigDecimal

class AcalaContributeViewState(
    private val interactor: AcalaContributeInteractor,
    customContributePayload: CustomContributePayload,
    resourceManager: ResourceManager,
    defaultReferralCode: String,
    private val bonusPercentage: BigDecimal,
) : ReferralContributeViewState(
    customContributePayload = customContributePayload,
    resourceManager = resourceManager,
    defaultReferralCode = defaultReferralCode,
    bonusPercentage = bonusPercentage
) {

    override fun createBonusPayload(referralCode: String): ReferralCodePayload {
        return AcalaBonusPayload(
            referralCode = referralCode,
            rewardRate = customContributePayload.parachainMetadata.rewardRate,
            referralBonus = bonusPercentage
        )
    }

    override suspend fun validatePayload(payload: ReferralCodePayload) {
        val isReferralValid = interactor.isReferralValid(payload.referralCode)

        if (!isReferralValid) throw IllegalArgumentException(resourceManager.getString(R.string.crowdloan_referral_code_invalid))
    }
}
