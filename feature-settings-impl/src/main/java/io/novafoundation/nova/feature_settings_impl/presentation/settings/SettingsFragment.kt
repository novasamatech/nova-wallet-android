package io.novafoundation.nova.feature_settings_impl.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.sendEmailIntent
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import kotlinx.android.synthetic.main.fragment_settings.accountView
import kotlinx.android.synthetic.main.fragment_settings.settingsAppVersion
import kotlinx.android.synthetic.main.fragment_settings.settingsAvatar
import kotlinx.android.synthetic.main.fragment_settings.settingsContainer
import kotlinx.android.synthetic.main.fragment_settings.settingsCurrency
import kotlinx.android.synthetic.main.fragment_settings.settingsEmail
import kotlinx.android.synthetic.main.fragment_settings.settingsGithub
import kotlinx.android.synthetic.main.fragment_settings.settingsLanguage
import kotlinx.android.synthetic.main.fragment_settings.settingsPin
import kotlinx.android.synthetic.main.fragment_settings.settingsPrivacy
import kotlinx.android.synthetic.main.fragment_settings.settingsRateUs
import kotlinx.android.synthetic.main.fragment_settings.settingsSafeMode
import kotlinx.android.synthetic.main.fragment_settings.settingsSafeModeContainer
import kotlinx.android.synthetic.main.fragment_settings.settingsTelegram
import kotlinx.android.synthetic.main.fragment_settings.settingsTerms
import kotlinx.android.synthetic.main.fragment_settings.settingsTwitter
import kotlinx.android.synthetic.main.fragment_settings.settingsWalletConnect
import kotlinx.android.synthetic.main.fragment_settings.settingsWallets
import kotlinx.android.synthetic.main.fragment_settings.settingsWebsite
import kotlinx.android.synthetic.main.fragment_settings.settingsYoutube

class SettingsFragment : BaseFragment<SettingsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun initViews() {
        settingsContainer.applyStatusBarInsets()

        accountView.setWholeClickListener { viewModel.accountActionsClicked() }

        settingsWallets.setOnClickListener { viewModel.walletsClicked() }

        settingsCurrency.setOnClickListener { viewModel.currenciesClicked() }
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

        settingsSafeModeContainer.setOnClickListener { viewModel.changeSafeMode() }
        settingsPin.setOnClickListener { viewModel.changePinCodeClicked() }

        settingsWalletConnect.setOnClickListener { viewModel.walletConnectClicked() }

        settingsAvatar.setOnClickListener { viewModel.selectedWalletClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<SettingsFeatureComponent>(
            requireContext(),
            SettingsFeatureApi::class.java
        )
            .settingsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SettingsViewModel) {
        setupSafeModeConfirmation(viewModel.safeModeAwaitableAction)
        observeBrowserEvents(viewModel)

        viewModel.selectedWalletModel.observe {
            settingsAvatar.setModel(it)

            accountView.setAccountIcon(it.walletIcon)
            accountView.setTitle(it.name)
        }

        viewModel.selectedCurrencyFlow.observe {
            settingsCurrency.setValue(it.code)
        }

        viewModel.selectedLanguageFlow.observe {
            settingsLanguage.setValue(it.displayName)
        }

        viewModel.safeModeStatus.observe {
            settingsSafeMode.isChecked = it
        }

        viewModel.appVersionFlow.observe(settingsAppVersion::setText)

        viewModel.openEmailEvent.observeEvent { requireContext().sendEmailIntent(it) }

        viewModel.walletConnectSessionsUi.observe(settingsWalletConnect::setValue)
    }
}
