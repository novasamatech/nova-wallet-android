package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import kotlinx.android.synthetic.main.fragment_confirm_bond_more.confirmBondMoreAccount
import kotlinx.android.synthetic.main.fragment_confirm_bond_more.confirmBondMoreAmount
import kotlinx.android.synthetic.main.fragment_confirm_bond_more.confirmBondMoreConfirm
import kotlinx.android.synthetic.main.fragment_confirm_bond_more.confirmBondMoreFee
import kotlinx.android.synthetic.main.fragment_confirm_bond_more.confirmBondMoreHints
import kotlinx.android.synthetic.main.fragment_confirm_bond_more.confirmBondMoreToolbar
import kotlinx.android.synthetic.main.fragment_confirm_bond_more.confirmBondMoreWallet

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

        confirmBondMoreAccount.setOnClickListener { viewModel.originAccountClicked() }

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
        observeHints(viewModel.hintsMixin, confirmBondMoreHints)

        viewModel.showNextProgress.observe(confirmBondMoreConfirm::setProgress)

        viewModel.amountModelFlow.observe(confirmBondMoreAmount::setAmount)
        viewModel.feeStatusFlow.observe(confirmBondMoreFee::setFeeStatus)

        viewModel.walletUiFlow.observe(confirmBondMoreWallet::showWallet)

        viewModel.originAddressModelFlow.observe(confirmBondMoreAccount::showAddress)
    }
}
