package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet

import android.content.Context
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_ledger_impl.R
import kotlinx.android.synthetic.main.fragment_ledger_message.ledgerMessageActions
import kotlinx.android.synthetic.main.fragment_ledger_message.ledgerMessageCancel
import kotlinx.android.synthetic.main.fragment_ledger_message.ledgerMessageConfirm
import kotlinx.android.synthetic.main.fragment_ledger_message.ledgerMessageContainer
import kotlinx.android.synthetic.main.fragment_ledger_message.ledgerMessageFooterMessage
import kotlinx.android.synthetic.main.fragment_ledger_message.ledgerMessageGraphics
import kotlinx.android.synthetic.main.fragment_ledger_message.ledgerMessageSubtitle
import kotlinx.android.synthetic.main.fragment_ledger_message.ledgerMessageTitle

sealed class LedgerMessageCommand {

    object Hide : LedgerMessageCommand()

    sealed class Show(
        val title: String,
        val subtitle: String,
        val graphics: Graphics,
        val onCancel: () -> Unit,
    ) : LedgerMessageCommand() {

        sealed class Actions(
            title: String,
            subtitle: String,
            graphics: Graphics,
            onCancel: () -> Unit,
        ) : Show(title, subtitle, graphics, onCancel) {

            class RecoverableError(
                title: String,
                subtitle: String,
                graphics: Graphics,
                onCancel: () -> Unit,
                val onRetry: () -> Unit
            ) : Actions(title, subtitle, graphics, onCancel)

            class FatalError(
                title: String,
                subtitle: String,
                graphics: Graphics,
                val onConfirm: () -> Unit,
                onCancel: () -> Unit = onConfirm, // when error is fatal, confirm is the same as hide by default
            ) : Actions(title, subtitle, graphics, onCancel)
        }

        class Info(
            title: String,
            subtitle: String,
            graphics: Graphics,
            onCancel: () -> Unit,
            val footer: Footer
        ) : Show(title, subtitle, graphics, onCancel)
    }

    sealed class Footer {

        class Timer(
            val timerValue: TimerValue,
            val closeToExpire: (TimerValue) -> Boolean,
            val timerFinished: () -> Unit,
            @StringRes val messageFormat: Int
        ) : Footer()

        class Value(val value: String) : Footer()
    }

    class Graphics(@DrawableRes val src: Int, @ColorRes val tint: Int? = null)
}

class LedgerMessageBottomSheet(
    context: Context,
) : BaseBottomSheet(context) {

    val container: View
        get() = ledgerMessageContainer

    init {
        setContentView(R.layout.fragment_ledger_message)
    }

    fun receiveCommand(command: LedgerMessageCommand) {
        ledgerMessageActions.setVisible(command is LedgerMessageCommand.Show.Actions)
        ledgerMessageCancel.setVisible(command is LedgerMessageCommand.Show.Actions.RecoverableError)
        setupFooterVisibility(command is LedgerMessageCommand.Show.Info)

        when (command) {
            LedgerMessageCommand.Hide -> dismiss()

            is LedgerMessageCommand.Show.Actions.FatalError -> {
                setupBaseShow(command)
                ledgerMessageConfirm.setOnClickListener { command.onConfirm() }
                ledgerMessageConfirm.setText(R.string.common_ok_back)
            }

            is LedgerMessageCommand.Show.Actions.RecoverableError -> {
                setupBaseShow(command)
                ledgerMessageConfirm.setOnClickListener { command.onRetry() }
                ledgerMessageConfirm.setText(R.string.common_retry)
                ledgerMessageCancel.setOnClickListener { command.onCancel() }
            }

            is LedgerMessageCommand.Show.Info -> {
                setupBaseShow(command)
                showFooter(command.footer)
            }
        }
    }

    private fun setupFooterVisibility(visible: Boolean) {
        ledgerMessageFooterMessage.setVisible(visible)

        if (!visible) {
            ledgerMessageFooterMessage.stopTimer()
        }
    }

    private fun showFooter(footer: LedgerMessageCommand.Footer) {
        when (footer) {
            is LedgerMessageCommand.Footer.Value -> {
                ledgerMessageFooterMessage.text = footer.value
            }
            is LedgerMessageCommand.Footer.Timer -> {
                ledgerMessageFooterMessage.startTimer(
                    value = footer.timerValue,
                    customMessageFormat = footer.messageFormat,
                    onTick = { view, _ ->
                        val textColorRes = if (footer.closeToExpire(footer.timerValue)) R.color.red else R.color.white_64

                        view.setTextColorRes(textColorRes)
                    },
                    onFinish = { footer.timerFinished() }
                )
            }
        }
    }

    private fun setupBaseShow(command: LedgerMessageCommand.Show) {
        ledgerMessageTitle.text = command.title
        ledgerMessageSubtitle.text = command.subtitle
        ledgerMessageGraphics.setIcon(command.graphics.src, command.graphics.tint)

        setOnCancelListener { command.onCancel() }
    }
}
