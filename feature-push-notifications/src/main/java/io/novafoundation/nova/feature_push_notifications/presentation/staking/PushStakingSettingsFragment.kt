package io.novafoundation.nova.feature_push_notifications.presentation.staking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.databinding.FragmentPushStakingSettingsBinding
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureComponent
import io.novafoundation.nova.feature_push_notifications.presentation.staking.adapter.PushStakingRVItem
import io.novafoundation.nova.feature_push_notifications.presentation.staking.adapter.PushStakingSettingsAdapter
import javax.inject.Inject

class PushStakingSettingsFragment : BaseFragment<PushStakingSettingsViewModel, FragmentPushStakingSettingsBinding>(), PushStakingSettingsAdapter.ItemHandler {

    companion object {
        private const val KEY_REQUEST = "KEY_REQUEST"

        fun getBundle(request: PushStakingSettingsRequester.Request): Bundle {
            return Bundle().apply {
                putParcelable(KEY_REQUEST, request)
            }
        }
    }

    override val binder by viewBinding(FragmentPushStakingSettingsBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        PushStakingSettingsAdapter(imageLoader, this)
    }

    override fun initViews() {
        binder.pushStakingToolbar.applyStatusBarInsets()
        binder.pushStakingToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.pushStakingToolbar.setRightActionClickListener { viewModel.clearClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.pushStakingList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<PushNotificationsFeatureComponent>(requireContext(), PushNotificationsFeatureApi::class.java)
            .pushStakingSettings()
            .create(this, argument(KEY_REQUEST))
            .inject(this)
    }

    override fun subscribe(viewModel: PushStakingSettingsViewModel) {
        viewModel.clearButtonEnabledFlow.observe {
            binder.pushStakingToolbar.setRightActionEnabled(it)
        }

        viewModel.stakingSettingsList.observe {
            binder.pushStakingList.isVisible = it is ExtendedLoadingState.Loaded
            binder.pushStakingProgress.isVisible = it is ExtendedLoadingState.Loading

            if (it is ExtendedLoadingState.Loaded) {
                adapter.submitList(it.data)
            }
        }
    }

    override fun itemClicked(item: PushStakingRVItem) {
        viewModel.itemClicked(item)
    }
}
