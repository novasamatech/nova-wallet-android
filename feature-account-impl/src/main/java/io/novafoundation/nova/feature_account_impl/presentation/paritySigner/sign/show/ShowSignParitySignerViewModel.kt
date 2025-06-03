package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.SharedState
import io.novafoundation.nova.common.utils.cycleMultiple
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.getOrThrow
import io.novafoundation.nova.common.utils.mediatorLiveData
import io.novafoundation.nova.common.utils.reversed
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.updateFrom
import io.novafoundation.nova.feature_account_api.data.signer.SeparateFlowSignerState
import io.novafoundation.nova.feature_account_api.data.signer.SignerPayload
import io.novafoundation.nova.feature_account_api.data.signer.accountId
import io.novafoundation.nova.feature_account_api.data.signer.chainId
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.formatWithPolkadotVaultLabel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.sign.cancelled
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignCommunicator
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show.ParitySignerSignMode
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show.ShowSignParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.common.QrCodeExpiredPresentableFactory
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model.ScanSignParitySignerPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model.mapValidityPeriodToParcel
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.extrinsic.ValidityPeriod
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

private val MODE_TO_INDEX = mapOf(
    ParitySignerSignMode.WITH_METADATA_PROOF to 0,
    ParitySignerSignMode.LEGACY to 1
)

private val INDEX_TO_MODE = MODE_TO_INDEX.reversed()

class ShowSignParitySignerViewModel(
    private val router: AccountRouter,
    private val interactor: ShowSignParitySignerInteractor,
    private val signSharedState: SharedState<SeparateFlowSignerState>,
    private val qrCodeGenerator: QrCodeGenerator,
    private val responder: PolkadotVaultVariantSignCommunicator,
    private val payload: ShowSignParitySignerPayload,
    private val chainRegistry: ChainRegistry,
    private val addressIconGenerator: AddressIconGenerator,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val externalActions: ExternalActions.Presentation,
    private val qrCodeExpiredPresentableFactory: QrCodeExpiredPresentableFactory,
    private val extrinsicValidityUseCase: ExtrinsicValidityUseCase,
    private val resourceManager: ResourceManager,
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
) : BaseViewModel(), ExternalActions by externalActions, Browserable {

    private val request = payload.request

    override val openBrowserEvent = mediatorLiveData { updateFrom(externalActions.openBrowserEvent) }

    val qrCodeExpiredPresentable = qrCodeExpiredPresentableFactory.create(request, payload.polkadotVaultVariant)

    val chain = flowOf {
        val signPayload = signSharedState.getOrThrow()
        val chainId = signPayload.payload.chainId()

        chainRegistry.getChain(chainId)
    }.shareInBackground()

    val supportsMultipleSigningModes = polkadotVaultVariantConfigProvider.variantConfigFor(payload.polkadotVaultVariant)
        .sign.supportsProofSigning

    val selectedSigningModeIndex = singleReplaySharedFlow<Int>()

    private val selectedSigningMode = selectedSigningModeIndex.map { INDEX_TO_MODE.getValue(it) }
        .shareInBackground()

    val qrCodeSequence = selectedSigningMode.mapLatest {
        val signPayload = signSharedState.getOrThrow()

        val frames = interactor.qrCodeContent(signPayload.payload, it).frames

        frames.map { qrCodeGenerator.generateQrBitmap(it) }.cycleMultiple()
    }.shareInBackground()

    val addressModel = chain.map { chain ->
        val signPayload = signSharedState.getOrThrow()

        addressIconGenerator.createAccountAddressModel(chain, signPayload.payload.accountId(), addressDisplayUseCase)
    }.shareInBackground()

    val validityPeriod = flowOf { determineSigningValidityPeriod() }
        .shareInBackground()

    val title = resourceManager.formatWithPolkadotVaultLabel(R.string.account_parity_signer_sign_title, payload.polkadotVaultVariant)

    val errorButtonLabel = resourceManager.formatWithPolkadotVaultLabel(R.string.account_parity_signer_sign_have_error, payload.polkadotVaultVariant)

    init {
        setInitialSigningMode()
    }

    fun backClicked() {
        responder.respond(request.cancelled())

        router.back()
    }

    fun continueClicked() = launch {
        val validityPeriodParcel = validityPeriod.first()?.let(::mapValidityPeriodToParcel)
        val payload = ScanSignParitySignerPayload(request, validityPeriodParcel, payload.polkadotVaultVariant)

        router.openScanParitySignerSignature(payload)
    }

    fun troublesClicked() = launch {
        val variantConfig = polkadotVaultVariantConfigProvider.variantConfigFor(payload.polkadotVaultVariant)
        openBrowserEvent.value = variantConfig.sign.troubleShootingLink.event()
    }

    fun timerFinished() {
        launch {
            qrCodeExpiredPresentable.showQrCodeExpired(validityPeriod.first()!!)
        }
    }

    fun addressClicked() = launch {
        val address = addressModel.first().address
        val chain = chain.first()

        externalActions.showAddressActions(address, chain)
    }

    private suspend fun determineSigningValidityPeriod(): ValidityPeriod? {
        return when (val payload = signSharedState.getOrThrow().payload) {
            is SignerPayload.Extrinsic -> extrinsicValidityUseCase.extrinsicValidityPeriod(payload.extrinsic)
            is SignerPayload.Raw -> null
        }
    }

    private fun setInitialSigningMode() {
        val mode = if (supportsMultipleSigningModes) {
            ParitySignerSignMode.WITH_METADATA_PROOF
        } else {
            ParitySignerSignMode.LEGACY
        }
        val modeIndex = MODE_TO_INDEX.getValue(mode)

        launch { selectedSigningModeIndex.emit(modeIndex) }
    }
}
