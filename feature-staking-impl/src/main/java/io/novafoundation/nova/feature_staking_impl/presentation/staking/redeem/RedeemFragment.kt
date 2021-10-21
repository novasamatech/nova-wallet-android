package io.novafoundation.nova.feature_staking_impl.presentation.staking.redeem

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
import kotlinx.android.synthetic.main.fragment_redeem.redeemAmount
import kotlinx.android.synthetic.main.fragment_redeem.redeemConfirm
import kotlinx.android.synthetic.main.fragment_redeem.redeemContainer
import kotlinx.android.synthetic.main.fragment_redeem.redeemFee
import kotlinx.android.synthetic.main.fragment_redeem.redeemOriginAccount
import kotlinx.android.synthetic.main.fragment_redeem.redeemToolbar

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class RedeemFragment : BaseFragment<RedeemViewModel>() {

    companion object {

        fun getBundle(payload: RedeemPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_redeem, container, false)
    }

    override fun initViews() {
        redeemContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }

            consume(true)
        }

        redeemToolbar.setHomeButtonListener { viewModel.backClicked() }
        redeemConfirm.prepareForProgress(viewLifecycleOwner)
        redeemConfirm.setOnClickListener { viewModel.confirmClicked() }
        redeemOriginAccount.setWholeClickListener { viewModel.originAccountClicked() }
    }

    override fun inject() {
        val payload = argument<RedeemPayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .redeemFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: RedeemViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)

        viewModel.showNextProgress.observe(redeemConfirm::setProgress)

        viewModel.assetModelLiveData.observe {
            redeemAmount.setAssetBalance(it.assetBalance)
            redeemAmount.setAssetName(it.tokenName)
            redeemAmount.setAssetImageResource(it.tokenIconRes)
        }

        viewModel.amountLiveData.observe { (amount, fiatAmount) ->
            redeemAmount.amountInput.setText(amount)
            fiatAmount.let(redeemAmount::setAssetBalanceDollarAmount)
        }

        viewModel.originAddressModelLiveData.observe {
            redeemOriginAccount.setMessage(it.nameOrAddress)
            redeemOriginAccount.setTextIcon(it.image)
        }

        viewModel.feeLiveData.observe(redeemFee::setFeeStatus)
    }
}
