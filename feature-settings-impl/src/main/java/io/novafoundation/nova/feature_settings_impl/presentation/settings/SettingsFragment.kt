package io.novafoundation.nova.feature_settings_impl.presentation.settings

import android.content.Intent
import android.provider.Settings
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
import io.novafoundation.nova.feature_settings_impl.databinding.FragmentSettingsBinding
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent

class SettingsFragment : BaseFragment<SettingsViewModel, FragmentSettingsBinding>() {

    override fun createBinding() = FragmentSettingsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.settingsContainer.applyStatusBarInsets()

        binder.accountView.setWholeClickListener { viewModel.accountActionsClicked() }

        binder.settingsWallets.setOnClickListener { viewModel.walletsClicked() }
        binder.settingsNetworks.setOnClickListener { viewModel.networksClicked() }
        binder.settingsPushNotifications.setOnClickListener { viewModel.pushNotificationsClicked() }
        binder.settingsCurrency.setOnClickListener { viewModel.currenciesClicked() }
        binder.settingsLanguage.setOnClickListener { viewModel.languagesClicked() }

        binder.settingsTelegram.setOnClickListener { viewModel.telegramClicked() }
        binder.settingsTwitter.setOnClickListener { viewModel.twitterClicked() }
        binder.settingsYoutube.setOnClickListener { viewModel.openYoutube() }

        binder.settingsWebsite.setOnClickListener { viewModel.websiteClicked() }
        binder.settingsGithub.setOnClickListener { viewModel.githubClicked() }
        binder.settingsTerms.setOnClickListener { viewModel.termsClicked() }
        binder.settingsPrivacy.setOnClickListener { viewModel.privacyClicked() }

        binder.settingsRateUs.setOnClickListener { viewModel.rateUsClicked() }
        binder.settingsWiki.setOnClickListener { viewModel.wikiClicked() }
        binder.settingsEmail.setOnClickListener { viewModel.emailClicked() }

        binder.settingsBiometricAuth.setOnClickListener { viewModel.changeBiometricAuth() }
        binder.settingsPinCodeVerification.setOnClickListener { viewModel.changePincodeVerification() }
        binder.settingsSafeMode.setOnClickListener { viewModel.changeSafeMode() }
        binder.settingsPin.setOnClickListener { viewModel.changePinCodeClicked() }

        binder.settingsCloudBackup.setOnClickListener { viewModel.cloudBackupClicked() }

        binder.settingsWalletConnect.setOnClickListener { viewModel.walletConnectClicked() }

        binder.settingsAvatar.setOnClickListener { viewModel.selectedWalletClicked() }
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
            binder.settingsAvatar.setModel(it)

            binder.accountView.setAccountIcon(it.walletIcon)
            binder.accountView.setTitle(it.name)
        }

        viewModel.pushNotificationsState.observe {
            binder.settingsPushNotifications.setValue(it)
        }

        viewModel.selectedCurrencyFlow.observe {
            binder.settingsCurrency.setValue(it.code)
        }

        viewModel.selectedLanguageFlow.observe {
            binder.settingsLanguage.setValue(it.displayName)
        }

        viewModel.showBiometricNotReadyDialogEvent.observeEvent {
            showBiometricNotReadyDialog()
        }

        viewModel.biometricAuthStatus.observe {
            binder.settingsBiometricAuth.setChecked(it)
        }

        viewModel.biometricEventMessages.observe {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }

        viewModel.pinCodeVerificationStatus.observe {
            binder.settingsPinCodeVerification.setChecked(it)
        }

        viewModel.safeModeStatus.observe {
            binder.settingsSafeMode.setChecked(it)
        }

        viewModel.appVersionFlow.observe(binder.settingsAppVersion::setText)

        viewModel.openEmailEvent.observeEvent { requireContext().sendEmailIntent(it) }

        viewModel.walletConnectSessionsUi.observe(binder.settingsWalletConnect::setValue)
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
