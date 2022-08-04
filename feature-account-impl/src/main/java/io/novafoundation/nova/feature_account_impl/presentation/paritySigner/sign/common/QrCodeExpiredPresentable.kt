package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.common

import android.widget.TextView
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.hasAlredyTriggered
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.ValidityPeriod
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.closeToExpire
import kotlinx.coroutines.CoroutineScope
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
) {

    fun create(coroutineScope: CoroutineScope): QrCodeExpiredPresentable.Presentation = RealQrCodeExpiredPresentable(
        resourceManager = resourceManager,
        actionAwaitableMixinFactory = actionAwaitableMixinFactory,
        router = router,
        coroutineScope = coroutineScope
    )
}

private class RealQrCodeExpiredPresentable(
    private val resourceManager: ResourceManager,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val router: AccountRouter,
    private val coroutineScope: CoroutineScope,
) : QrCodeExpiredPresentable.Presentation, CoroutineScope by coroutineScope {

    override val acknowledgeExpired: ActionAwaitableMixin.Presentation<String, Unit> = actionAwaitableMixinFactory.create()

    @OptIn(ExperimentalTime::class)
    override suspend fun showQrCodeExpired(validityPeriod: ValidityPeriod) {
        if (acknowledgeExpired.hasAlredyTriggered()) return

        val message = withContext(Dispatchers.Default) {
            val validityPeriodMillis = validityPeriod.period.millis
            val durationFormatted = resourceManager.formatDuration(validityPeriodMillis.milliseconds, estimated = false)
            resourceManager.getString(R.string.account_parity_signer_sign_qr_code_expired_descrition, durationFormatted)
        }

        acknowledgeExpired.awaitAction(message)

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
