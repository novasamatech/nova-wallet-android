package io.novafoundation.nova.feature_push_notifications.presentation.settings

import androidx.core.view.isVisible

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationDialog
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.databinding.FragmentPushSettingsBinding
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureComponent

class PushSettingsFragment : BaseFragment<PushSettingsViewModel, FragmentPushSettingsBinding>() {

    override fun createBinding() = FragmentPushSettingsBinding.inflate(layoutInflater)

    override fun initViews() {
        onBackPressed { viewModel.backClicked() }

        binder.pushSettingsToolbar.setRightActionClickListener { viewModel.saveClicked() }
        binder.pushSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.pushSettingsEnable.setOnClickListener { viewModel.enableSwitcherClicked() }
        binder.pushSettingsWallets.setOnClickListener { viewModel.walletsClicked() }
        binder.pushSettingsAnnouncements.setOnClickListener { viewModel.announementsClicked() }
        binder.pushSettingsSentTokens.setOnClickListener { viewModel.sentTokensClicked() }
        binder.pushSettingsReceivedTokens.setOnClickListener { viewModel.receivedTokensClicked() }
        binder.pushSettingsGovernance.setOnClickListener { viewModel.governanceClicked() }
        binder.pushSettingsStakingRewards.setOnClickListener { viewModel.stakingRewardsClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<PushNotificationsFeatureComponent>(requireContext(), PushNotificationsFeatureApi::class.java)
            .pushSettingsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: PushSettingsViewModel) {
        setupConfirmationDialog(R.style.AccentNegativeAlertDialogTheme_Reversed, viewModel.closeConfirmationAction)

        viewModel.pushSettingsWasChangedState.observe { binder.pushSettingsToolbar.setRightActionEnabled(it) }
        viewModel.savingInProgress.observe { binder.pushSettingsToolbar.showProgress(it) }

        viewModel.pushEnabledState.observe { enabled ->
            binder.pushSettingsEnable.setChecked(enabled)
            binder.pushSettingsAnnouncements.setEnabled(enabled)
            binder.pushSettingsSentTokens.setEnabled(enabled)
            binder.pushSettingsReceivedTokens.setEnabled(enabled)
            binder.pushSettingsGovernance.setEnabled(enabled)
            binder.pushSettingsStakingRewards.setEnabled(enabled)
        }

        viewModel.pushWalletsQuantity.observe { binder.pushSettingsWallets.setValue(it) }
        viewModel.showNoSelectedWalletsTip.observe { binder.pushSettingsNoSelectedWallets.isVisible = it }
        viewModel.pushAnnouncements.observe { binder.pushSettingsAnnouncements.setChecked(it) }
        viewModel.pushSentTokens.observe { binder.pushSettingsSentTokens.setChecked(it) }
        viewModel.pushReceivedTokens.observe { binder.pushSettingsReceivedTokens.setChecked(it) }
        viewModel.pushGovernanceState.observe { binder.pushSettingsGovernance.setValue(it) }
        viewModel.pushStakingRewardsState.observe { binder.pushSettingsStakingRewards.setValue(it) }
    }
}
