package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.start

import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.formatting.spannable.highlightedText
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.databinding.FragmentGenericImportLedgerStartBinding
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent

class StartImportGenericLedgerFragment : BaseFragment<StartImportGenericLedgerViewModel, FragmentGenericImportLedgerStartBinding>() {

    override val binder by viewBinding(FragmentGenericImportLedgerStartBinding::bind)

    override fun initViews() {
        binder.startImportGenericLedgerToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.startImportGenericLedgerToolbar.applyStatusBarInsets()

        binder.startImportGenericLedgerContinue.setOnClickListener { viewModel.continueClicked() }

        binder.startImportGenericLedgerGuideLink.setOnClickListener { viewModel.guideClicked() }

        binder.startImportGenericLedgerStep1.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_generic_import_start_step_1,
                R.string.account_ledger_generic_import_start_step_1_highlighted
            )
        )
        binder.startImportGenericLedgerStep2.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_generic_import_start_step_2,
                R.string.account_ledger_generic_import_start_step_2_highlighted
            )
        )
        binder.startImportGenericLedgerStep3.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_import_start_step_3,
                R.string.account_ledger_import_start_step_3_highlighted
            )
        )
        binder.startImportGenericLedgerStep4.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_import_start_step_4,
                R.string.account_ledger_import_start_step_4_highlighted
            )
        )
    }

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .startImportGenericLedgerComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: StartImportGenericLedgerViewModel) {
        observeBrowserEvents(viewModel)
    }
}
