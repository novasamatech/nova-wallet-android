package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Bundle
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorSelectValidatorBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries

private const val PAYLOAD_KEY = "payload"

class BittensorSelectValidatorFragment : BaseFragment<BittensorSelectValidatorViewModel, FragmentBittensorSelectValidatorBinding>() {

    companion object {

        fun getBundle(payload: BittensorSelectValidatorCommunicator.Request) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    private val adapter by lazy { BittensorValidatorAdapter(viewModel::select, viewModel::infoClicked) }

    override fun createBinding() = FragmentBittensorSelectValidatorBinding.inflate(layoutInflater)

    override fun inject() {
        val payload = argument<BittensorSelectValidatorCommunicator.Request>(PAYLOAD_KEY)
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorSelectValidatorComponentFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun initViews() {
        binder.toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        binder.list.adapter = adapter
        binder.confirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun subscribe(viewModel: BittensorSelectValidatorViewModel) = with(binder) {
        observeBittensorRetries(viewModel.retryEvents)
        setupExternalActions(viewModel)
        viewModel.rows.observe(adapter::submitList)
        viewModel.selectedAddress.observe(adapter::select)
        viewModel.buttonState.observe(confirm::setState)
        viewModel.buttonLabel.observe { confirm.text = it }
        viewModel.isLoading.observe {
            progress.isVisible = it
        }
        viewModel.isEmpty.observe { empty.isVisible = it }
    }

    override fun onDestroyView() {
        binder.list.adapter = null
        super.onDestroyView()
    }
}
