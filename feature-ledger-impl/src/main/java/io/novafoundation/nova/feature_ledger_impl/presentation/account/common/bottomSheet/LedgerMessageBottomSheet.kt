package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.common.view.setModelOrHide
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.databinding.FragmentLedgerMessageBinding

sealed class LedgerMessageCommand {

    companion object

    object Hide : LedgerMessageCommand()

    sealed class Show(
        val title: String,
        val subtitle: String,
        val graphics: Graphics,
        val alert: AlertModel?,
        val onCancel: () -> Unit,
    ) : LedgerMessageCommand() {

        sealed class Error(
            title: String,
            subtitle: String,
            graphics: Graphics,
            alert: AlertModel?,
            onCancel: () -> Unit,
        ) : Show(title, subtitle, graphics, alert, onCancel) {

            class RecoverableError(
                title: String,
                subtitle: String,
                graphics: Graphics,
                alert: AlertModel?,
                onCancel: () -> Unit,
                val onRetry: () -> Unit
            ) : Error(title, subtitle, graphics, alert, onCancel)

            class FatalError(
                title: String,
                subtitle: String,
                graphics: Graphics,
                alert: AlertModel?,
                val onConfirm: () -> Unit,
                onCancel: () -> Unit = onConfirm, // when error is fatal, confirm is the same as hide by default
            ) : Error(title, subtitle, graphics, alert, onCancel)
        }

        class Info(
            title: String,
            subtitle: String,
            graphics: Graphics,
            onCancel: () -> Unit,
            alert: AlertModel?,
            val footer: Footer
        ) : Show(title, subtitle, graphics, alert, onCancel)
    }

    sealed class Footer {

        class Timer(
            val timerValue: TimerValue,
            val closeToExpire: (TimerValue) -> Boolean,
            val timerFinished: () -> Unit,
            @StringRes val messageFormat: Int
        ) : Footer()

        class Value(val value: String) : Footer()

        class Columns(
            val first: Column,
            val second: Column
        ) : Footer() {

            class Column(
                val label: String,
                val value: String
            )
        }
    }

    class Graphics(@DrawableRes val ledgerImageRes: Int)
}

class LedgerMessageBottomSheet(
    context: Context,
) : BaseBottomSheet<FragmentLedgerMessageBinding>(context) {

    override val binder = FragmentLedgerMessageBinding.inflate(LayoutInflater.from(context))

    val container: View
        get() = binder.ledgerMessageContainer

    fun receiveCommand(command: LedgerMessageCommand) {
        binder.ledgerMessageActions.setVisible(command is LedgerMessageCommand.Show.Error)
        binder.ledgerMessageCancel.setVisible(command is LedgerMessageCommand.Show.Error.RecoverableError)
        setupFooterVisibility(command is LedgerMessageCommand.Show.Info)

        when (command) {
            LedgerMessageCommand.Hide -> dismiss()

            is LedgerMessageCommand.Show.Error.FatalError -> {
                setupBaseShow(command)
                binder.ledgerMessageConfirm.setOnClickListener { command.onConfirm() }
                binder.ledgerMessageConfirm.setText(R.string.common_ok_back)
            }

            is LedgerMessageCommand.Show.Error.RecoverableError -> {
                setupBaseShow(command)
                binder.ledgerMessageConfirm.setOnClickListener { command.onRetry() }
                binder.ledgerMessageConfirm.setText(R.string.common_retry)
                binder.ledgerMessageCancel.setOnClickListener { command.onCancel() }
            }

            is LedgerMessageCommand.Show.Info -> {
                setupBaseShow(command)
                showFooter(command.footer)
            }
        }
    }

    private fun setupFooterVisibility(visible: Boolean) {
        binder.ledgerMessageFooterMessage.setVisible(visible)

        if (!visible) {
            binder.ledgerMessageFooterMessage.stopTimer()
        }
    }

    private fun showFooter(footer: LedgerMessageCommand.Footer) {
        binder.ledgerMessageFooterMessage.setVisible(footer !is LedgerMessageCommand.Footer.Columns)
        binder.ledgerMessageFooterColumns.setVisible(footer is LedgerMessageCommand.Footer.Columns)

        when (footer) {
            is LedgerMessageCommand.Footer.Value -> {
                binder.ledgerMessageFooterMessage.text = footer.value
            }

            is LedgerMessageCommand.Footer.Timer -> {
                binder.ledgerMessageFooterMessage.startTimer(
                    value = footer.timerValue,
                    customMessageFormat = footer.messageFormat,
                    onTick = { view, _ ->
                        val textColorRes = if (footer.closeToExpire(footer.timerValue)) R.color.text_negative else R.color.text_secondary

                        view.setTextColorRes(textColorRes)
                    },
                    onFinish = { footer.timerFinished() }
                )
            }

            is LedgerMessageCommand.Footer.Columns -> {
                binder.ledgerMessageFooterTitle1.text = footer.first.label
                binder.ledgerMessageFooterMessage1.text = footer.first.value

                binder.ledgerMessageFooterTitle2.text = footer.second.label
                binder.ledgerMessageFooterMessage2.text = footer.second.value
            }
        }
    }

    private fun setupBaseShow(command: LedgerMessageCommand.Show) {
        binder.ledgerMessageTitle.text = command.title
        binder.ledgerMessageSubtitle.text = command.subtitle
        binder.ledgerMessageImage.setImageResource(command.graphics.ledgerImageRes)
        binder.ledgerMessageAlert.setModelOrHide(command.alert)

        setOnCancelListener { command.onCancel() }
    }
}
