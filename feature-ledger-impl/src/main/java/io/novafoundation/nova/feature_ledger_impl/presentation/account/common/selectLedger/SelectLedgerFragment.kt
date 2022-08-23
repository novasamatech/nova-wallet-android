package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.model.SelectLedgerModel
import kotlinx.android.synthetic.main.fragment_select_ledger.selectLedgerDevices
import kotlinx.android.synthetic.main.fragment_select_ledger.selectLedgerProgress
import kotlinx.android.synthetic.main.fragment_select_ledger.selectLedgerToolbar


abstract class SelectLedgerFragment<V : SelectLedgerViewModel> : BaseFragment<V>(), SelectLedgerAdapter.Handler {

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_select_ledger, container, false)
    }

    override fun initViews() {
        selectLedgerToolbar.setHomeButtonListener { viewModel.backClicked() }
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
    }

    override fun onStart() {
        super.onStart()

//        lifecycleScope.launchWhenStarted {
//            if (viewModel.canTrackBluetooth.first()) {
//                enableBluetoothConnectivityTracker()
//            }
//        }

        enableBluetoothConnectivityTracker()
    }

    override fun onStop() {
        super.onStop()

        disableBluetoothConnectivityTracker()
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
