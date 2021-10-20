package jp.co.soramitsu.feature_crowdloan_impl.di.customCrowdloan.acala

import android.content.Context
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.feature_crowdloan_impl.BuildConfig
import jp.co.soramitsu.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import jp.co.soramitsu.feature_crowdloan_impl.domain.contribute.custom.karura.AcalaContributeInteractor
import jp.co.soramitsu.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeViewState
import jp.co.soramitsu.feature_crowdloan_impl.presentation.contribute.custom.karura.AcalaContributeSubmitter
import jp.co.soramitsu.feature_crowdloan_impl.presentation.contribute.custom.karura.AcalaContributeViewState
import jp.co.soramitsu.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload
import jp.co.soramitsu.feature_crowdloan_impl.presentation.contribute.custom.referral.ReferralContributeView
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal

abstract class AcalaBasedContributeFactory(
    override val submitter: AcalaContributeSubmitter,
    private val interactor: AcalaContributeInteractor,
    private val resourceManager: ResourceManager,
    private val defaultReferralCode: String,
) : CustomContributeFactory {

    protected open val bonusMultiplier: BigDecimal = 0.05.toBigDecimal()

    override fun createViewState(scope: CoroutineScope, payload: CustomContributePayload): CustomContributeViewState {
        return AcalaContributeViewState(interactor, payload, resourceManager, defaultReferralCode, bonusMultiplier)
    }

    override fun createView(context: Context) = ReferralContributeView(context)
}

class AcalaContributeFactory(
    submitter: AcalaContributeSubmitter,
    interactor: AcalaContributeInteractor,
    resourceManager: ResourceManager,
) : AcalaBasedContributeFactory(
    submitter = submitter,
    interactor = interactor,
    resourceManager = resourceManager,
    defaultReferralCode = BuildConfig.ACALA_NOVA_REFERRAL
) {

    override val flowType: String = "Acala"
}

class KaruraContributeFactory(
    submitter: AcalaContributeSubmitter,
    interactor: AcalaContributeInteractor,
    resourceManager: ResourceManager,
) : AcalaBasedContributeFactory(
    submitter = submitter,
    interactor = interactor,
    resourceManager = resourceManager,
    defaultReferralCode = BuildConfig.KARURA_NOVA_REFERRAL
)  {

    override val flowType: String = "Karura"
}
