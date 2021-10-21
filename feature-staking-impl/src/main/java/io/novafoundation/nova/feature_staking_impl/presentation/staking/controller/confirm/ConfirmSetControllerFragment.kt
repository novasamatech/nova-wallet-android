package io.novafoundation.nova.feature_staking_impl.presentation.staking.controller.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import kotlinx.android.synthetic.main.fragment_confirm_set_controller.confirmSetControllerConfirm
import kotlinx.android.synthetic.main.fragment_confirm_set_controller.confirmSetControllerDestinationAccount
import kotlinx.android.synthetic.main.fragment_confirm_set_controller.confirmSetControllerFee
import kotlinx.android.synthetic.main.fragment_confirm_set_controller.confirmSetControllerStashAccount
import kotlinx.android.synthetic.main.fragment_confirm_set_controller.confirmSetControllerToolbar

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ConfirmSetControllerFragment : BaseFragment<ConfirmSetControllerViewModel>() {
    companion object {
        fun getBundle(payload: ConfirmSetControllerPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_confirm_set_controller, container, false)
    }

    override fun initViews() {
        confirmSetControllerToolbar.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        confirmSetControllerToolbar.setHomeButtonListener { viewModel.back() }

        confirmSetControllerConfirm.setOnClickListener { viewModel.confirmClicked() }

        confirmSetControllerStashAccount.setWholeClickListener { viewModel.openStashExternalActions() }
        confirmSetControllerDestinationAccount.setWholeClickListener { viewModel.openControllerExternalActions() }
    }

    override fun inject() {
        val payload = argument<ConfirmSetControllerPayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmSetControllerFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmSetControllerViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)

        viewModel.feeStatusLiveData.observe(confirmSetControllerFee::setFeeStatus)

        viewModel.stashAddressLiveData.observe {
            confirmSetControllerStashAccount.setTextIcon(it.image)
            confirmSetControllerStashAccount.setMessage(it.nameOrAddress)
        }

        viewModel.controllerAddressLiveData.observe {
            confirmSetControllerDestinationAccount.setTextIcon(it.image)
            confirmSetControllerDestinationAccount.setMessage(it.nameOrAddress)
        }
    }
}
