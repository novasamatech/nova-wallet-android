package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm

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
import kotlinx.android.synthetic.main.fragment_nomination_pools_confirm_bond_more.nominationPoolsConfirmBondMoreAmount
import kotlinx.android.synthetic.main.fragment_nomination_pools_confirm_bond_more.nominationPoolsConfirmBondMoreConfirm
import kotlinx.android.synthetic.main.fragment_nomination_pools_confirm_bond_more.nominationPoolsConfirmBondMoreExtrinsicInformation
import kotlinx.android.synthetic.main.fragment_nomination_pools_confirm_bond_more.nominationPoolsConfirmBondMoreHints
import kotlinx.android.synthetic.main.fragment_nomination_pools_confirm_bond_more.nominationPoolsConfirmBondMoreToolbar

private const val PAYLOAD_KEY = "NominationPoolsConfirmBondMoreFragment.PAYLOAD_KEY"

class NominationPoolsConfirmBondMoreFragment : BaseFragment<NominationPoolsConfirmBondMoreViewModel>() {

    companion object {

        fun getBundle(payload: NominationPoolsConfirmBondMorePayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_nomination_pools_confirm_bond_more, container, false)
    }

    override fun initViews() {
        nominationPoolsConfirmBondMoreToolbar.applyStatusBarInsets()

        nominationPoolsConfirmBondMoreExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        nominationPoolsConfirmBondMoreToolbar.setHomeButtonListener { viewModel.backClicked() }
        nominationPoolsConfirmBondMoreConfirm.prepareForProgress(viewLifecycleOwner)
        nominationPoolsConfirmBondMoreConfirm.setOnClickListener { viewModel.confirmClicked() }
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
        observeHints(viewModel.hintsMixin, nominationPoolsConfirmBondMoreHints)

        viewModel.showNextProgress.observe(nominationPoolsConfirmBondMoreConfirm::setProgressState)

        viewModel.amountModelFlow.observe(nominationPoolsConfirmBondMoreAmount::setAmount)

        viewModel.feeStatusFlow.observe(nominationPoolsConfirmBondMoreExtrinsicInformation::setFeeStatus)
        viewModel.walletUiFlow.observe(nominationPoolsConfirmBondMoreExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(nominationPoolsConfirmBondMoreExtrinsicInformation::setAccount)
    }
}
