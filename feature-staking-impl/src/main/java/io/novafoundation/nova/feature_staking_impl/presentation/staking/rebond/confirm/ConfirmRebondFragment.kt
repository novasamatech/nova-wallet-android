package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import kotlinx.android.synthetic.main.fragment_confirm_rebond.confirmRebondAmount
import kotlinx.android.synthetic.main.fragment_confirm_rebond.confirmRebondConfirm
import kotlinx.android.synthetic.main.fragment_confirm_rebond.confirmRebondFee
import kotlinx.android.synthetic.main.fragment_confirm_rebond.confirmRebondOriginAccount
import kotlinx.android.synthetic.main.fragment_confirm_rebond.confirmRebondToolbar

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ConfirmRebondFragment : BaseFragment<ConfirmRebondViewModel>() {

    companion object {

        fun getBundle(payload: ConfirmRebondPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_confirm_rebond, container, false)
    }

    override fun initViews() {
        confirmRebondToolbar.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        confirmRebondOriginAccount.setWholeClickListener { viewModel.originAccountClicked() }

        confirmRebondToolbar.setHomeButtonListener { viewModel.backClicked() }
        confirmRebondConfirm.prepareForProgress(viewLifecycleOwner)
        confirmRebondConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        val payload = argument<ConfirmRebondPayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmRebondFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmRebondViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeRetries(viewModel)

        viewModel.showNextProgress.observe(confirmRebondConfirm::setProgress)

        viewModel.assetModelFlow.observe {
            confirmRebondAmount.setAssetBalance(it.assetBalance)
            confirmRebondAmount.setAssetName(it.tokenName)
            confirmRebondAmount.loadAssetImage(it.imageUrl)
        }

        confirmRebondAmount.amountInput.setText(viewModel.amount)

        viewModel.amountFiatFLow.observe {
            it.let(confirmRebondAmount::setAssetBalanceDollarAmount)
        }

        viewModel.feeLiveData.observe(confirmRebondFee::setFeeStatus)

        viewModel.originAddressModelLiveData.observe {
            confirmRebondOriginAccount.setMessage(it.nameOrAddress)
            confirmRebondOriginAccount.setTextIcon(it.image)
        }
    }
}
