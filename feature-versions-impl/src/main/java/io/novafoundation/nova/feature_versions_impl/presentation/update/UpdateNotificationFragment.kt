package io.novafoundation.nova.feature_versions_impl.presentation.update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_versions_impl.R
import io.novafoundation.nova.feature_versions_impl.di.VersionsFeatureComponent

class UpdateNotificationFragment : BaseFragment<UpdateNotificationViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_update_notifications, container, false)
    }

    override fun initViews() {

    }

    override fun inject() {
        FeatureUtils.getFeature<VersionsFeatureComponent>(this, VersionsFeatureApi::class.java)
            .updateNotificationsFragmentComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: UpdateNotificationViewModel) {

    }
}
