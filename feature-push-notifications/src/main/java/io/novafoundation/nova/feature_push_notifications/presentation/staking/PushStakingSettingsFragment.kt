package io.novafoundation.nova.feature_push_notifications.presentation.staking

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
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureComponent
import io.novafoundation.nova.feature_push_notifications.presentation.staking.adapter.PushStakingRVItem
import io.novafoundation.nova.feature_push_notifications.presentation.staking.adapter.PushStakingSettingsAdapter
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_push_staking_settings.pushStakingList
import kotlinx.android.synthetic.main.fragment_push_staking_settings.pushStakingProgress
import kotlinx.android.synthetic.main.fragment_push_staking_settings.pushStakingToolbar

class PushStakingSettingsFragment : BaseFragment<PushStakingSettingsViewModel>(), PushStakingSettingsAdapter.ItemHandler {

    companion object {
        private const val KEY_REQUEST = "KEY_REQUEST"

        fun getBundle(request: PushStakingSettingsRequester.Request): Bundle {
            return Bundle().apply {
                putParcelable(KEY_REQUEST, request)
            }
        }
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        PushStakingSettingsAdapter(imageLoader, this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_push_staking_settings, container, false)
    }

    override fun initViews() {
        pushStakingToolbar.applyStatusBarInsets()
        pushStakingToolbar.setHomeButtonListener { viewModel.backClicked() }
        pushStakingToolbar.setRightActionClickListener { viewModel.clearClicked() }
        onBackPressed { viewModel.backClicked() }

        pushStakingList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<PushNotificationsFeatureComponent>(requireContext(), PushNotificationsFeatureApi::class.java)
            .pushStakingSettings()
            .create(this, argument(KEY_REQUEST))
            .inject(this)
    }

    override fun subscribe(viewModel: PushStakingSettingsViewModel) {
        viewModel.clearButtonEnabledFlow.observe {
            pushStakingToolbar.setRightActionEnabled(it)
        }

        viewModel.stakingSettingsList.observe {
            pushStakingList.isVisible = it is ExtendedLoadingState.Loaded
            pushStakingProgress.isVisible = it is ExtendedLoadingState.Loading

            if (it is ExtendedLoadingState.Loaded) {
                adapter.submitList(it.data)
            }
        }
    }

    override fun itemClicked(item: PushStakingRVItem) {
        viewModel.itemClicked(item)
    }
}
