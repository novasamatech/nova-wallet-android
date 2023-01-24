package io.novafoundation.nova.feature_versions_impl.presentation.update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.navigation.DelayedNavigation
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.getTopSystemBarInset
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_versions_impl.R
import io.novafoundation.nova.feature_versions_impl.di.VersionsFeatureComponent
import kotlinx.android.synthetic.main.fragment_update_notifications.updateNotificationsProgress
import kotlinx.android.synthetic.main.fragment_update_notifications.updatesApply
import kotlinx.android.synthetic.main.fragment_update_notifications.updatesList
import kotlinx.android.synthetic.main.fragment_update_notifications.updatesToolbar

class UpdateNotificationFragment : BaseFragment<UpdateNotificationViewModel>(), UpdateNotificationsAdapter.SeeAllClickedListener {

    private val adapter = UpdateNotificationsAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_update_notifications, container, false)
    }

    override fun initViews() {
        onBackPressed { viewModel.skipClicked() }
        updatesToolbar.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(0, insets.getTopSystemBarInset(), 0, 0)
            insets
        }

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
        viewModel.notificationModels.observe {
            updateNotificationsProgress.isVisible = it is LoadingState.Loading
            updatesList.isGone = it is LoadingState.Loading
            if (it is LoadingState.Loaded) {
                adapter.submitList(it.data)
            }
        }
    }

    override fun onSeeAllClicked() {
        viewModel.showAllNotifications()
    }
}
