package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Bundle
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.permissions.setupPermissionAsker
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.databinding.FragmentSelectLedgerBinding
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessagePresentable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.setupLedgerMessages
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.model.SelectLedgerModel
import javax.inject.Inject

abstract class SelectLedgerFragment<V : SelectLedgerViewModel> : BaseFragment<V, FragmentSelectLedgerBinding>(), SelectLedgerAdapter.Handler {

    override fun createBinding() = FragmentSelectLedgerBinding.inflate(layoutInflater)

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

    override fun initViews() {
        binder.selectLedgerToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }
        binder.selectLedgerToolbar.applyStatusBarInsets()

        binder.selectLedgerGrantPermissions.setOnClickListener { viewModel.requirePermissionsAndEnableBluetooth() }

        binder.selectLedgerDevices.setHasFixedSize(true)
        binder.selectLedgerDevices.adapter = adapter
    }

    override fun subscribe(viewModel: V) {
        viewModel.deviceModels.observe {
            adapter.submitList(it)

            binder.selectLedgerDevices.setVisible(it.isNotEmpty())
            binder.selectLedgerProgress.setVisible(it.isEmpty())
        }

        viewModel.showRequestLocationDialog.observe {
            dialog(requireContext(), R.style.AccentAlertDialogTheme) {
                setTitle(R.string.select_ledger_location_enable_request_title)
                setMessage(getString(R.string.select_ledger_location_enable_request_message))
                setPositiveButton(R.string.common_enable) { _, _ -> viewModel.enableLocation() }
                setNegativeButton(R.string.common_cancel, null)
            }
        }

        viewModel.hints.observe(binder.selectLedgerHints::setText)
        viewModel.showPermissionsButton.observe { selectLedgerGrantPermissions.isVisible = it }

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
