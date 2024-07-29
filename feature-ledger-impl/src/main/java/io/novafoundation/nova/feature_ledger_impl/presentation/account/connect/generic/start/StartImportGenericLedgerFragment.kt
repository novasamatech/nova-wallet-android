package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.formatting.spannable.*
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import kotlinx.android.synthetic.main.fragment_generic_import_ledger_start.startImportGenericLedgerContinue
import kotlinx.android.synthetic.main.fragment_generic_import_ledger_start.startImportGenericLedgerGuideLink
import kotlinx.android.synthetic.main.fragment_generic_import_ledger_start.startImportGenericLedgerStep1
import kotlinx.android.synthetic.main.fragment_generic_import_ledger_start.startImportGenericLedgerStep2
import kotlinx.android.synthetic.main.fragment_generic_import_ledger_start.startImportGenericLedgerStep3
import kotlinx.android.synthetic.main.fragment_generic_import_ledger_start.startImportGenericLedgerStep4
import kotlinx.android.synthetic.main.fragment_generic_import_ledger_start.startImportGenericLedgerToolbar

class StartImportGenericLedgerFragment : BaseFragment<StartImportGenericLedgerViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_generic_import_ledger_start, container, false)
    }

    override fun initViews() {
        startImportGenericLedgerToolbar.setHomeButtonListener { viewModel.backClicked() }
        startImportGenericLedgerToolbar.applyStatusBarInsets()

        startImportGenericLedgerContinue.setOnClickListener { viewModel.continueClicked() }

        startImportGenericLedgerGuideLink.setOnClickListener { viewModel.guideClicked() }

        startImportGenericLedgerStep1.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_generic_import_start_step_1,
                R.string.account_ledger_generic_import_start_step_1_highlighted
            )
        )
        startImportGenericLedgerStep2.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_generic_import_start_step_2,
                R.string.account_ledger_generic_import_start_step_2_highlighted
            )
        )
        startImportGenericLedgerStep3.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_import_start_step_3,
                R.string.account_ledger_import_start_step_3_highlighted
            )
        )
        startImportGenericLedgerStep4.setStepText(
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
