package io.novafoundation.nova.feature_versions_impl.presentation.update

import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_versions_impl.databinding.FragmentUpdateNotificationsBinding
import io.novafoundation.nova.feature_versions_impl.di.VersionsFeatureComponent
import io.novafoundation.nova.feature_versions_impl.presentation.update.adapters.UpdateNotificationsAdapter
import io.novafoundation.nova.feature_versions_impl.presentation.update.adapters.UpdateNotificationsBannerAdapter
import io.novafoundation.nova.feature_versions_impl.presentation.update.adapters.UpdateNotificationsSeeAllAdapter

class UpdateNotificationFragment : BaseFragment<UpdateNotificationViewModel, FragmentUpdateNotificationsBinding>(), UpdateNotificationsSeeAllAdapter.SeeAllClickedListener {

    override val binder by viewBinding(FragmentUpdateNotificationsBinding::bind)

    private val bannerAdapter = UpdateNotificationsBannerAdapter()
    private val listAdapter = UpdateNotificationsAdapter()
    private val seeAllAdapter = UpdateNotificationsSeeAllAdapter(this)
    private val adapter = ConcatAdapter(bannerAdapter, listAdapter, seeAllAdapter)

    override fun initViews() {
        binder.updatesToolbar.applyStatusBarInsets()
        binder.updatesList.adapter = adapter
        val decoration = UpdateNotificationsItemDecoration(requireContext())
        binder.updatesList.addItemDecoration(decoration)
        binder.updatesToolbar.setRightActionClickListener { viewModel.skipClicked() }
        binder.updatesApply.setOnClickListener { viewModel.installUpdateClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<VersionsFeatureComponent>(this, VersionsFeatureApi::class.java)
            .updateNotificationsFragmentComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: UpdateNotificationViewModel) {
        viewModel.bannerModel.observe {
            bannerAdapter.setModel(it)
        }

        viewModel.notificationModels.observe {
            binder.updateNotificationsProgress.isVisible = it is LoadingState.Loading
            binder.updatesList.isGone = it is LoadingState.Loading
            if (it is LoadingState.Loaded) {
                listAdapter.submitList(it.data)
            }
        }

        viewModel.seeAllButtonVisible.observe {
            seeAllAdapter.showButton(it)
        }
    }

    override fun onSeeAllClicked() {
        viewModel.showAllNotifications()
    }
}
