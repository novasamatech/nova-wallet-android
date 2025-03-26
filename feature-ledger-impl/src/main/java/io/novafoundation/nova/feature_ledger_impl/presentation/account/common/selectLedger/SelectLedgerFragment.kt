package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.permissions.setupPermissionAsker
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessagePresentable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.setupLedgerMessages
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.model.SelectLedgerModel
import kotlinx.android.synthetic.main.fragment_select_ledger.selectLedgerDevices
import kotlinx.android.synthetic.main.fragment_select_ledger.selectLedgerHints
import kotlinx.android.synthetic.main.fragment_select_ledger.selectLedgerProgress
import kotlinx.android.synthetic.main.fragment_select_ledger.selectLedgerToolbar
import javax.inject.Inject

abstract class SelectLedgerFragment<V : SelectLedgerViewModel> : BaseFragment<V>(), SelectLedgerAdapter.Handler {

    @Inject
    lateinit var ledgerMessagePresentable: LedgerMessagePresentable

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        SelectLedgerAdapter(this)
    }

    private val bluetoothConnectivityReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF -> viewModel.bluetoothStateChanged(BluetoothState.OFF)
                BluetoothAdapter.STATE_ON -> viewModel.bluetoothStateChanged(BluetoothState.ON)
            }
        }
    }

    private val locationStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action != null && action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                viewModel.locationStateChanged()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_select_ledger, container, false)
    }

    override fun initViews() {
        selectLedgerToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }
        selectLedgerToolbar.applyStatusBarInsets()

        selectLedgerDevices.setHasFixedSize(true)
        selectLedgerDevices.adapter = adapter
    }

    override fun subscribe(viewModel: V) {
        viewModel.deviceModels.observe {
            adapter.submitList(it)

            selectLedgerDevices.setVisible(it.isNotEmpty())
            selectLedgerProgress.setVisible(it.isEmpty())
        }

        viewModel.showRequestLocationDialog.observe {
            dialog(requireContext(), R.style.AccentAlertDialogTheme) {
                setTitle(R.string.select_ledger_location_enable_request_title)
                setMessage(getString(R.string.select_ledger_location_enable_request_message))
                setPositiveButton(R.string.common_enable) { _, _ -> viewModel.enableLocation() }
                setNegativeButton(R.string.common_cancel, null)
            }
        }

        viewModel.hints.observe(selectLedgerHints::setText)

        setupPermissionAsker(viewModel)
        setupLedgerMessages(ledgerMessagePresentable)
        observeBrowserEvents(viewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableBluetoothConnectivityTracker()
        enableLocationStateTracker()
    }

    override fun onDestroy() {
        super.onDestroy()

        disableBluetoothConnectivityTracker()
        disableLocationStateTracker()
    }

    private fun enableLocationStateTracker() {
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)

        requireActivity().registerReceiver(locationStateReceiver, filter)
    }

    private fun disableLocationStateTracker() {
        try {
            requireActivity().unregisterReceiver(locationStateReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    private fun enableBluetoothConnectivityTracker() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)

        requireActivity().registerReceiver(bluetoothConnectivityReceiver, filter)
    }

    private fun disableBluetoothConnectivityTracker() {
        try {
            requireActivity().unregisterReceiver(bluetoothConnectivityReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    override fun itemClicked(item: SelectLedgerModel) {
        viewModel.deviceClicked(item)
    }
}
