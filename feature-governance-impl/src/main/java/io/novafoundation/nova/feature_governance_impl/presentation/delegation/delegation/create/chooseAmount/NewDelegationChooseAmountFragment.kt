package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.AmountChipModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.setChips
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view.setAmountChangeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser

class NewDelegationChooseAmountFragment : BaseFragment<NewDelegationChooseAmountViewModel>() {

    companion object {

        private const val PAYLOAD = "NewDelegationChooseAmountFragment.Payload"

        fun getBundle(payload: NewDelegationChooseAmountPayload): Bundle = bundleOf(PAYLOAD to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_new_delegation_choose_amount, container, false)
    }

    override fun initViews() {
        newDelegationChooseAmountContainer.applyStatusBarInsets()

        newDelegationChooseAmountToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        newDelegationChooseAmountConfirm.prepareForProgress(viewLifecycleOwner)
        newDelegationChooseAmountConfirm.setOnClickListener { viewModel.continueClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .newDelegationChooseAmountFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: NewDelegationChooseAmountViewModel) {
        setupAmountChooser(viewModel.amountChooserMixin, newDelegationChooseAmountAmount)
        observeValidations(viewModel)
        observeHints(viewModel.hintsMixin, newDelegationChooseAmountHints)

        viewModel.title.observe(newDelegationChooseAmountToolbar::setTitle)

        newDelegationChooseAmountVotePower.votePowerSeekbar.setValues(viewModel.convictionValues)
        newDelegationChooseAmountVotePower.votePowerSeekbar.bindTo(viewModel.selectedConvictionIndex, viewLifecycleOwner.lifecycleScope)

        viewModel.locksChangeUiFlow.observe {
            newDelegationChooseAmountLockedAmountChanges.setAmountChangeModel(it.amountChange)
            newDelegationChooseAmountLockedPeriodChanges.setAmountChangeModel(it.periodChange)
        }

        viewModel.amountChips.observe(::setChips)

        viewModel.votesFormattedFlow.observe {
            newDelegationChooseAmountVotePower.votePowerVotesText.text = it
        }

        viewModel.buttonState.observe(newDelegationChooseAmountConfirm::setState)
    }

    private fun setChips(newChips: List<AmountChipModel>) {
        newDelegationChooseAmountAmountChipsContainer.setChips(
            newChips = newChips,
            onClicked = viewModel::amountChipClicked,
            scrollingParent = newDelegationChooseAmountAmountChipsScroll
        )
    }
}
