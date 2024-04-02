package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.model.ParachainStakingUnbondConfirmPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond_confirm.parachainStakingUnbondConfirmAmount
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond_confirm.parachainStakingUnbondConfirmCollator
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond_confirm.parachainStakingUnbondConfirmConfirm
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond_confirm.parachainStakingUnbondConfirmContainer
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond_confirm.parachainStakingUnbondConfirmExtrinsicInfo
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond_confirm.parachainStakingUnbondConfirmHints
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond_confirm.parachainStakingUnbondConfirmToolbar

class ParachainStakingUnbondConfirmFragment : BaseFragment<ParachainStakingUnbondConfirmViewModel>() {

    companion object {

        private const val PAYLOAD = "ParachainStakingUnbondConfirmFragment.Payload"

        fun getBundle(payload: ParachainStakingUnbondConfirmPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_parachain_staking_unbond_confirm, container, false)
    }

    override fun initViews() {
        parachainStakingUnbondConfirmContainer.applyStatusBarInsets()

        parachainStakingUnbondConfirmToolbar.setHomeButtonListener { viewModel.backClicked() }

        parachainStakingUnbondConfirmExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        parachainStakingUnbondConfirmConfirm.prepareForProgress(viewLifecycleOwner)
        parachainStakingUnbondConfirmConfirm.setOnClickListener { viewModel.confirmClicked() }

        parachainStakingUnbondConfirmCollator.setOnClickListener { viewModel.collatorClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .parachainStakingUnbondConfirmFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ParachainStakingUnbondConfirmViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel, parachainStakingUnbondConfirmExtrinsicInfo.fee)
        observeHints(viewModel.hintsMixin, parachainStakingUnbondConfirmHints)

        viewModel.showNextProgress.observe(parachainStakingUnbondConfirmConfirm::setProgressState)

        viewModel.currentAccountModelFlow.observe(parachainStakingUnbondConfirmExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(parachainStakingUnbondConfirmExtrinsicInfo::setWallet)

        viewModel.collatorAddressModel.observe(parachainStakingUnbondConfirmCollator::showAddress)
        viewModel.amountModel.observe(parachainStakingUnbondConfirmAmount::setAmount)
    }
}
