package io.novafoundation.nova.feature_settings_impl.presentation.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationDialog
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.sendEmailIntent
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import kotlinx.android.synthetic.main.fragment_settings.accountView
import kotlinx.android.synthetic.main.fragment_settings.settingsAppVersion
import kotlinx.android.synthetic.main.fragment_settings.settingsAvatar
import kotlinx.android.synthetic.main.fragment_settings.settingsBiometricAuth
import kotlinx.android.synthetic.main.fragment_settings.settingsCard
import kotlinx.android.synthetic.main.fragment_settings.settingsCloudBackup
import kotlinx.android.synthetic.main.fragment_settings.settingsContainer
import kotlinx.android.synthetic.main.fragment_settings.settingsCurrency
import kotlinx.android.synthetic.main.fragment_settings.settingsEmail
import kotlinx.android.synthetic.main.fragment_settings.settingsGithub
import kotlinx.android.synthetic.main.fragment_settings.settingsLanguage
import kotlinx.android.synthetic.main.fragment_settings.settingsNetworks
import kotlinx.android.synthetic.main.fragment_settings.settingsPin
import kotlinx.android.synthetic.main.fragment_settings.settingsPinCodeVerification
import kotlinx.android.synthetic.main.fragment_settings.settingsPrivacy
import kotlinx.android.synthetic.main.fragment_settings.settingsPushNotifications
import kotlinx.android.synthetic.main.fragment_settings.settingsRateUs
import kotlinx.android.synthetic.main.fragment_settings.settingsSafeMode
import kotlinx.android.synthetic.main.fragment_settings.settingsTelegram
import kotlinx.android.synthetic.main.fragment_settings.settingsTerms
import kotlinx.android.synthetic.main.fragment_settings.settingsTwitter
import kotlinx.android.synthetic.main.fragment_settings.settingsWalletConnect
import kotlinx.android.synthetic.main.fragment_settings.settingsWallets
import kotlinx.android.synthetic.main.fragment_settings.settingsWebsite
import kotlinx.android.synthetic.main.fragment_settings.settingsWiki
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

        settingsCard.setOnClickListener { viewModel.novaCardClicked() }

        settingsWallets.setOnClickListener { viewModel.walletsClicked() }
        settingsNetworks.setOnClickListener { viewModel.networksClicked() }
        settingsPushNotifications.setOnClickListener { viewModel.pushNotificationsClicked() }
        settingsCurrency.setOnClickListener { viewModel.currenciesClicked() }
        settingsLanguage.setOnClickListener { viewModel.languagesClicked() }

        settingsTelegram.setOnClickListener { viewModel.telegramClicked() }
        settingsTwitter.setOnClickListener { viewModel.twitterClicked() }
        settingsYoutube.setOnClickListener { viewModel.openYoutube() }

        settingsWebsite.setOnClickListener { viewModel.websiteClicked() }
        settingsGithub.setOnClickListener { viewModel.githubClicked() }
        settingsTerms.setOnClickListener { viewModel.termsClicked() }
        settingsPrivacy.setOnClickListener { viewModel.privacyClicked() }

        settingsRateUs.setOnClickListener { viewModel.rateUsClicked() }
        settingsWiki.setOnClickListener { viewModel.wikiClicked() }
        settingsEmail.setOnClickListener { viewModel.emailClicked() }

        settingsBiometricAuth.setOnClickListener { viewModel.changeBiometricAuth() }
        settingsPinCodeVerification.setOnClickListener { viewModel.changePincodeVerification() }
        settingsSafeMode.setOnClickListener { viewModel.changeSafeMode() }
        settingsPin.setOnClickListener { viewModel.changePinCodeClicked() }

        settingsCloudBackup.setOnClickListener { viewModel.cloudBackupClicked() }

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
        setupConfirmationDialog(R.style.AccentAlertDialogTheme, viewModel.confirmationAwaitableAction)
        observeBrowserEvents(viewModel)

        viewModel.selectedWalletModel.observe {
            settingsAvatar.setModel(it)

            accountView.setAccountIcon(it.walletIcon)
            accountView.setTitle(it.name)
        }

        viewModel.pushNotificationsState.observe {
            settingsPushNotifications.setValue(it)
        }

        viewModel.selectedCurrencyFlow.observe {
            settingsCurrency.setValue(it.code)
        }

        viewModel.selectedLanguageFlow.observe {
            settingsLanguage.setValue(it.displayName)
        }

        viewModel.showBiometricNotReadyDialogEvent.observeEvent {
            showBiometricNotReadyDialog()
        }

        viewModel.biometricAuthStatus.observe {
            settingsBiometricAuth.setChecked(it)
        }

        viewModel.biometricEventMessages.observe {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }

        viewModel.pinCodeVerificationStatus.observe {
            settingsPinCodeVerification.setChecked(it)
        }

        viewModel.safeModeStatus.observe {
            settingsSafeMode.setChecked(it)
        }

        viewModel.appVersionFlow.observe(settingsAppVersion::setText)

        viewModel.openEmailEvent.observeEvent { requireContext().sendEmailIntent(it) }

        viewModel.walletConnectSessionsUi.observe(settingsWalletConnect::setValue)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun showBiometricNotReadyDialog() {
        dialog(requireContext(), customStyle = R.style.AccentAlertDialogTheme) {
            setTitle(R.string.settings_biometric_not_ready_title)
            setMessage(R.string.settings_biometric_not_ready_message)
            setNegativeButton(R.string.common_cancel, null)
            setPositiveButton(getString(R.string.common_settings)) { _, _ ->
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }
}
