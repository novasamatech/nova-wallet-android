package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.common

import android.widget.TextView
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenResponder
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.cancelled
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.ValidityPeriod
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.closeToExpire
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

interface QrCodeExpiredPresentable {

    val acknowledgeExpired: ActionAwaitableMixin<String, Unit>

    interface Presentation : QrCodeExpiredPresentable {

        suspend fun showQrCodeExpired(validityPeriod: ValidityPeriod)
    }
}

class QrCodeExpiredPresentableFactory(
    private val resourceManager: ResourceManager,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val router: AccountRouter,
    private val responder: ParitySignerSignInterScreenResponder,
) {

    fun create(
        request: ParitySignerSignInterScreenCommunicator.Request
    ): QrCodeExpiredPresentable.Presentation = RealQrCodeExpiredPresentable(
        resourceManager = resourceManager,
        actionAwaitableMixinFactory = actionAwaitableMixinFactory,
        router = router,
        responder = responder,
        request = request
    )
}

private class RealQrCodeExpiredPresentable(
    private val resourceManager: ResourceManager,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val router: AccountRouter,
    private val responder: ParitySignerSignInterScreenResponder,
    private val request: ParitySignerSignInterScreenCommunicator.Request
) : QrCodeExpiredPresentable.Presentation {

    override val acknowledgeExpired: ActionAwaitableMixin.Presentation<String, Unit> = actionAwaitableMixinFactory.create()

    @OptIn(ExperimentalTime::class)
    override suspend fun showQrCodeExpired(validityPeriod: ValidityPeriod) {
        val message = withContext(Dispatchers.Default) {
            val validityPeriodMillis = validityPeriod.period.millis
            val durationFormatted = resourceManager.formatDuration(validityPeriodMillis.milliseconds, estimated = false)
            resourceManager.getString(R.string.account_parity_signer_sign_qr_code_expired_descrition, durationFormatted)
        }

        acknowledgeExpired.awaitAction(message)

        responder.respond(request.cancelled())
        router.finishParitySignerFlow()
    }
}

fun BaseFragment<*>.observeValidityPeriod(
    validityPeriodFlow: Flow<ValidityPeriod>,
    qrCodeExpiredPresentable: QrCodeExpiredPresentable,
    timerView: TextView,
    onTimerFinished: () -> Unit
) {
    validityPeriodFlow.observe { validityPeriod ->
        timerView.startTimer(
            value = validityPeriod.period,
            customMessageFormat = R.string.account_parity_signer_sign_qr_code_valid_format,
            lifecycle = viewLifecycleOwner.lifecycle,
            onTick = { view, _ ->
                val textColorRes = if (validityPeriod.closeToExpire()) R.color.red else R.color.white_64

                view.setTextColorRes(textColorRes)
            },
            onFinish = { view ->
                onTimerFinished()

                view.setText(R.string.account_parity_signer_sign_qr_code_expired)
            }
        )
    }

    qrCodeExpiredPresentable.acknowledgeExpired.awaitableActionLiveData.observeEvent {
        errorDialog(
            context = requireContext(),
            onConfirm = { it.onSuccess(Unit) }
        ) {
            setTitle(R.string.account_parity_signer_sign_qr_code_expired)
            setMessage(it.payload)
        }
    }
}
