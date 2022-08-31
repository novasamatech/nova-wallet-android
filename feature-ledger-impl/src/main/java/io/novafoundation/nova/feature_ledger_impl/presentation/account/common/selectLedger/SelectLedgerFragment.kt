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
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.permissions.setupPermissionAsker
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessagePresentable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.setupLedgerMessages
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.model.SelectLedgerModel
import kotlinx.android.synthetic.main.fragment_select_ledger.selectLedgerDevices
import kotlinx.android.synthetic.main.fragment_select_ledger.selectLedgerProgress
import kotlinx.android.synthetic.main.fragment_select_ledger.selectLedgerToolbar
import javax.inject.Inject

abstract class SelectLedgerFragment<V : SelectLedgerViewModel> : BaseFragment<V>(), SelectLedgerAdapter.Handler {

    companion object {

        private const val PAYLOAD_KEY = "SelectLedgerFragment.PAYLOAD_KEY"

        fun getBundle(payload: SelectLedgerPayload): Bundle = bundleOf(PAYLOAD_KEY to payload)
    }

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

        setupPermissionAsker(viewModel)
        setupLedgerMessages(ledgerMessagePresentable)
    }

    override fun onStart() {
        super.onStart()

        enableBluetoothConnectivityTracker()
    }

    override fun onStop() {
        super.onStop()

        disableBluetoothConnectivityTracker()
    }

    protected fun payload() = argument<SelectLedgerPayload>(PAYLOAD_KEY)

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
