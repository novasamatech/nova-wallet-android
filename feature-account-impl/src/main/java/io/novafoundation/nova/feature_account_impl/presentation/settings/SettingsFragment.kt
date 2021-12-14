package io.novafoundation.nova.feature_account_impl.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.sendEmailIntent
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import kotlinx.android.synthetic.main.fragment_profile.accountView
import kotlinx.android.synthetic.main.fragment_profile.settingsAppVersion
import kotlinx.android.synthetic.main.fragment_profile.settingsContainer
import kotlinx.android.synthetic.main.fragment_profile.settingsEmail
import kotlinx.android.synthetic.main.fragment_profile.settingsGithub
import kotlinx.android.synthetic.main.fragment_profile.settingsLanguage
import kotlinx.android.synthetic.main.fragment_profile.settingsNetworks
import kotlinx.android.synthetic.main.fragment_profile.settingsPin
import kotlinx.android.synthetic.main.fragment_profile.settingsPrivacy
import kotlinx.android.synthetic.main.fragment_profile.settingsRateUs
import kotlinx.android.synthetic.main.fragment_profile.settingsTelegram
import kotlinx.android.synthetic.main.fragment_profile.settingsTerms
import kotlinx.android.synthetic.main.fragment_profile.settingsTwitter
import kotlinx.android.synthetic.main.fragment_profile.settingsWallets
import kotlinx.android.synthetic.main.fragment_profile.settingsWebsite
import kotlinx.android.synthetic.main.fragment_profile.settingsYoutube

class SettingsFragment : BaseFragment<SettingsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun initViews() {
        settingsContainer.applyStatusBarInsets()

        accountView.setWholeClickListener { viewModel.accountActionsClicked() }

        settingsWallets.setOnClickListener { viewModel.walletsClicked() }
        settingsNetworks.setOnClickListener { viewModel.networksClicked() }

        settingsLanguage.setOnClickListener { viewModel.languagesClicked() }

        settingsTelegram.setOnClickListener { viewModel.telegramClicked() }
        settingsTwitter.setOnClickListener { viewModel.twitterClicked() }
        settingsYoutube.setOnClickListener { viewModel.openYoutube() }

        settingsWebsite.setOnClickListener { viewModel.websiteClicked() }
        settingsGithub.setOnClickListener { viewModel.githubClicked() }
        settingsTerms.setOnClickListener { viewModel.termsClicked() }
        settingsPrivacy.setOnClickListener { viewModel.privacyClicked() }

        settingsEmail.setOnClickListener { viewModel.emailClicked() }
        settingsRateUs.setOnClickListener { viewModel.rateUsClicked() }

        settingsPin.setOnClickListener { viewModel.changePinCodeClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .profileComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SettingsViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.selectedAccountFlow.observe { account ->
            accountView.setTitle(account.name)
        }

        viewModel.accountIconFlow.observe(accountView::setAccountIcon)

        viewModel.selectedLanguageFlow.observe {
            settingsLanguage.setValue(it.displayName)
        }

        viewModel.appVersionFlow.observe(settingsAppVersion::setText)

        viewModel.openEmailEvent.observeEvent { requireContext().sendEmailIntent(it) }
    }
}
