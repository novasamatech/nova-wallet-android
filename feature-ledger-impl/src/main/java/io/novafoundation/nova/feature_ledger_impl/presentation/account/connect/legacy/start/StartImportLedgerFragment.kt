package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.formatting.spannable.highlightedText
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent
import kotlinx.android.synthetic.main.fragment_import_ledger_start.startImportLedgerContinue
import kotlinx.android.synthetic.main.fragment_import_ledger_start.startImportLedgerDepractionWarning
import kotlinx.android.synthetic.main.fragment_import_ledger_start.startImportLedgerGuideLink
import kotlinx.android.synthetic.main.fragment_import_ledger_start.startImportLedgerStep1
import kotlinx.android.synthetic.main.fragment_import_ledger_start.startImportLedgerStep2
import kotlinx.android.synthetic.main.fragment_import_ledger_start.startImportLedgerStep3
import kotlinx.android.synthetic.main.fragment_import_ledger_start.startImportLedgerStep4
import kotlinx.android.synthetic.main.fragment_import_ledger_start.startImportLedgerToolbar

class StartImportLedgerFragment : BaseFragment<StartImportLedgerViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_import_ledger_start, container, false)
    }

    override fun initViews() {
        startImportLedgerToolbar.setHomeButtonListener { viewModel.backClicked() }
        startImportLedgerToolbar.applyStatusBarInsets()

        startImportLedgerContinue.setOnClickListener { viewModel.continueClicked() }

        startImportLedgerDepractionWarning.setOnClickListener { viewModel.deprecationWarningClicked() }

        startImportLedgerGuideLink.setOnClickListener { viewModel.guideClicked() }

        startImportLedgerStep1.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_import_start_step_1,
                R.string.account_ledger_import_start_step_1_highlighted
            )
        )

        startImportLedgerStep2.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_import_start_step_2,
                R.string.account_ledger_import_start_step_2_highlighted
            )
        )

        startImportLedgerStep3.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_import_start_step_3,
                R.string.account_ledger_import_start_step_3_highlighted
            )
        )

        startImportLedgerStep4.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_import_start_step_4,
                R.string.account_ledger_import_start_step_4_highlighted
            )
        )
    }

    override fun inject() {
        FeatureUtils.getFeature<LedgerFeatureComponent>(requireContext(), LedgerFeatureApi::class.java)
            .startImportLedgerComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: StartImportLedgerViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.shouldShowWarning.observe(startImportLedgerDepractionWarning::setVisible)
    }
}
