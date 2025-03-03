package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.formatting.spannable.highlightedText
import io.novafoundation.nova.common.utils.setupWithViewPager2
import io.novafoundation.nova.feature_ledger_impl.R
import kotlinx.android.synthetic.main.fragment_import_ledger_start.startImportLedgerConnectionMode
import kotlinx.android.synthetic.main.fragment_import_ledger_start.startImportLedgerConnectionModePages
import kotlinx.android.synthetic.main.fragment_import_ledger_start.startImportLedgerContinue
import kotlinx.android.synthetic.main.fragment_import_ledger_start.startImportLedgerToolbar

private const val BLUETOOTH_PAGE_INDEX = 0
private const val USB_PAGE_INDEX = 1

abstract class StartImportLedgerFragment<VM : StartImportLedgerViewModel> : BaseFragment<VM>(), StartImportLedgerPagerAdapter.Handler {

    protected val pageAdapter by lazy(LazyThreadSafetyMode.NONE) { StartImportLedgerPagerAdapter(createPages(), this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_import_ledger_start, container, false)
    }

    override fun initViews() {
        startImportLedgerToolbar.setHomeButtonListener { viewModel.backClicked() }
        startImportLedgerToolbar.applyStatusBarInsets()

        startImportLedgerContinue.setOnClickListener {
            when (startImportLedgerConnectionModePages.currentItem) {
                BLUETOOTH_PAGE_INDEX -> viewModel.continueWithBluetooth()
                USB_PAGE_INDEX -> viewModel.continueWithUsb()
            }
        }

        startImportLedgerConnectionModePages.adapter = pageAdapter
        startImportLedgerConnectionMode.setupWithViewPager2(startImportLedgerConnectionModePages, pageAdapter::getPageTitle)
    }

    override fun subscribe(viewModel: VM) {
        observeBrowserEvents(viewModel)
    }

    override fun guideLinkClicked() {
        viewModel.guideClicked()
    }

    private fun createPages(): List<ConnectionModePageModel> {
        return buildList {
            add(BLUETOOTH_PAGE_INDEX, createBluetoothPage())
            add(USB_PAGE_INDEX, createUSBPage())
        }
    }

    private fun createBluetoothPage(): ConnectionModePageModel {
        return ConnectionModePageModel(
            modeName = getString(R.string.start_import_ledger_connection_mode_bluetooth),
            guideItems = listOf(
                LedgerGuideItem(1, networkAppIsInstalledStep()),
                LedgerGuideItem(2, openingNetworkAppStep()),
                LedgerGuideItem(3, enableBluetoothStep()),
                LedgerGuideItem(4, selectAccountStep())
            )
        )
    }

    private fun createUSBPage(): ConnectionModePageModel {
        return ConnectionModePageModel(
            modeName = getString(R.string.start_import_ledger_connection_mode_usb),
            guideItems = listOf(
                LedgerGuideItem(1, networkAppIsInstalledStep()),
                LedgerGuideItem(2, openingNetworkAppStep()),
                LedgerGuideItem(3, selectAccountStep())
            )
        )
    }

    private fun networkAppIsInstalledStep() = requireContext().highlightedText(
        R.string.account_ledger_import_start_step_1,
        R.string.account_ledger_import_start_step_1_highlighted
    )

    private fun openingNetworkAppStep() = requireContext().highlightedText(
        R.string.account_ledger_import_start_step_2,
        R.string.account_ledger_import_start_step_2_highlighted
    )

    private fun enableBluetoothStep() = requireContext().highlightedText(
        R.string.account_ledger_import_start_step_3,
        R.string.account_ledger_import_start_step_3_highlighted
    )

    private fun selectAccountStep() = requireContext().highlightedText(
        R.string.account_ledger_import_start_step_4,
        R.string.account_ledger_import_start_step_4_highlighted
    )
}
