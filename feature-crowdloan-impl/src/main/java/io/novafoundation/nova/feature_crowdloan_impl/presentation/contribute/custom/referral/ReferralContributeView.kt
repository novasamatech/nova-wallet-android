package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.referral

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.styleText
import io.novafoundation.nova.common.utils.observe
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.databinding.ViewReferralFlowBinding
import io.novafoundation.nova.feature_crowdloan_impl.di.CrowdloanFeatureComponent
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeView
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeViewState

import javax.inject.Inject

class ReferralContributeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : CustomContributeView(context, attrs, defStyle) {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val binder = ViewReferralFlowBinding.inflate(inflater(), this)

    init {
        FeatureUtils.getFeature<CrowdloanFeatureComponent>(
            context,
            CrowdloanFeatureApi::class.java
        ).inject(this)

        binder.referralPrivacyText.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun bind(
        viewState: CustomContributeViewState,
        scope: LifecycleCoroutineScope
    ) {
        require(viewState is ReferralContributeViewState)

        binder.referralReferralCodeInput.content.bindTo(viewState.enteredReferralCodeFlow, scope)
        binder.referralPrivacySwitch.bindTo(viewState.privacyAcceptedFlow, scope)

        binder.referralNovaBonusTitle.text = viewState.applyNovaTitle

        viewState.applyNovaCodeEnabledFlow.observe(scope) { enabled ->
            binder.referralNovaBonusApply.isEnabled = enabled

            val applyBonusButtonText = if (enabled) R.string.common_apply else R.string.common_applied
            binder.referralNovaBonusApply.setText(applyBonusButtonText)
        }

        viewState.bonusFlow.observe(scope) { bonus ->
            binder.referralBonus.setVisible(bonus != null)

            binder.referralBonus.showValue(bonus)
        }

        with(viewState.learnBonusesTitle) {
            binder.referralLearnMore.loadIcon(iconLink, imageLoader)
            binder.referralLearnMore.title.text = text

            binder.referralLearnMore.setOnClickListener { viewState.learnMoreClicked() }
        }

        binder.referralNovaBonusApply.setOnClickListener { viewState.applyNovaCode() }

        binder.referralPrivacyText.text = styleText(context.getString(R.string.onboarding_terms_and_conditions_1_v2_2_1)) {
            clickable(context.getString(R.string.onboarding_terms_and_conditions_2)) {
                viewState.termsClicked()
            }
        }

        viewState.openBrowserFlow.observe(scope) {
            context.showBrowser(it)
        }
    }
}
