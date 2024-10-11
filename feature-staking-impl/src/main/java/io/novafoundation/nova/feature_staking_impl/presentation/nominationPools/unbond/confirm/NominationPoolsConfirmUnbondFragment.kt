package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

private const val PAYLOAD_KEY = "NominationPoolsConfirmUnbondFragment.PAYLOAD_KEY"

class NominationPoolsConfirmUnbondFragment : BaseFragment<NominationPoolsConfirmUnbondViewModel>() {

    companion object {

        fun getBundle(payload: NominationPoolsConfirmUnbondPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_nomination_pools_confirm_unbond, container, false)
    }

    override fun initViews() {
        nominationPoolsConfirmUnbondToolbar.applyStatusBarInsets()

        nominationPoolsConfirmUnbondExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        nominationPoolsConfirmUnbondToolbar.setHomeButtonListener { viewModel.backClicked() }
        nominationPoolsConfirmUnbondConfirm.prepareForProgress(viewLifecycleOwner)
        nominationPoolsConfirmUnbondConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .nominationPoolsStakingConfirmUnbond()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: NominationPoolsConfirmUnbondViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeHints(viewModel.hintsMixin, nominationPoolsConfirmUnbondHints)

        viewModel.showNextProgress.observe(nominationPoolsConfirmUnbondConfirm::setProgressState)

        viewModel.amountModelFlow.observe(nominationPoolsConfirmUnbondAmount::setAmount)

        viewModel.feeStatusFlow.observe(nominationPoolsConfirmUnbondExtrinsicInformation::setFeeStatus)
        viewModel.walletUiFlow.observe(nominationPoolsConfirmUnbondExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(nominationPoolsConfirmUnbondExtrinsicInformation::setAccount)
    }
}
