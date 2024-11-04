package io.novafoundation.nova.feature_push_notifications.presentation.governance

import android.os.Bundle
import androidx.core.view.isVisible
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.observe
import io.novafoundation.nova.feature_push_notifications.databinding.FragmentPushGovernanceSettingsBinding
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureComponent
import io.novafoundation.nova.feature_push_notifications.presentation.governance.adapter.PushGovernanceRVItem
import io.novafoundation.nova.feature_push_notifications.presentation.governance.adapter.PushGovernanceSettingsAdapter
import javax.inject.Inject

class PushGovernanceSettingsFragment : BaseFragment<PushGovernanceSettingsViewModel, FragmentPushGovernanceSettingsBinding>(), PushGovernanceSettingsAdapter.ItemHandler {

    companion object {
        private const val KEY_REQUEST = "KEY_REQUEST"

        fun getBundle(request: PushGovernanceSettingsRequester.Request): Bundle {
            return Bundle().apply {
                putParcelable(KEY_REQUEST, request)
            }
        }
    }

    override val binder by viewBinding(FragmentPushGovernanceSettingsBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        PushGovernanceSettingsAdapter(imageLoader, this)
    }

    override fun initViews() {
        binder.pushGovernanceToolbar.applyStatusBarInsets()
        binder.pushGovernanceToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.pushGovernanceToolbar.setRightActionClickListener { viewModel.clearClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.pushGovernanceList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<PushNotificationsFeatureComponent>(requireContext(), PushNotificationsFeatureApi::class.java)
            .pushGovernanceSettings()
            .create(this, argument(KEY_REQUEST))
            .inject(this)
    }

    override fun subscribe(viewModel: PushGovernanceSettingsViewModel) {
        viewModel.clearButtonEnabledFlow.observe {
            binder.pushGovernanceToolbar.setRightActionEnabled(it)
        }

        viewModel.governanceSettingsList.observe {
            binder.pushGovernanceList.isVisible = it is ExtendedLoadingState.Loaded
            binder.pushGovernanceProgress.isVisible = it is ExtendedLoadingState.Loading

            if (it is ExtendedLoadingState.Loaded) {
                adapter.submitList(it.data)
            }
        }
    }

    override fun enableSwitcherClick(item: PushGovernanceRVItem) {
        viewModel.enableSwitcherClicked(item)
    }

    override fun newReferendaClick(item: PushGovernanceRVItem) {
        viewModel.newReferendaClicked(item)
    }

    override fun referendaUpdatesClick(item: PushGovernanceRVItem) {
        viewModel.referendaUpdatesClicked(item)
    }

    override fun tracksClicked(item: PushGovernanceRVItem) {
        viewModel.tracksClicked(item)
    }
}
