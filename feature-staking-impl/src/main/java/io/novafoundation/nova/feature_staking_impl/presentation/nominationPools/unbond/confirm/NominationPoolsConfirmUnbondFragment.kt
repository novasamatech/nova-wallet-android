package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentNominationPoolsConfirmUnbondBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

private const val PAYLOAD_KEY = "NominationPoolsConfirmUnbondFragment.PAYLOAD_KEY"

class NominationPoolsConfirmUnbondFragment : BaseFragment<NominationPoolsConfirmUnbondViewModel, FragmentNominationPoolsConfirmUnbondBinding>() {

    companion object {

        fun getBundle(payload: NominationPoolsConfirmUnbondPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override val binder by viewBinding(FragmentNominationPoolsConfirmUnbondBinding::bind)

    override fun initViews() {
        binder.nominationPoolsConfirmUnbondToolbar.applyStatusBarInsets()

        binder.nominationPoolsConfirmUnbondExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.nominationPoolsConfirmUnbondToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.nominationPoolsConfirmUnbondConfirm.prepareForProgress(viewLifecycleOwner)
        binder.nominationPoolsConfirmUnbondConfirm.setOnClickListener { viewModel.confirmClicked() }
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
        observeHints(viewModel.hintsMixin, binder.nominationPoolsConfirmUnbondHints)

        viewModel.showNextProgress.observe(binder.nominationPoolsConfirmUnbondConfirm::setProgressState)

        viewModel.amountModelFlow.observe(binder.nominationPoolsConfirmUnbondAmount::setAmount)

        viewModel.feeStatusFlow.observe(binder.nominationPoolsConfirmUnbondExtrinsicInformation::setFeeStatus)
        viewModel.walletUiFlow.observe(binder.nominationPoolsConfirmUnbondExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(binder.nominationPoolsConfirmUnbondExtrinsicInformation::setAccount)
    }
}
