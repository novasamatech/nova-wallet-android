package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.common

import android.widget.TextView
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.polkadotVaultLabelFor
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.cancelled
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.runtime.extrinsic.ValidityPeriod
import io.novafoundation.nova.runtime.extrinsic.startExtrinsicValidityTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

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
    private val responder: SignInterScreenCommunicator,
) {

    fun create(
        request: SignInterScreenCommunicator.Request,
        variant: PolkadotVaultVariant
    ): QrCodeExpiredPresentable.Presentation = RealQrCodeExpiredPresentable(
        resourceManager = resourceManager,
        actionAwaitableMixinFactory = actionAwaitableMixinFactory,
        router = router,
        responder = responder,
        request = request,
        variant = variant
    )
}

private class RealQrCodeExpiredPresentable(
    private val resourceManager: ResourceManager,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val router: AccountRouter,
    private val responder: SignInterScreenCommunicator,
    private val request: SignInterScreenCommunicator.Request,
    private val variant: PolkadotVaultVariant,
) : QrCodeExpiredPresentable.Presentation {

    override val acknowledgeExpired: ActionAwaitableMixin.Presentation<String, Unit> = actionAwaitableMixinFactory.create()

    override suspend fun showQrCodeExpired(validityPeriod: ValidityPeriod) {
        val message = withContext(Dispatchers.Default) {
            val validityPeriodMillis = validityPeriod.period.millis
            val durationFormatted = resourceManager.formatDuration(validityPeriodMillis.milliseconds, estimated = false)
            val polkadotVaultVariantLabel = resourceManager.polkadotVaultLabelFor(variant)

            resourceManager.getString(R.string.account_parity_signer_sign_qr_code_expired_descrition, durationFormatted, polkadotVaultVariantLabel)
        }

        acknowledgeExpired.awaitAction(message)

        responder.respond(request.cancelled())
        router.finishParitySignerFlow()
    }
}

fun BaseFragment<*, *>.setupQrCodeExpiration(
    validityPeriodFlow: Flow<ValidityPeriod>,
    qrCodeExpiredPresentable: QrCodeExpiredPresentable,
    timerView: TextView,
    onTimerFinished: () -> Unit
) {
    validityPeriodFlow.observe { validityPeriod ->
        viewLifecycleOwner.startExtrinsicValidityTimer(
            validityPeriod = validityPeriod,
            timerFormat = R.string.account_parity_signer_sign_qr_code_valid_format,
            timerView = timerView,
            onTimerFinished = {
                onTimerFinished()

                timerView.setText(R.string.account_parity_signer_sign_qr_code_expired)
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
