package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.systemCall.ScanQrCodeCall
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.common.utils.systemCall.onSystemCallFailure
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.AccountIdentifierProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.ExternalAccount
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.providers.EmptyAccountIdentifierProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.providers.Web3NameIdentifierProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.AddressInputSpec
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.AddressInputSpecProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.EVMSpecProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.SingleChainSpecProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.SubstrateSpecProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.myselfBehavior.CrossChainOnlyBehaviorProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.myselfBehavior.MyselfBehavior
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.myselfBehavior.MyselfBehaviorProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.myselfBehavior.NoMyselfBehaviorProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.qr.MultiChainQrSharingFactory
import io.novafoundation.nova.web3names.domain.networking.Web3NamesInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddressInputMixinFactory(
    private val addressIconGenerator: AddressIconGenerator,
    private val systemCallExecutor: SystemCallExecutor,
    private val clipboardManager: ClipboardManager,
    private val resourceManager: ResourceManager,
    private val qrSharingFactory: MultiChainQrSharingFactory,
    private val accountUseCase: SelectedAccountUseCase,
    private val web3NamesInteractor: Web3NamesInteractor
) {

    fun create(
        inputSpecProvider: AddressInputSpecProvider,
        myselfBehaviorProvider: MyselfBehaviorProvider = noMyself(),
        errorDisplayer: (cause: String) -> Unit,
        showAccountEvent: ((address: String) -> Unit)?,
        coroutineScope: CoroutineScope,
        accountIdentifierProvider: AccountIdentifierProvider.Factory = noAccountIdentifiers(),
    ): AddressInputMixin = AddressInputMixinProvider(
        specProvider = inputSpecProvider,
        myselfBehaviorProvider = myselfBehaviorProvider,
        systemCallExecutor = systemCallExecutor,
        clipboardManager = clipboardManager,
        qrSharingFactory = qrSharingFactory,
        resourceManager = resourceManager,
        errorDisplayer = errorDisplayer,
        coroutineScope = coroutineScope,
        accountIdentifierProviderFactory = accountIdentifierProvider,
        showAddressEventCallback = showAccountEvent
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
        originChain: Flow<Chain>,
        destinationChainFlow: Flow<Chain>
    ): MyselfBehaviorProvider = CrossChainOnlyBehaviorProvider(
        accountUseCase = accountUseCase,
        originChain = originChain,
        destinationChain = destinationChainFlow
    )

    // external accounts

    fun noAccountIdentifiers() = AccountIdentifierProvider.Factory { EmptyAccountIdentifierProvider() }

    fun web3nIdentifiers(
        destinationChainFlow: Flow<ChainWithAsset>,
        inputSpecProvider: AddressInputSpecProvider,
        coroutineScope: CoroutineScope,
    ) = AccountIdentifierProvider.Factory { input ->
        Web3NameIdentifierProvider(
            web3NameInteractor = web3NamesInteractor,
            destinationChain = destinationChainFlow,
            addressInputSpecProvider = inputSpecProvider,
            coroutineScope = coroutineScope,
            input = input,
            resourceManager = resourceManager
        )
    }

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
    private val showAddressEventCallback: ((address: String) -> Unit)?,
    private val accountIdentifierProviderFactory: AccountIdentifierProvider.Factory,
    coroutineScope: CoroutineScope,
) : AddressInputMixin,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val clipboardFlow = clipboardManager.observePrimaryClip()
        .inBackground()
        .share()

    override val inputFlow = MutableStateFlow("")

    private val accountIdentifierProvider = accountIdentifierProviderFactory.create(inputFlow)

    override val externalIdentifierEventLiveData = accountIdentifierProvider.eventsLiveData

    override val selectedExternalAccountFlow = accountIdentifierProvider.selectedExternalAccountFlow

    override val state = combine(
        myselfBehaviorProvider.behavior,
        specProvider.spec,
        accountIdentifierProvider.selectedExternalAccountFlow,
        inputFlow,
        clipboardFlow,
        ::createState
    ).shareInBackground()

    init {
        resetIdentifierInputOnSpecChange()
    }

    override suspend fun getInputSpec(): AddressInputSpec {
        return specProvider.spec.first()
    }

    override fun pasteClicked() {
        launch {
            inputFlow.value = withContext(Dispatchers.IO) {
                clipboardManager.getTextOrNull().orEmpty()
            }
            accountIdentifierProvider.loadExternalAccounts(inputFlow.value)
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

    override fun selectedExternalAddressClicked() {
        if (showAddressEventCallback == null) return

        launch {
            val selectedAccount = selectedExternalAccountFlow.first().dataOrNull
            if (selectedAccount != null && selectedAccount.isValid) {
                showAddressEventCallback.invoke(selectedAccount.address)
            }
        }
    }

    override fun loadExternalIdentifiers() {
        accountIdentifierProvider.loadExternalAccounts(inputFlow.value)
    }

    override fun selectExternalAccount(externalAccount: ExternalAccount) {
        accountIdentifierProvider.selectExternalAccount(externalAccount)
    }

    override suspend fun getAddress(): String {
        val externalAddress = selectedExternalAccountFlow.first().dataOrNull?.address
        return externalAddress ?: inputFlow.value
    }

    override fun clearExtendedAccount() {
        accountIdentifierProvider.selectExternalAccount(null)
    }

    private fun resetIdentifierInputOnSpecChange() {
        specProvider.spec.onEach {
            val currentInput = inputFlow.value

            if (accountIdentifierProvider.isIdentifierValid(currentInput)) {
                inputFlow.value = ""
            }
        }.launchIn(this)
    }

    private suspend fun createState(
        myselfBehavior: MyselfBehavior,
        inputSpec: AddressInputSpec,
        externalAccount: ExtendedLoadingState<ExternalAccount?>,
        input: String,
        clipboard: String?
    ): AddressInputState {
        val icon = externalAccount.dataOrNull?.icon ?: generateIcon(inputSpec, input)

        return AddressInputState(
            iconState = icon,
            pasteShown = input.isEmpty() && clipboard != null,
            scanShown = input.isEmpty(),
            clearShown = input.isNotEmpty(),
            myselfShown = input.isEmpty() && myselfBehavior.myselfAvailable()
        )
    }

    private suspend fun generateIcon(
        inputSpec: AddressInputSpec,
        input: String
    ): AddressInputState.IdenticonState {
        return inputSpec.generateIcon(input).toIdenticonState()
    }
}
