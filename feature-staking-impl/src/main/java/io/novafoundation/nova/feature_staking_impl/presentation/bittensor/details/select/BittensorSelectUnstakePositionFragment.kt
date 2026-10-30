package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select

import android.os.Bundle
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorSelectUnstakePositionBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class BittensorSelectUnstakePositionFragment :
    BaseFragment<BittensorSelectUnstakePositionViewModel, FragmentBittensorSelectUnstakePositionBinding>() {

    companion object {
        private const val PAYLOAD_KEY = "payload"

        fun getBundle(request: BittensorSelectUnstakePositionCommunicator.Request) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, request)
        }
    }

    override fun createBinding() = FragmentBittensorSelectUnstakePositionBinding.inflate(layoutInflater)

    override fun inject() {
        val request = argument<BittensorSelectUnstakePositionCommunicator.Request>(PAYLOAD_KEY)
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorSelectUnstakePositionComponentFactory()
            .create(this, request)
            .inject(this)
    }

    override fun initViews() {
        binder.toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        binder.positions.adapter = BittensorUnstakePositionAdapter(viewModel.options, viewModel::select)
    }

    override fun subscribe(viewModel: BittensorSelectUnstakePositionViewModel) = Unit
}
