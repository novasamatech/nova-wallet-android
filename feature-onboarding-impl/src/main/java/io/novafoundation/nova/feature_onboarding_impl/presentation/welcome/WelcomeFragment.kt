package io.novafoundation.nova.feature_onboarding_impl.presentation.welcome

import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.insets.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.insets.applyStatusBarInsets
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_onboarding_api.di.OnboardingFeatureApi
import io.novafoundation.nova.feature_onboarding_impl.R
import io.novafoundation.nova.feature_onboarding_impl.databinding.FragmentWelcomeBinding
import io.novafoundation.nova.feature_onboarding_impl.di.OnboardingFeatureComponent

class WelcomeFragment : BaseFragment<WelcomeViewModel, FragmentWelcomeBinding>() {

    companion object {
        private const val KEY_DISPLAY_BACK = "display_back"
        private const val KEY_ADD_ACCOUNT_PAYLOAD = "add_account_payload"

        fun bundle(displayBack: Boolean): Bundle {
            return Bundle().apply {
                putBoolean(KEY_DISPLAY_BACK, displayBack)
                putParcelable(KEY_ADD_ACCOUNT_PAYLOAD, AddAccountPayload.MetaAccount)
            }
        }

        fun bundle(payload: AddAccountPayload): Bundle {
            return Bundle().apply {
                putBoolean(KEY_DISPLAY_BACK, true)
                putParcelable(KEY_ADD_ACCOUNT_PAYLOAD, payload)
            }
        }
    }

    override fun createBinding() = FragmentWelcomeBinding.inflate(layoutInflater)

    override fun applyInsets(rootView: View) {
        binder.welcomeStatus.applyStatusBarInsets()
        binder.welcomeTerms.applyNavigationBarInsets()
    }

    override fun initViews() {
        configureTermsAndPrivacy(
            getString(R.string.onboarding_terms_and_conditions_1_v2_2_1),
            getString(R.string.onboarding_terms_and_conditions_2),
            getString(R.string.onboarding_privacy_policy)
        )
        binder.welcomeTerms.movementMethod = LinkMovementMethod.getInstance()
        binder.welcomeTerms.highlightColor = Color.TRANSPARENT

        binder.welcomeCreateWalletButton.setOnClickListener { viewModel.createAccountClicked() }
        binder.welcomeRestoreWalletButton.setOnClickListener { viewModel.importAccountClicked() }

        binder.welcomeBackButton.setOnClickListener { viewModel.backClicked() }
    }

    private fun configureTermsAndPrivacy(sourceText: String, terms: String, privacy: String) {
        val clickableColor = requireContext().getColor(R.color.text_primary)

        binder.welcomeTerms.text = SpannableFormatter.format(
            sourceText,
            terms.toSpannable(colorSpan(clickableColor)).setFullSpan(clickableSpan(viewModel::termsClicked)),
            privacy.toSpannable(colorSpan(clickableColor)).setFullSpan(clickableSpan(viewModel::privacyClicked)),
        )
    }

    override fun inject() {
        FeatureUtils.getFeature<OnboardingFeatureComponent>(context!!, OnboardingFeatureApi::class.java)
            .welcomeComponentFactory()
            .create(
                fragment = this,
                shouldShowBack = argument(KEY_DISPLAY_BACK),
                addAccountPayload = argument(KEY_ADD_ACCOUNT_PAYLOAD)
            )
            .inject(this)
    }

    override fun subscribe(viewModel: WelcomeViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.shouldShowBackLiveData.observe(binder.welcomeBackButton::setVisible)
    }
}
