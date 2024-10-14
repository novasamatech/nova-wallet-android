package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.formatting.spannable.highlightedText
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.databinding.FragmentImportLedgerStartBinding
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureComponent

class StartImportLedgerFragment : BaseFragment<StartImportLedgerViewModel, FragmentImportLedgerStartBinding>() {

    override val binder by viewBinding(FragmentImportLedgerStartBinding::bind)

    override fun initViews() {
        binder.startImportLedgerToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.startImportLedgerToolbar.applyStatusBarInsets()

        binder.startImportLedgerContinue.setOnClickListener { viewModel.continueClicked() }

        binder.startImportLedgerDepractionWarning.setOnClickListener { viewModel.deprecationWarningClicked() }

        binder.startImportLedgerGuideLink.setOnClickListener { viewModel.guideClicked() }

        binder.startImportLedgerStep1.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_import_start_step_1,
                R.string.account_ledger_import_start_step_1_highlighted
            )
        )

        binder.startImportLedgerStep2.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_import_start_step_2,
                R.string.account_ledger_import_start_step_2_highlighted
            )
        )

        binder.startImportLedgerStep3.setStepText(
            requireContext().highlightedText(
                R.string.account_ledger_import_start_step_3,
                R.string.account_ledger_import_start_step_3_highlighted
            )
        )

        binder.startImportLedgerStep4.setStepText(
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

        viewModel.shouldShowWarning.observe(binder.startImportLedgerDepractionWarning::setVisible)
    }
}
