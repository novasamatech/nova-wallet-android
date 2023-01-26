package io.novafoundation.nova.feature_versions_impl.presentation.update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_versions_impl.R
import io.novafoundation.nova.feature_versions_impl.di.VersionsFeatureComponent
import io.novafoundation.nova.feature_versions_impl.presentation.update.adapters.UpdateNotificationsAdapter
import io.novafoundation.nova.feature_versions_impl.presentation.update.adapters.UpdateNotificationsBannerAdapter
import io.novafoundation.nova.feature_versions_impl.presentation.update.adapters.UpdateNotificationsSeeAllAdapter
import kotlinx.android.synthetic.main.fragment_update_notifications.updateNotificationsProgress
import kotlinx.android.synthetic.main.fragment_update_notifications.updatesApply
import kotlinx.android.synthetic.main.fragment_update_notifications.updatesList
import kotlinx.android.synthetic.main.fragment_update_notifications.updatesToolbar

class UpdateNotificationFragment : BaseFragment<UpdateNotificationViewModel>(), UpdateNotificationsSeeAllAdapter.SeeAllClickedListener {

    private val bannerAdapter = UpdateNotificationsBannerAdapter()
    private val listAdapter = UpdateNotificationsAdapter()
    private val seeAllAdapter = UpdateNotificationsSeeAllAdapter(this)
    private val adapter = ConcatAdapter(bannerAdapter, listAdapter, seeAllAdapter)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_update_notifications, container, false)
    }

    override fun initViews() {
        updatesToolbar.applyStatusBarInsets()
        updatesList.adapter = adapter
        val decoration = UpdateNotificationsItemDecoration(requireContext())
        updatesList.addItemDecoration(decoration)
        updatesToolbar.setRightActionClickListener { viewModel.skipClicked() }
        updatesApply.setOnClickListener { viewModel.installUpdateClicked() }
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
            updateNotificationsProgress.isVisible = it is LoadingState.Loading
            updatesList.isGone = it is LoadingState.Loading
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
