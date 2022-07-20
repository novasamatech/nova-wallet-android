package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.systemCall.ScanQrCodeCall
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.common.utils.systemCall.onSystemCallFailure
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.AddressInputSpec
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.AddressInputSpecProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.EVMSpecProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.SingleChainSpecProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.SubstrateSpecProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.myselfBehavior.CrossChainOnlyBehaviorProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.myselfBehavior.MyselfBehavior
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.myselfBehavior.MyselfBehaviorProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.myselfBehavior.NoMyselfBehaviorProvider
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
        inputSpecProvider: AddressInputSpecProvider,
        myselfBehaviorProvider: MyselfBehaviorProvider,
        errorDisplayer: (cause: String) -> Unit,
        coroutineScope: CoroutineScope
    ): AddressInputMixin = AddressInputMixinProvider(
        specProvider = inputSpecProvider,
        myselfBehaviorProvider = myselfBehaviorProvider,
        systemCallExecutor = systemCallExecutor,
        clipboardManager = clipboardManager,
        qrSharingFactory = qrSharingFactory,
        resourceManager = resourceManager,
        errorDisplayer = errorDisplayer,
        coroutineScope = coroutineScope
    )

    // address input

    fun singleChainInputSpec(
        destinationChainFlow: Flow<Chain>
    ): AddressInputSpecProvider = SingleChainSpecProvider(
        addressIconGenerator = addressIconGenerator,
        targetChain = destinationChainFlow
    )

    fun substrateInputSpec(): AddressInputSpecProvider = SubstrateSpecProvider(addressIconGenerator)

    fun evmInputSpec(): AddressInputSpecProvider = EVMSpecProvider(addressIconGenerator)

    // myself behavior

    fun crossChainOnlyMyself(
        originChain: Deferred<Chain>,
        destinationChainFlow: Flow<Chain>
    ): MyselfBehaviorProvider = CrossChainOnlyBehaviorProvider(
        accountUseCase = accountUseCase,
        originChain = originChain,
        destinationChain = destinationChainFlow
    )

    fun noMyself(): MyselfBehaviorProvider = NoMyselfBehaviorProvider()
}

class AddressInputMixinProvider(
    private val specProvider: AddressInputSpecProvider,
    private val myselfBehaviorProvider: MyselfBehaviorProvider,
    private val systemCallExecutor: SystemCallExecutor,
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

    override suspend fun getInputSpec(): AddressInputSpec {
        return specProvider.spec.first()
    }

    override val state = combine(myselfBehaviorProvider.behavior, specProvider.spec, inputFlow, clipboardFlow, ::createState)
        .shareInBackground()

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
                val spec = specProvider.spec.first()

                qrSharingFactory.create(spec::isValidAddress).decode(it).address
            }.onSuccess { address ->
                inputFlow.value = address
            }.onSystemCallFailure {
                errorDisplayer(resourceManager.getString(R.string.invoice_scan_error_no_info))
            }
        }
    }

    override fun myselfClicked() {
        launch {
            val myself = myselfBehaviorProvider.behavior.first().myself() ?: return@launch

            inputFlow.value = myself
        }
    }

    private suspend fun createState(
        myselfBehavior: MyselfBehavior,
        inputSpec: AddressInputSpec,
        input: String,
        clipboard: String?
    ): AddressInputState {
        val iconState = inputSpec.generateIcon(input)
            .fold(
                onSuccess = { AddressInputState.IdenticonState.Address(it) },
                onFailure = { AddressInputState.IdenticonState.Placeholder }
            )

        return AddressInputState(
            iconState = iconState,
            pasteShown = input.isEmpty() && clipboard != null,
            scanShown = input.isEmpty(),
            clearShown = input.isNotEmpty(),
            myselfShown = input.isEmpty() && myselfBehavior.myselfAvailable()
        )
    }
}
