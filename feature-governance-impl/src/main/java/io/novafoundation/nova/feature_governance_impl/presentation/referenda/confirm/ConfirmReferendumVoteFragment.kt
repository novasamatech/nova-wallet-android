package io.novafoundation.nova.feature_governance_impl.presentation.referenda.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_referendum_confirm_vote.confirmReferendumVoteConfirm
import kotlinx.android.synthetic.main.fragment_referendum_confirm_vote.confirmReferendumVoteInformation
import kotlinx.android.synthetic.main.fragment_referendum_confirm_vote.confirmReferendumVoteNote
import kotlinx.android.synthetic.main.fragment_referendum_confirm_vote.confirmReferendumVoteToolbar

class ConfirmReferendumVoteFragment : BaseFragment<ConfirmReferendumVoteViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_referendum_confirm_vote, container, false)
    }

    override fun initViews() {
        confirmReferendumVoteNote.setDrawableStart(R.drawable.ic_star_filled, widthInDp = 16, paddingInDp = 8, tint = R.color.white_48)
        confirmReferendumVoteToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        confirmReferendumVoteConfirm.prepareForProgress(viewLifecycleOwner)
        confirmReferendumVoteConfirm.setOnClickListener { viewModel.confirmClicked() }

        confirmReferendumVoteInformation.setOnAccountClickedListener { viewModel.accountClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .confirmReferendumVote()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmReferendumVoteViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        // TODO observeHints(viewModel.hintsMixin, confirmReferendumVoteHints)

        setupFeeLoading(viewModel, confirmReferendumVoteInformation.fee)

        viewModel.addressModel.observe(confirmReferendumVoteInformation::setAccount)
        viewModel.walletModel.observe(confirmReferendumVoteInformation::setWallet)

        viewModel.showNextProgress.observe(confirmReferendumVoteConfirm::setProgress)
    }
}
