package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond

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
import io.novafoundation.nova.common.presentation.showLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.model.ParachainStakingRebondPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_parachain_staking_rebond.parachainStakingRebondAmount
import kotlinx.android.synthetic.main.fragment_parachain_staking_rebond.parachainStakingRebondCollator
import kotlinx.android.synthetic.main.fragment_parachain_staking_rebond.parachainStakingRebondConfirm
import kotlinx.android.synthetic.main.fragment_parachain_staking_rebond.parachainStakingRebondContainer
import kotlinx.android.synthetic.main.fragment_parachain_staking_rebond.parachainStakingRebondExtrinsicInfo
import kotlinx.android.synthetic.main.fragment_parachain_staking_rebond.parachainStakingRebondHints
import kotlinx.android.synthetic.main.fragment_parachain_staking_rebond.parachainStakingRebondToolbar

class ParachainStakingRebondFragment : BaseFragment<ParachainStakingRebondViewModel>() {

    companion object {

        private const val PAYLOAD = "ParachainStakingRebondFragment.Payload"

        fun getBundle(payload: ParachainStakingRebondPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_parachain_staking_rebond, container, false)
    }

    override fun initViews() {
        parachainStakingRebondContainer.applyStatusBarInsets()

        parachainStakingRebondToolbar.setHomeButtonListener { viewModel.backClicked() }

        parachainStakingRebondExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        parachainStakingRebondCollator.setOnClickListener { viewModel.collatorClicked() }

        parachainStakingRebondConfirm.prepareForProgress(viewLifecycleOwner)
        parachainStakingRebondConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .parachainStakingRebondFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ParachainStakingRebondViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel, parachainStakingRebondExtrinsicInfo.fee)
        observeHints(viewModel.hintsMixin, parachainStakingRebondHints)

        viewModel.showNextProgress.observe(parachainStakingRebondConfirm::setProgressState)

        viewModel.currentAccountModelFlow.observe(parachainStakingRebondExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(parachainStakingRebondExtrinsicInfo::setWallet)

        viewModel.collatorAddressModel.observe(parachainStakingRebondCollator::showAddress)

        viewModel.rebondAmount.observe(parachainStakingRebondAmount::showLoadingState)
    }
}
