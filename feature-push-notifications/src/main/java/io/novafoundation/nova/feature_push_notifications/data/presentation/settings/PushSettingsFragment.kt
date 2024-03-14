package io.novafoundation.nova.feature_push_notifications.data.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationDialog
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.data.di.PushNotificationsFeatureComponent
import kotlinx.android.synthetic.main.fragment_push_settings.pushSettingsAnnouncements
import kotlinx.android.synthetic.main.fragment_push_settings.pushSettingsEnable
import kotlinx.android.synthetic.main.fragment_push_settings.pushSettingsGovernance
import kotlinx.android.synthetic.main.fragment_push_settings.pushSettingsReceivedTokens
import kotlinx.android.synthetic.main.fragment_push_settings.pushSettingsSentTokens
import kotlinx.android.synthetic.main.fragment_push_settings.pushSettingsStakingRewards
import kotlinx.android.synthetic.main.fragment_push_settings.pushSettingsToolbar
import kotlinx.android.synthetic.main.fragment_push_settings.pushSettingsWallets

class PushSettingsFragment : BaseFragment<PushSettingsViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_push_settings, container, false)
    }

    override fun initViews() {
        onBackPressed { viewModel.backClicked() }

        pushSettingsToolbar.applyStatusBarInsets()
        pushSettingsToolbar.setRightActionClickListener { viewModel.saveClicked() }
        pushSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }

        pushSettingsEnable.setOnClickListener { viewModel.enableSwitcherClicked() }
        pushSettingsWallets.setOnClickListener { viewModel.walletsClicked() }
        pushSettingsAnnouncements.setOnClickListener { viewModel.announementsClicked() }
        pushSettingsSentTokens.setOnClickListener { viewModel.sentTokensClicked() }
        pushSettingsReceivedTokens.setOnClickListener { viewModel.receivedTokensClicked() }
        pushSettingsGovernance.setOnClickListener { viewModel.governanceClicked() }
        pushSettingsStakingRewards.setOnClickListener { viewModel.stakingRewardsClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<PushNotificationsFeatureComponent>(requireContext(), PushNotificationsFeatureApi::class.java)
            .pushSettingsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: PushSettingsViewModel) {
        setupConfirmationDialog(R.style.AccentNegativeAlertDialogTheme_Reversed, viewModel.closeConfirmationAction)

        viewModel.pushSettingsWasChangedState.observe { pushSettingsToolbar.setRightActionEnabled(it) }
        viewModel.savingInProgress.observe { pushSettingsToolbar.showProgress(it) }

        viewModel.pushEnabledState.observe { enabled ->
            pushSettingsEnable.setChecked(enabled)
            pushSettingsAnnouncements.setEnabled(enabled)
            pushSettingsSentTokens.setEnabled(enabled)
            pushSettingsReceivedTokens.setEnabled(enabled)
            pushSettingsGovernance.setEnabled(enabled)
            pushSettingsStakingRewards.setEnabled(enabled)
        }

        viewModel.pushWalletsQuantity.observe { pushSettingsWallets.setValue(it) }
        viewModel.pushAnnouncements.observe { pushSettingsAnnouncements.setChecked(it) }
        viewModel.pushSentTokens.observe { pushSettingsSentTokens.setChecked(it) }
        viewModel.pushReceivedTokens.observe { pushSettingsReceivedTokens.setChecked(it) }
        viewModel.pushGovernanceState.observe { pushSettingsGovernance.setValue(it) }
        viewModel.pushStakingRewardsState.observe { pushSettingsStakingRewards.setValue(it) }
    }
}
