package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.start

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.list.instruction.InstructionItem
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.formatting.spannable.highlightedText
import io.novafoundation.nova.common.utils.setupWithViewPager2
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.databinding.FragmentImportLedgerStartBinding

private const val BLUETOOTH_PAGE_INDEX = 0
private const val USB_PAGE_INDEX = 1

abstract class StartImportLedgerFragment<VM : StartImportLedgerViewModel> :
    BaseFragment<VM, FragmentImportLedgerStartBinding>(),
    StartImportLedgerPagerAdapter.Handler {

    protected val pageAdapter by lazy(LazyThreadSafetyMode.NONE) { StartImportLedgerPagerAdapter(createPages(), this) }

    override fun createBinding() = FragmentImportLedgerStartBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.startImportLedgerToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.startImportLedgerContinue.setOnClickListener {
            when (binder.startImportLedgerConnectionModePages.currentItem) {
                BLUETOOTH_PAGE_INDEX -> viewModel.continueWithBluetooth()
                USB_PAGE_INDEX -> viewModel.continueWithUsb()
            }
        }

        binder.startImportLedgerConnectionModePages.adapter = pageAdapter
        binder.startImportLedgerConnectionMode.setupWithViewPager2(binder.startImportLedgerConnectionModePages, pageAdapter::getPageTitle)
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
                InstructionItem.Step(1, networkAppIsInstalledStep()),
                InstructionItem.Step(2, openingNetworkAppStep()),
                InstructionItem.Step(3, enableBluetoothStep()),
                InstructionItem.Step(4, selectAccountStep())
            )
        )
    }

    private fun createUSBPage(): ConnectionModePageModel {
        return ConnectionModePageModel(
            modeName = getString(R.string.start_import_ledger_connection_mode_usb),
            guideItems = listOf(
                InstructionItem.Step(1, networkAppIsInstalledStep()),
                InstructionItem.Step(2, openingNetworkAppStep()),
                InstructionItem.Step(3, enableOTGSetting()),
                InstructionItem.Step(4, selectAccountStep())
            )
        )
    }

    abstract fun networkAppIsInstalledStep(): CharSequence

    abstract fun openingNetworkAppStep(): CharSequence

    private fun enableBluetoothStep() = requireContext().highlightedText(
        R.string.account_ledger_import_start_step_3,
        R.string.account_ledger_import_start_step_3_highlighted
    )

    private fun enableOTGSetting() = requireContext().highlightedText(
        R.string.account_ledger_import_start_step_otg,
        R.string.account_ledger_import_start_step_otg_highlighted
    )

    private fun selectAccountStep() = requireContext().highlightedText(
        R.string.account_ledger_import_start_step_4,
        R.string.account_ledger_import_start_step_4_highlighted
    )
}
