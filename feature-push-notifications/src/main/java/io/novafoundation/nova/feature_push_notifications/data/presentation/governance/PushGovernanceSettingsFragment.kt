package io.novafoundation.nova.feature_push_notifications.data.presentation.governance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.observe
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.data.di.PushNotificationsFeatureComponent
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.adapter.PushGovernanceRVItem
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.adapter.PushGovernanceSettingsAdapter
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_push_governance_settings.pushGovernanceList
import kotlinx.android.synthetic.main.fragment_push_governance_settings.pushGovernanceProgress
import kotlinx.android.synthetic.main.fragment_push_governance_settings.pushGovernanceToolbar

class PushGovernanceSettingsFragment : BaseFragment<PushGovernanceSettingsViewModel>(), PushGovernanceSettingsAdapter.ItemHandler {

    companion object {
        private const val KEY_REQUEST = "KEY_REQUEST"

        fun getBundle(request: PushGovernanceSettingsRequester.Request): Bundle {
            return Bundle().apply {
                putParcelable(KEY_REQUEST, request)
            }
        }
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        PushGovernanceSettingsAdapter(imageLoader, this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_push_governance_settings, container, false)
    }

    override fun initViews() {
        pushGovernanceToolbar.applyStatusBarInsets()
        pushGovernanceToolbar.setHomeButtonListener { viewModel.backClicked() }
        pushGovernanceToolbar.setRightActionClickListener { viewModel.clearClicked() }
        onBackPressed { viewModel.backClicked() }

        pushGovernanceList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<PushNotificationsFeatureComponent>(requireContext(), PushNotificationsFeatureApi::class.java)
            .pushGovernanceSettings()
            .create(this, argument(KEY_REQUEST))
            .inject(this)
    }

    override fun subscribe(viewModel: PushGovernanceSettingsViewModel) {
        viewModel.clearButtonEnabledFlow.observe {
            pushGovernanceToolbar.setRightActionEnabled(it)
        }

        viewModel.governanceSettingsList.observe {
            pushGovernanceList.isVisible = it is ExtendedLoadingState.Loaded
            pushGovernanceToolbar.setRightActionEnabled(it is ExtendedLoadingState.Loaded)
            pushGovernanceProgress.isVisible = it is ExtendedLoadingState.Loading

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

    override fun delegateVotesClick(item: PushGovernanceRVItem) {
        viewModel.delegateVotesClicked(item)
    }

    override fun tracksClicked(item: PushGovernanceRVItem) {
        viewModel.tracksClicked(item)
    }
}
