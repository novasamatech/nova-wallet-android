package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet

import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.second
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand.Footer.Columns.Column
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand.Show.Error.RecoverableError
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers.LedgerDeviceFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter.MessageKind
import io.novafoundation.nova.runtime.extrinsic.ValidityPeriod
import io.novafoundation.nova.runtime.extrinsic.closeToExpire

class MessageCommandFormatterFactory(
    private val resourceManager: ResourceManager,
    private val deviceMapper: LedgerDeviceFormatter
) {

    fun create(messageFormatter: LedgerMessageFormatter): MessageCommandFormatter {
        return MessageCommandFormatter(resourceManager, deviceMapper, messageFormatter)
    }
}

class MessageCommandFormatter(
    private val resourceManager: ResourceManager,
    private val deviceMapper: LedgerDeviceFormatter,
    private val messageFormatter: LedgerMessageFormatter,
) {

    context(Browserable.Presentation)
    suspend fun unknownError(
        device: LedgerDevice,
        onRetry: () -> Unit,
        onCancel: () -> Unit
    ): LedgerMessageCommand {
        return retryCommand(
            title = resourceManager.getString(R.string.ledger_error_general_title),
            subtitle = resourceManager.getString(R.string.ledger_error_general_message),
            alertModel = messageFormatter.alertForKind(MessageKind.OTHER),
            device = device,
            onRetry = onRetry,
            onCancel = onCancel
        )
    }

    context(Browserable.Presentation)
    suspend fun substrateApplicationError(
        reason: LedgerApplicationResponse,
        device: LedgerDevice,
        onCancel: () -> Unit,
        onRetry: () -> Unit
    ): LedgerMessageCommand {
        val errorTitle: String
        val errorMessage: String
        val alert: AlertModel?

        when (reason) {
            LedgerApplicationResponse.APP_NOT_OPEN, LedgerApplicationResponse.WRONG_APP_OPEN -> {
                val appName = messageFormatter.appName()

                errorTitle = resourceManager.getString(R.string.ledger_error_app_not_launched_title, appName)
                errorMessage = resourceManager.getString(R.string.ledger_error_app_not_launched_message, appName)
                alert = messageFormatter.alertForKind(MessageKind.APP_NOT_OPEN)
            }

            LedgerApplicationResponse.TRANSACTION_REJECTED -> {
                errorTitle = resourceManager.getString(R.string.ledger_error_app_cancelled_title)
                errorMessage = resourceManager.getString(R.string.ledger_error_app_cancelled_message)
                alert = messageFormatter.alertForKind(MessageKind.OTHER)
            }

            else -> {
                errorTitle = resourceManager.getString(R.string.ledger_error_general_title)
                errorMessage = resourceManager.getString(R.string.ledger_error_general_message)
                alert = messageFormatter.alertForKind(MessageKind.OTHER)
            }
        }

        return retryCommand(errorTitle, errorMessage, device, alert, onCancel, onRetry)
    }

    fun retryCommand(
        title: String,
        subtitle: String,
        device: LedgerDevice,
        alertModel: AlertModel?,
        onCancel: () -> Unit,
        onRetry: () -> Unit
    ): LedgerMessageCommand {
        val deviceMapper = deviceMapper.createDelegate(device)
        return RecoverableError(
            title = title,
            subtitle = subtitle,
            alert = alertModel,
            onCancel = onCancel,
            onRetry = onRetry,
            graphics = deviceMapper.getErrorImage()
        )
    }

    context(Browserable.Presentation)
    suspend fun fatalErrorCommand(
        title: String,
        subtitle: String,
        device: LedgerDevice,
        onCancel: () -> Unit,
        onConfirm: () -> Unit,
    ): LedgerMessageCommand {
        val deviceMapper = deviceMapper.createDelegate(device)

        return LedgerMessageCommand.Show.Error.FatalError(
            title = title,
            subtitle = subtitle,
            alert = messageFormatter.alertForKind(MessageKind.OTHER),
            graphics = deviceMapper.getErrorImage(),
            onCancel = onCancel,
            onConfirm = onConfirm
        )
    }

    context(Browserable.Presentation)
    suspend fun signCommand(
        validityPeriod: ValidityPeriod,
        device: LedgerDevice,
        onTimeFinished: () -> Unit,
        onCancel: () -> Unit,
    ): LedgerMessageCommand {
        val deviceMapper = deviceMapper.createDelegate(device)

        return LedgerMessageCommand.Show.Info(
            title = resourceManager.getString(R.string.ledger_review_approve_title),
            subtitle = deviceMapper.getSignMessage(),
            onCancel = onCancel,
            alert = messageFormatter.alertForKind(MessageKind.OTHER),
            graphics = deviceMapper.getSignImage(),
            footer = LedgerMessageCommand.Footer.Timer(
                timerValue = validityPeriod.period,
                closeToExpire = { validityPeriod.closeToExpire() },
                timerFinished = { onTimeFinished() },
                messageFormat = R.string.ledger_sign_transaction_validity_format
            )
        )
    }

    fun reviewAddressCommand(
        addresses: List<Pair<AddressScheme, String>>,
        device: LedgerDevice,
        onCancel: () -> Unit,
    ): LedgerMessageCommand {
        val deviceMapper = deviceMapper.createDelegate(device)

        val footer = when (addresses.size) {
            0 -> error("At least one address should be not null")

            1 -> LedgerMessageCommand.Footer.Value(
                value = addresses.single().second.toTwoLinesAddress(),
            )

            2 -> LedgerMessageCommand.Footer.Columns(
                first = columnFor(addresses.first()),
                second = columnFor(addresses.second())
            )


            else -> error("Too many addresses passed: ${addresses.size}")
        }

        return LedgerMessageCommand.Show.Info(
            title = resourceManager.getString(R.string.ledger_review_approve_title),
            subtitle = deviceMapper.getReviewAddressMessage(),
            onCancel = onCancel,
            alert = null,
            graphics = deviceMapper.getApproveImage(),
            footer = footer
        )
    }

    fun hideCommand(): LedgerMessageCommand {
        return LedgerMessageCommand.Hide
    }

    private fun String.toTwoLinesAddress(): String {
        val middle = length / 2
        return substring(0, middle) + "\n" + substring(middle)
    }

    private fun columnFor(addressWithScheme: Pair<AddressScheme, String>): Column {
        val label = when (addressWithScheme.first) {
            AddressScheme.EVM -> resourceManager.getString(R.string.common_substrate_address)
            AddressScheme.SUBSTRATE -> resourceManager.getString(R.string.common_evm_address)
        }

        return Column(label, addressWithScheme.second)
    }
}

@Suppress("UNCHECKED_CAST")
fun createLedgerReviewAddresses(
    allowedAddressSchemes: List<AddressScheme>,
    vararg allAddresses: Pair<AddressScheme, String?>
): List<Pair<AddressScheme, String>> {
    return allAddresses.filter { it.first in allowedAddressSchemes && it.second != null } as List<Pair<AddressScheme, String>>
}
