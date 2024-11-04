package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentNewDelegationChooseAmountBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.AmountChipModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.setChips
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view.setAmountChangeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser

class NewDelegationChooseAmountFragment : BaseFragment<NewDelegationChooseAmountViewModel, FragmentNewDelegationChooseAmountBinding>() {

    companion object {

        private const val PAYLOAD = "NewDelegationChooseAmountFragment.Payload"

        fun getBundle(payload: NewDelegationChooseAmountPayload): Bundle = bundleOf(PAYLOAD to payload)
    }

    override val binder by viewBinding(FragmentNewDelegationChooseAmountBinding::bind)

    override fun initViews() {
        binder.newDelegationChooseAmountContainer.applyStatusBarInsets()

        binder.newDelegationChooseAmountToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.newDelegationChooseAmountConfirm.prepareForProgress(viewLifecycleOwner)
        binder.newDelegationChooseAmountConfirm.setOnClickListener { viewModel.continueClicked() }
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
        setupAmountChooser(viewModel.amountChooserMixin, binder.newDelegationChooseAmountAmount)
        observeValidations(viewModel)
        observeHints(viewModel.hintsMixin, binder.newDelegationChooseAmountHints)

        viewModel.title.observe(binder.newDelegationChooseAmountToolbar::setTitle)

        binder.newDelegationChooseAmountVotePower.votePowerSeekbar.setValues(viewModel.convictionValues)
        binder.newDelegationChooseAmountVotePower.votePowerSeekbar.bindTo(viewModel.selectedConvictionIndex, viewLifecycleOwner.lifecycleScope)

        viewModel.locksChangeUiFlow.observe {
            binder.newDelegationChooseAmountLockedAmountChanges.setAmountChangeModel(it.amountChange)
            binder.newDelegationChooseAmountLockedPeriodChanges.setAmountChangeModel(it.periodChange)
        }

        viewModel.amountChips.observe(::setChips)

        viewModel.votesFormattedFlow.observe {
            binder.newDelegationChooseAmountVotePower.votePowerVotesText.text = it
        }

        viewModel.buttonState.observe(binder.newDelegationChooseAmountConfirm::setState)
    }

    private fun setChips(newChips: List<AmountChipModel>) {
        binder.newDelegationChooseAmountAmountChipsContainer.setChips(
            newChips = newChips,
            onClicked = viewModel::amountChipClicked,
            scrollingParent = binder.newDelegationChooseAmountAmountChipsScroll
        )
    }
}
