package io.novafoundation.nova.feature_push_notifications.presentation.multisigs

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.common.view.dialog.infoDialog
import io.novafoundation.nova.common.view.settings.SettingsSwitcherView
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.databinding.FragmentPushMultisigSettingsBinding
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureComponent
import kotlinx.coroutines.flow.Flow

class PushMultisigSettingsFragment : BaseFragment<PushMultisigSettingsViewModel, FragmentPushMultisigSettingsBinding>() {

    companion object : PayloadCreator<PushMultisigSettingsRequester.Request> by FragmentPayloadCreator()

    override fun createBinding() = FragmentPushMultisigSettingsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.pushMultisigsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.pushMultisigSettingsSwitcher.setOnClickListener { viewModel.switchMultisigNotificationsState() }
        binder.pushMultisigInitiatingSwitcher.setOnClickListener { viewModel.switchInitialNotificationsState() }
        binder.pushMultisigApprovalSwitcher.setOnClickListener { viewModel.switchApprovingNotificationsState() }
        binder.pushMultisigExecutedSwitcher.setOnClickListener { viewModel.switchExecutionNotificationsState() }
        binder.pushMultisigRejectedSwitcher.setOnClickListener { viewModel.switchRejectionNotificationsState() }
    }

    override fun inject() {
        FeatureUtils.getFeature<PushNotificationsFeatureComponent>(requireContext(), PushNotificationsFeatureApi::class.java)
            .pushMultisigSettings()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: PushMultisigSettingsViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.isMultisigNotificationsEnabled.observe {
            binder.pushMultisigSettingsSwitcher.setChecked(it)

            binder.pushMultisigInitiatingSwitcher.isEnabled = it
            binder.pushMultisigApprovalSwitcher.isEnabled = it
            binder.pushMultisigExecutedSwitcher.isEnabled = it
            binder.pushMultisigRejectedSwitcher.isEnabled = it
        }

        binder.pushMultisigInitiatingSwitcher.bindWithFlow(viewModel.isInitiationEnabled)
        binder.pushMultisigApprovalSwitcher.bindWithFlow(viewModel.isApprovingEnabled)
        binder.pushMultisigExecutedSwitcher.bindWithFlow(viewModel.isExecutionEnabled)
        binder.pushMultisigRejectedSwitcher.bindWithFlow(viewModel.isRejectionEnabled)

        viewModel.noOneMultisigWalletSelectedEvent.observeEvent {
            infoDialog(requireContext()) {
                setTitle(R.string.no_ms_accounts_found_dialog_title)
                setMessage(R.string.no_ms_accounts_found_dialog_message)
                setNegativeButton(R.string.common_learn_more) { _, _ -> viewModel.learnMoreClicked() }
                setPositiveButton(R.string.common_got_it, null)
            }
        }
    }

    private fun SettingsSwitcherView.bindWithFlow(flow: Flow<Boolean>) {
        flow.observe { setChecked(it) }
    }
}
