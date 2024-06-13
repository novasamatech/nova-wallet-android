package io.novafoundation.nova.feature_onboarding_impl.presentation.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.formatting.applyTermsAndPrivacyPolicy
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.setupImportTypeChooser
import io.novafoundation.nova.feature_onboarding_api.di.OnboardingFeatureApi
import io.novafoundation.nova.feature_onboarding_impl.R
import io.novafoundation.nova.feature_onboarding_impl.di.OnboardingFeatureComponent
import kotlinx.android.synthetic.main.fragment_welcome.back
import kotlinx.android.synthetic.main.fragment_welcome.createAccountBtn
import kotlinx.android.synthetic.main.fragment_welcome.importAccountBtn
import kotlinx.android.synthetic.main.fragment_welcome.termsTv
import kotlinx.android.synthetic.main.fragment_welcome.welcomeAddWatchWallet
import kotlinx.android.synthetic.main.fragment_welcome.welcomeConnectHardwareWallet

class WelcomeFragment : BaseFragment<WelcomeViewModel>() {

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    override fun initViews() {
        createAccountBtn.setOnClickListener { viewModel.createAccountClicked() }
        importAccountBtn.setOnClickListener { viewModel.importAccountClicked() }
        welcomeAddWatchWallet.setOnClickListener { viewModel.addWatchWalletClicked() }
        welcomeConnectHardwareWallet.setOnClickListener { viewModel.connectHardwareWalletClicked() }

        back.setOnClickListener { viewModel.backClicked() }
        configureTermsAndPrivacy()
    }

    private fun configureTermsAndPrivacy() {
        termsTv.applyTermsAndPrivacyPolicy(
            R.string.onboarding_terms_and_conditions_1_v2_2_1,
            R.string.onboarding_terms_and_conditions_2,
            R.string.onboarding_privacy_policy,
            viewModel::termsClicked,
            viewModel::privacyClicked
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
        setupImportTypeChooser(viewModel)

        viewModel.shouldShowBackLiveData.observe(back::setVisible)

        viewModel.selectHardwareWallet.awaitableActionLiveData.observeEvent {
            SelectHardwareWalletBottomSheet(requireContext(), it.payload, it.onSuccess)
                .show()
        }
    }
}
