package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.TimerValue
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
            graphics: Graphics = Graphics.error(),
            alert: AlertModel?,
            onCancel: () -> Unit,
        ) : Show(title, subtitle, graphics, alert, onCancel) {

            class RecoverableError(
                title: String,
                subtitle: String,
                graphics: Graphics = Graphics.error(),
                alert: AlertModel?,
                onCancel: () -> Unit,
                val onRetry: () -> Unit
            ) : Error(title, subtitle, graphics, alert, onCancel)

            class FatalError(
                title: String,
                subtitle: String,
                graphics: Graphics = Graphics.error(),
                alert: AlertModel?,
                val onConfirm: () -> Unit,
                onCancel: () -> Unit = onConfirm, // when error is fatal, confirm is the same as hide by default
            ) : Error(title, subtitle, graphics, alert, onCancel)
        }

        class Info(
            title: String,
            subtitle: String,
            graphics: Graphics = Graphics.info(),
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
    }

    class Graphics(
        @DrawableRes val icon: Int,
        @ColorRes val iconTint: Int?,
        @DrawableRes val background: Int,
    ) {
        companion object
    }
}

private fun LedgerMessageCommand.Graphics.Companion.error() = LedgerMessageCommand.Graphics(
    icon = R.drawable.ic_warning_filled,
    iconTint = null,
    background = R.drawable.ic_ledger_warning
)

private fun LedgerMessageCommand.Graphics.Companion.info() = LedgerMessageCommand.Graphics(
    icon = R.drawable.ic_checkmark_circle_16,
    iconTint = R.color.icon_secondary,
    background = R.drawable.ic_ledger_info
)

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
        }
    }

    private fun setupBaseShow(command: LedgerMessageCommand.Show) {
        binder.ledgerMessageTitle.text = command.title
        binder.ledgerMessageSubtitle.text = command.subtitle
        binder.ledgerMessageGraphics.setIcon(command.graphics.icon, command.graphics.iconTint)
        binder.ledgerMessageGraphics.setLedgerImage(command.graphics.background)
        binder.ledgerMessageAlert.setModelOrHide(command.alert)

        setOnCancelListener { command.onCancel() }
    }
}

fun LedgerMessageCommand.Companion.reviewAddress(
    resourceManager: ResourceManager,
    deviceName: String,
    address: String,
    onCancel: () -> Unit
): LedgerMessageCommand {
    return LedgerMessageCommand.Show.Info(
        title = resourceManager.getString(R.string.ledger_review_approve_title),
        subtitle = resourceManager.getString(R.string.ledger_verify_address_subtitle, deviceName),
        onCancel = onCancel,
        alert = null,
        footer = LedgerMessageCommand.Footer.Value(
            value = address,
        )
    )
}
