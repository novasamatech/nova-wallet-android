package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm

import android.os.Bundle

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentNominationPoolsConfirmBondMoreBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

private const val PAYLOAD_KEY = "NominationPoolsConfirmBondMoreFragment.PAYLOAD_KEY"

class NominationPoolsConfirmBondMoreFragment : BaseFragment<NominationPoolsConfirmBondMoreViewModel, FragmentNominationPoolsConfirmBondMoreBinding>() {

    companion object {

        fun getBundle(payload: NominationPoolsConfirmBondMorePayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun createBinding() = FragmentNominationPoolsConfirmBondMoreBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.nominationPoolsConfirmBondMoreExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.nominationPoolsConfirmBondMoreToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.nominationPoolsConfirmBondMoreConfirm.prepareForProgress(viewLifecycleOwner)
        binder.nominationPoolsConfirmBondMoreConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .nominationPoolsStakingConfirmBondMore()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: NominationPoolsConfirmBondMoreViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeHints(viewModel.hintsMixin, binder.nominationPoolsConfirmBondMoreHints)

        viewModel.showNextProgress.observe(binder.nominationPoolsConfirmBondMoreConfirm::setProgressState)

        viewModel.amountModelFlow.observe(binder.nominationPoolsConfirmBondMoreAmount::setAmount)

        viewModel.feeStatusFlow.observe(binder.nominationPoolsConfirmBondMoreExtrinsicInformation::setFeeStatus)
        viewModel.walletUiFlow.observe(binder.nominationPoolsConfirmBondMoreExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(binder.nominationPoolsConfirmBondMoreExtrinsicInformation::setAccount)
    }
}
