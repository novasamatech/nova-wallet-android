package io.novafoundation.nova.feature_onboarding_impl.presentation.consentUpgrade

import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.text.style.UnderlineSpan
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_onboarding_api.di.OnboardingFeatureApi
import io.novafoundation.nova.feature_onboarding_impl.databinding.FragmentConsentBannerUpgradeBinding
import io.novafoundation.nova.feature_onboarding_impl.di.OnboardingFeatureComponent

class ConsentBannerUpgradeFragment : BaseBottomSheetFragment<ConsentBannerUpgradeViewModel, FragmentConsentBannerUpgradeBinding>() {

    override fun createBinding() = FragmentConsentBannerUpgradeBinding.inflate(layoutInflater)

    override fun initViews() {
        // Legal compliance: the user MUST tap Accept; no other dismissal
        // path is permitted. isCancelable=false blocks back-press and
        // tap-outside. On the BottomSheetBehavior we lock the sheet in the
        // EXPANDED state — skipCollapsed prevents the peek state, isHideable
        // prevents reaching STATE_HIDDEN, and forcing STATE_EXPANDED on
        // display ensures the full content (consent text + Accept button)
        // is visible immediately rather than clipped at the default peek
        // height.
        isCancelable = false
        with(getBehaviour()) {
            isHideable = false
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        configureConsentText()

        binder.consentUpgradeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.consentToggled(isChecked)
        }

        binder.consentUpgradeAccept.setOnClickListener { viewModel.acceptClicked() }
    }

    private fun configureConsentText() {
        val sourceText = getString(R.string.consent_banner_text_template)
        val termsText = getString(R.string.consent_banner_terms_of_service)
        val privacyText = getString(R.string.consent_banner_privacy_notice)
        val clickableColor = requireContext().getColor(R.color.text_primary)

        binder.consentUpgradeText.text = SpannableFormatter.format(
            sourceText,
            termsText.toSpannable(colorSpan(clickableColor))
                .setFullSpan(clickableSpan(viewModel::termsClicked))
                .setFullSpan(UnderlineSpan()),
            privacyText.toSpannable(colorSpan(clickableColor))
                .setFullSpan(clickableSpan(viewModel::privacyClicked))
                .setFullSpan(UnderlineSpan()),
        )
        binder.consentUpgradeText.movementMethod = LinkMovementMethod.getInstance()
        binder.consentUpgradeText.highlightColor = Color.TRANSPARENT
    }

    override fun inject() {
        FeatureUtils.getFeature<OnboardingFeatureComponent>(this, OnboardingFeatureApi::class.java)
            .consentBannerUpgradeComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ConsentBannerUpgradeViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.consentAccepted.observe { accepted ->
            if (binder.consentUpgradeCheckbox.isChecked != accepted) {
                binder.consentUpgradeCheckbox.isChecked = accepted
            }
        }

        viewModel.acceptButtonState.observe { state ->
            binder.consentUpgradeAccept.setState(state)
        }

        viewModel.dismissEvent.observeEvent { dismiss() }
    }
}
