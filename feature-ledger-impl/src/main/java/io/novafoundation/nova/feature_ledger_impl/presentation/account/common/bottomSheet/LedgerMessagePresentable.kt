package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet

import android.content.Context
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.Event
import kotlinx.android.synthetic.main.fragment_ledger_message.ledgerMessageContainer

interface LedgerMessagePresentable {

    fun presentCommand(command: LedgerMessageCommand, context: Context)
}

interface LedgerMessageCommands {

    val ledgerMessageCommands: MutableLiveData<Event<LedgerMessageCommand>>
}

class SingleSheetLedgerMessagePresentable : LedgerMessagePresentable {

    private var bottomSheet: LedgerMessageBottomSheet? = null

    override fun presentCommand(command: LedgerMessageCommand, context: Context) {
        when {
            bottomSheet == null && command is LedgerMessageCommand.Show -> {
                bottomSheet = LedgerMessageBottomSheet(context)
                bottomSheet?.receiveCommand(command)
                bottomSheet?.show()
            }
            bottomSheet != null && command is LedgerMessageCommand.Show -> {
                bottomSheet?.ledgerMessageContainer
                    ?.animate()
                    ?.alpha(0f)
                    ?.withEndAction {
                        bottomSheet?.receiveCommand(command)

                        bottomSheet?.ledgerMessageContainer
                            ?.animate()
                            ?.alpha(1f)
                            ?.start()
                    }
                    ?.start()

            }
            else -> {
                bottomSheet?.receiveCommand(command)
                bottomSheet = null
            }
        }
    }
}

fun <F, V> F.setupLedgerMessages(presentable: LedgerMessagePresentable)
    where F : BaseFragment<V>, V : LedgerMessageCommands {
    viewModel.ledgerMessageCommands.observeEvent {
        presentable.presentCommand(it, requireContext())
    }
}
