package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.referral

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.styleText
import io.novafoundation.nova.common.utils.observe
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_crowdloan_impl.R
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

    init {
        View.inflate(context, R.layout.view_referral_flow, this)

        FeatureUtils.getFeature<CrowdloanFeatureComponent>(
            context,
            CrowdloanFeatureApi::class.java
        ).inject(this)

        referralPrivacyText.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun bind(
        viewState: CustomContributeViewState,
        scope: LifecycleCoroutineScope
    ) {
        require(viewState is ReferralContributeViewState)

        referralReferralCodeInput.content.bindTo(viewState.enteredReferralCodeFlow, scope)
        referralPrivacySwitch.bindTo(viewState.privacyAcceptedFlow, scope)

        referralNovaBonusTitle.text = viewState.applyNovaTitle

        viewState.applyNovaCodeEnabledFlow.observe(scope) { enabled ->
            referralNovaBonusApply.isEnabled = enabled

            val applyBonusButtonText = if (enabled) R.string.common_apply else R.string.common_applied
            referralNovaBonusApply.setText(applyBonusButtonText)
        }

        viewState.bonusFlow.observe(scope) { bonus ->
            referralBonus.setVisible(bonus != null)

            referralBonus.showValue(bonus)
        }

        with(viewState.learnBonusesTitle) {
            referralLearnMore.loadIcon(iconLink, imageLoader)
            referralLearnMore.title.text = text

            referralLearnMore.setOnClickListener { viewState.learnMoreClicked() }
        }

        referralNovaBonusApply.setOnClickListener { viewState.applyNovaCode() }

        referralPrivacyText.text = styleText(context.getString(R.string.onboarding_terms_and_conditions_1_v2_2_1)) {
            clickable(context.getString(R.string.onboarding_terms_and_conditions_2)) {
                viewState.termsClicked()
            }
        }

        viewState.openBrowserFlow.observe(scope) {
            context.showBrowser(it)
        }
    }
}
