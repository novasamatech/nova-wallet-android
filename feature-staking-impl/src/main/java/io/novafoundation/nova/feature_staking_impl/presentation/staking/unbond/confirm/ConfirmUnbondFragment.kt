package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import kotlinx.android.synthetic.main.fragment_confirm_unbond.confirmUnbondAmount
import kotlinx.android.synthetic.main.fragment_confirm_unbond.confirmUnbondConfirm
import kotlinx.android.synthetic.main.fragment_confirm_unbond.confirmUnbondFee
import kotlinx.android.synthetic.main.fragment_confirm_unbond.confirmUnbondOriginAccount
import kotlinx.android.synthetic.main.fragment_confirm_unbond.confirmUnbondToolbar

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ConfirmUnbondFragment : BaseFragment<ConfirmUnbondViewModel>() {

    companion object {

        fun getBundle(payload: ConfirmUnbondPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_confirm_unbond, container, false)
    }

    override fun initViews() {
        confirmUnbondToolbar.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        confirmUnbondOriginAccount.setWholeClickListener { viewModel.originAccountClicked() }

        confirmUnbondToolbar.setHomeButtonListener { viewModel.backClicked() }
        confirmUnbondConfirm.prepareForProgress(viewLifecycleOwner)
        confirmUnbondConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        val payload = argument<ConfirmUnbondPayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmUnbondFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmUnbondViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)

        viewModel.showNextProgress.observe(confirmUnbondConfirm::setProgress)

        viewModel.assetModelFlow.observe {
            confirmUnbondAmount.setAssetBalance(it.assetBalance)
            confirmUnbondAmount.setAssetName(it.tokenName)
            confirmUnbondAmount.setAssetImageResource(it.tokenIconRes)
        }

        confirmUnbondAmount.amountInput.setText(viewModel.amount)

        viewModel.amountFiatFLow.observe {
            it.let(confirmUnbondAmount::setAssetBalanceDollarAmount)
        }

        viewModel.feeStatusLiveData.observe(confirmUnbondFee::setFeeStatus)

        viewModel.originAddressModelLiveData.observe {
            confirmUnbondOriginAccount.setMessage(it.nameOrAddress)
            confirmUnbondOriginAccount.setTextIcon(it.image)
        }
    }
}
