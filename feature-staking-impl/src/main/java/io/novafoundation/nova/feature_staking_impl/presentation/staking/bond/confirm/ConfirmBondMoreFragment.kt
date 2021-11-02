package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm

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
import kotlinx.android.synthetic.main.fragment_confirm_bond_more.confirmBondMoreAmount
import kotlinx.android.synthetic.main.fragment_confirm_bond_more.confirmBondMoreConfirm
import kotlinx.android.synthetic.main.fragment_confirm_bond_more.confirmBondMoreFee
import kotlinx.android.synthetic.main.fragment_confirm_bond_more.confirmBondMoreOriginAccount
import kotlinx.android.synthetic.main.fragment_confirm_bond_more.confirmBondMoreToolbar

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ConfirmBondMoreFragment : BaseFragment<ConfirmBondMoreViewModel>() {

    companion object {

        fun getBundle(payload: ConfirmBondMorePayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_confirm_bond_more, container, false)
    }

    override fun initViews() {
        confirmBondMoreToolbar.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        confirmBondMoreOriginAccount.setWholeClickListener { viewModel.originAccountClicked() }

        confirmBondMoreToolbar.setHomeButtonListener { viewModel.backClicked() }
        confirmBondMoreConfirm.prepareForProgress(viewLifecycleOwner)
        confirmBondMoreConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        val payload = argument<ConfirmBondMorePayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmBondMoreFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmBondMoreViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)

        viewModel.showNextProgress.observe(confirmBondMoreConfirm::setProgress)

        viewModel.assetModelFlow.observe {
            confirmBondMoreAmount.setAssetBalance(it.assetBalance)
            confirmBondMoreAmount.setAssetName(it.tokenName)
            confirmBondMoreAmount.loadAssetImage(it.imageUrl)
        }

        confirmBondMoreAmount.amountInput.setText(viewModel.amount)

        viewModel.amountFiatFLow.observe {
            it.let(confirmBondMoreAmount::setAssetBalanceDollarAmount)
        }

        viewModel.feeStatusLiveData.observe(confirmBondMoreFee::setFeeStatus)

        viewModel.originAddressModelLiveData.observe {
            confirmBondMoreOriginAccount.setMessage(it.nameOrAddress)
            confirmBondMoreOriginAccount.setTextIcon(it.image)
        }
    }
}
