package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.systemCall.ScanQrCodeCall
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.common.utils.systemCall.onSystemCallFailure
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.domain.model.hasAccountIn
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.qr.MultiChainQrSharingFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddressInputMixinFactory(
    private val addressIconGenerator: AddressIconGenerator,
    private val systemCallExecutor: SystemCallExecutor,
    private val clipboardManager: ClipboardManager,
    private val resourceManager: ResourceManager,
    private val qrSharingFactory: MultiChainQrSharingFactory,
    private val accountUseCase: SelectedAccountUseCase,
) {

    fun create(
        originChain: Deferred<Chain>,
        destinationChainFlow: Flow<Chain>,
        errorDisplayer: (cause: String) -> Unit,
        coroutineScope: CoroutineScope
    ): AddressInputMixin = AddressInputMixinProvider(
        originChain = originChain,
        destinationChainFlow = destinationChainFlow,
        accountUseCase = accountUseCase,
        addressIconGenerator = addressIconGenerator,
        systemCallExecutor = systemCallExecutor,
        clipboardManager = clipboardManager,
        qrSharingFactory = qrSharingFactory,
        resourceManager = resourceManager,
        errorDisplayer = errorDisplayer,
        coroutineScope = coroutineScope
    )
}

class AddressInputMixinProvider(
    private val originChain: Deferred<Chain>,
    private val destinationChainFlow: Flow<Chain>,
    private val addressIconGenerator: AddressIconGenerator,
    private val systemCallExecutor: SystemCallExecutor,
    private val accountUseCase: SelectedAccountUseCase,
    private val clipboardManager: ClipboardManager,
    private val qrSharingFactory: MultiChainQrSharingFactory,
    private val resourceManager: ResourceManager,
    private val errorDisplayer: (error: String) -> Unit,
    coroutineScope: CoroutineScope,
) : AddressInputMixin,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val clipboardFlow = clipboardManager.observePrimaryClip()
        .inBackground()
        .share()

    override val inputFlow = MutableStateFlow("")

    override val state = combine(destinationChainFlow, inputFlow, clipboardFlow, ::createState)
        .inBackground()
        .share()

    override fun pasteClicked() {
        launch {
            inputFlow.value = withContext(Dispatchers.IO) {
                clipboardManager.getFromClipboard().orEmpty()
            }
        }
    }

    override fun clearClicked() {
        inputFlow.value = ""
    }

    override fun scanClicked() {
        launch {
            systemCallExecutor.executeSystemCall(ScanQrCodeCall()).mapCatching {
                qrSharingFactory.create(destinationChainFlow.first()).decode(it).address
            }.onSuccess { address ->
                inputFlow.value = address
            }.onSystemCallFailure {
                errorDisplayer(resourceManager.getString(R.string.invoice_scan_error_no_info))
            }
        }
    }

    override fun myselfClicked() {
        launch {
            val destinationChain = destinationChainFlow.first()
            val metaAccount = accountUseCase.getSelectedMetaAccount()

            val address = metaAccount.addressIn(destinationChain) ?: return@launch

            inputFlow.value = address
        }
    }

    private suspend fun createState(chain: Chain, input: String, clipboard: String?): AddressInputState {
        val iconState = runCatching {
            val icon = addressIconGenerator.createAddressIcon(
                accountId = chain.accountIdOf(address = input),
                sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
                backgroundColorRes = AddressIconGenerator.BACKGROUND_TRANSPARENT
            )

            AddressInputState.IdenticonState.Address(icon)
        }.getOrDefault(AddressInputState.IdenticonState.Placeholder)

        return AddressInputState(
            iconState = iconState,
            pasteShown = input.isEmpty() && clipboard != null,
            scanShown = input.isEmpty(),
            clearShown = input.isNotEmpty(),
            myselfShown = input.isEmpty() && myselfAvailable(destination = chain)
        )
    }

    private suspend fun myselfAvailable(destination: Chain): Boolean {
        val originChain = originChain()
        val metaAccount = accountUseCase.getSelectedMetaAccount()

        return originChain.id != destination.id && metaAccount.hasAccountIn(destination)
    }
}
