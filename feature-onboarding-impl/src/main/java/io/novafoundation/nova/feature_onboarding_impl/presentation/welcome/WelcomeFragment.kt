package io.novafoundation.nova.feature_onboarding_impl.presentation.welcome

import android.os.Bundle
import android.view.View

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.formatting.applyTermsAndPrivacyPolicy
import io.novafoundation.nova.common.utils.insets.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.insets.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setVisible
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
        binder.welcomeBottomStack.applyNavigationBarInsets()
    }

    override fun initViews() {
        binder.welcomeTerms.applyTermsAndPrivacyPolicy(
            containerResId = R.string.legal_consent_implicit,
            termsResId = R.string.common_terms_of_service,
            privacyResId = R.string.common_privacy_notice,
            termsClicked = viewModel::termsClicked,
            privacyClicked = viewModel::privacyClicked,
            underlineLinks = true
        )

        binder.welcomeCreateWalletButton.setOnClickListener { viewModel.createAccountClicked() }
        binder.welcomeRestoreWalletButton.setOnClickListener { viewModel.importAccountClicked() }

        binder.welcomeBackButton.setOnClickListener { viewModel.backClicked() }
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
