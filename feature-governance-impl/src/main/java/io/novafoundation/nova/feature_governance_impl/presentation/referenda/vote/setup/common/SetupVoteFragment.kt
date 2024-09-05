package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.AmountChipModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.setChips
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view.setAmountChangeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import kotlinx.android.synthetic.main.fragment_setup_vote.setupReferendumVoteAlertView
import kotlinx.android.synthetic.main.fragment_setup_vote.setupReferendumVoteAmount
import kotlinx.android.synthetic.main.fragment_setup_vote.setupReferendumVoteAmountChipsContainer
import kotlinx.android.synthetic.main.fragment_setup_vote.setupReferendumVoteAmountChipsScroll
import kotlinx.android.synthetic.main.fragment_setup_vote.setupReferendumVoteContainer
import kotlinx.android.synthetic.main.fragment_setup_vote.setupReferendumVoteLockedAmountChanges
import kotlinx.android.synthetic.main.fragment_setup_vote.setupReferendumVoteLockedPeriodChanges
import kotlinx.android.synthetic.main.fragment_setup_vote.setupReferendumVoteTitle
import kotlinx.android.synthetic.main.fragment_setup_vote.setupReferendumVoteToolbar
import kotlinx.android.synthetic.main.fragment_setup_vote.setupReferendumVoteVotePower
import kotlinx.android.synthetic.main.fragment_setup_vote.view.setupVoteControlFrame

abstract class SetupVoteFragment<T : SetupVoteViewModel> : BaseFragment<T>() {

    companion object {

        private const val PAYLOAD = "SetupVoteFragment.Payload"

        fun getBundle(payload: SetupVotePayload): Bundle = bundleOf(PAYLOAD to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setup_vote, container, false)
        view.setupVoteControlFrame.addView(getControlView(inflater, view))
        return view
    }

    override fun initViews() {
        setupReferendumVoteContainer.applyStatusBarInsets()

        setupReferendumVoteToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }
    }

    override fun subscribe(viewModel: T) {
        setupAmountChooser(viewModel.amountChooserMixin, setupReferendumVoteAmount)
        observeValidations(viewModel)

        setupReferendumVoteVotePower.votePowerSeekbar.setValues(viewModel.convictionValues)
        setupReferendumVoteVotePower.votePowerSeekbar.bindTo(viewModel.selectedConvictionIndex, viewLifecycleOwner.lifecycleScope)

        viewModel.title.observe(setupReferendumVoteTitle::setText)

        viewModel.locksChangeUiFlow.observe {
            setupReferendumVoteLockedAmountChanges.setAmountChangeModel(it.amountChange)
            setupReferendumVoteLockedPeriodChanges.setAmountChangeModel(it.periodChange)
        }

        viewModel.amountChips.observe(::setChips)

        viewModel.votesFormattedFlow.observe {
            setupReferendumVoteVotePower.votePowerVotesText.text = it
        }

        viewModel.abstainVotingSupported.observe {
            setupReferendumVoteAlertView.isVisible = it
        }
    }

    private fun setChips(newChips: List<AmountChipModel>) {
        setupReferendumVoteAmountChipsContainer.setChips(
            newChips = newChips,
            onClicked = viewModel::amountChipClicked,
            scrollingParent = setupReferendumVoteAmountChipsScroll
        )
    }

    protected fun getPayload(): SetupVotePayload {
        return argument(PAYLOAD)
    }

    abstract fun getControlView(inflater: LayoutInflater, parent: View): View
}
