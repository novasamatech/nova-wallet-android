package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.acala

import android.content.Context
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_crowdloan_impl.BuildConfig
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.ExtraBonusFlow
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.acala.AcalaContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeViewState
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.bonus.AcalaContributeSubmitter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.bonus.AcalaContributeViewState
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.confirm.AcalaConfirmContributeCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.select.AcalaSelectContributeCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.referral.ReferralContributeView
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal

abstract class AcalaBasedContributeFactory(
    override val submitter: AcalaContributeSubmitter,
    override val extraBonusFlow: AcalaBasedExtraBonusFlow,
) : CustomContributeFactory

abstract class AcalaBasedExtraBonusFlow(
    private val interactor: AcalaContributeInteractor,
    private val resourceManager: ResourceManager,
    private val defaultReferralCode: String,
) : ExtraBonusFlow {

    protected open val bonusMultiplier: BigDecimal = 0.05.toBigDecimal()

    override fun createViewState(scope: CoroutineScope, payload: CustomContributePayload): CustomContributeViewState {
        return AcalaContributeViewState(interactor, payload, resourceManager, defaultReferralCode, bonusMultiplier)
    }

    override fun createView(context: Context) = ReferralContributeView(context)
}

class AcalaContributeFactory(
    submitter: AcalaContributeSubmitter,
    extraBonusFlow: AcalaExtraBonusFlow,
    override val selectContributeCustomization: AcalaSelectContributeCustomization,
    override val confirmContributeCustomization: AcalaConfirmContributeCustomization,
) : AcalaBasedContributeFactory(
    submitter = submitter,
    extraBonusFlow = extraBonusFlow
) {

    override val flowType: String = "Acala"
}

class AcalaExtraBonusFlow(
    interactor: AcalaContributeInteractor,
    resourceManager: ResourceManager,
) : AcalaBasedExtraBonusFlow(
    interactor = interactor,
    resourceManager = resourceManager,
    defaultReferralCode = BuildConfig.ACALA_NOVA_REFERRAL
)

class KaruraContributeFactory(
    submitter: AcalaContributeSubmitter,
    extraBonusFlow: KaruraExtraBonusFlow,
) : AcalaBasedContributeFactory(
    submitter = submitter,
    extraBonusFlow = extraBonusFlow
) {

    override val flowType: String = "Karura"
}

class KaruraExtraBonusFlow(
    interactor: AcalaContributeInteractor,
    resourceManager: ResourceManager,
) : AcalaBasedExtraBonusFlow(
    interactor = interactor,
    resourceManager = resourceManager,
    defaultReferralCode = BuildConfig.KARURA_NOVA_REFERRAL
)
