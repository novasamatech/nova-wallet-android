package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentSetupVoteBinding
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.AmountChipModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.setChips
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view.setAmountChangeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser

abstract class SetupVoteFragment<T : SetupVoteViewModel> : BaseFragment<T, FragmentSetupVoteBinding>() {

    companion object {

        private const val PAYLOAD = "SetupVoteFragment.Payload"

        fun getBundle(payload: SetupVotePayload): Bundle = bundleOf(PAYLOAD to payload)
    }

    override fun createBinding() = FragmentSetupVoteBinding.inflate(layoutInflater)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = onCreateView(inflater, container, savedInstanceState)
        binder.setupVoteControlFrame.addView(getControlView(inflater, view))
        return view
    }

    override fun initViews() {
        binder.setupReferendumVoteContainer.applyStatusBarInsets()

        binder.setupReferendumVoteToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }
    }

    override fun subscribe(viewModel: T) {
        setupAmountChooser(viewModel.amountChooserMixin, binder.setupReferendumVoteAmount)
        observeValidations(viewModel)

        binder.setupReferendumVoteVotePower.votePowerSeekbar.setValues(viewModel.convictionValues)
        binder.setupReferendumVoteVotePower.votePowerSeekbar.bindTo(viewModel.selectedConvictionIndex, viewLifecycleOwner.lifecycleScope)

        viewModel.title.observe(binder.setupReferendumVoteTitle::setText)

        viewModel.locksChangeUiFlow.observe {
            binder.setupReferendumVoteLockedAmountChanges.setAmountChangeModel(it.amountChange)
            binder.setupReferendumVoteLockedPeriodChanges.setAmountChangeModel(it.periodChange)
        }

        viewModel.amountChips.observe(::setChips)

        viewModel.votesFormattedFlow.observe {
            binder.setupReferendumVoteVotePower.votePowerVotesText.text = it
        }

        viewModel.abstainVotingSupported.observe {
            binder.setupReferendumVoteAlertView.isVisible = it
        }
    }

    private fun setChips(newChips: List<AmountChipModel>) {
        binder.setupReferendumVoteAmountChipsContainer.setChips(
            newChips = newChips,
            onClicked = viewModel::amountChipClicked,
            scrollingParent = binder.setupReferendumVoteAmountChipsScroll
        )
    }

    protected fun getPayload(): SetupVotePayload {
        return argument(PAYLOAD)
    }

    abstract fun getControlView(inflater: LayoutInflater, parent: View): View
}
