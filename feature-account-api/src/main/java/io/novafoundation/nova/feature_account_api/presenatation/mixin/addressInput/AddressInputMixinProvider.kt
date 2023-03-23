package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.loadedNothing
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
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.qr.MultiChainQrSharingFactory
import io.novafoundation.nova.web3names.domain.networking.Web3NamesInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
        myselfBehaviorProvider: MyselfBehaviorProvider,
        accountIdentifierProvider: AccountIdentifierProvider?,
        errorDisplayer: (cause: String) -> Unit,
        showAccountEvent: ((address: String) -> Unit)?,
        coroutineScope: CoroutineScope
    ): AddressInputMixin = AddressInputMixinProvider(
        specProvider = inputSpecProvider,
        myselfBehaviorProvider = myselfBehaviorProvider,
        systemCallExecutor = systemCallExecutor,
        clipboardManager = clipboardManager,
        qrSharingFactory = qrSharingFactory,
        resourceManager = resourceManager,
        errorDisplayer = errorDisplayer,
        coroutineScope = coroutineScope,
        accountIdentifierProvider = accountIdentifierProvider,
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
        originChain: Deferred<Chain>,
        destinationChainFlow: Flow<Chain>
    ): MyselfBehaviorProvider = CrossChainOnlyBehaviorProvider(
        accountUseCase = accountUseCase,
        originChain = originChain,
        destinationChain = destinationChainFlow
    )

    fun accountIdentifierProvider(
        destinationChainFlow: Flow<ChainWithAsset>,
        inputSpecProvider: AddressInputSpecProvider,
        coroutineScope: CoroutineScope
    ): AccountIdentifierProvider = Web3NameIdentifierProvider(
        web3NameInteractor = web3NamesInteractor,
        destinationChain = destinationChainFlow,
        addressInputSpecProvider = inputSpecProvider,
        coroutineScope = coroutineScope
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
    private val accountIdentifierProvider: AccountIdentifierProvider?,
    private val showAddressEventCallback: ((address: String) -> Unit)?,
    coroutineScope: CoroutineScope,
) : AddressInputMixin,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val clipboardFlow = clipboardManager.observePrimaryClip()
        .inBackground()
        .share()

    override val inputFlow = MutableStateFlow("")

    override val showExternalAccountsFlow = accountIdentifierProvider?.externalAccountsFlow
        ?.map { ExternalAccountsWithSelected(it, accountIdentifierProvider.getSelectedExternalAccount()) }
        ?: emptyFlow()

    override val selectedExternalIdentifierFlow = accountIdentifierProvider?.selectedExternalAccountFlow ?: flowOf(loadedNothing())

    override val state = combine(
        myselfBehaviorProvider.behavior,
        specProvider.spec,
        inputFlow,
        clipboardFlow,
        ::createState
    ).shareInBackground()

    init {
        accountIdentifierProvider?.init(inputFlow)
    }

    override suspend fun getInputSpec(): AddressInputSpec {
        return specProvider.spec.first()
    }

    override fun pasteClicked() {
        launch {
            inputFlow.value = withContext(Dispatchers.IO) {
                clipboardManager.getTextOrNull().orEmpty()
            }
            accountIdentifierProvider?.loadExternalAccounts(inputFlow.value)
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
        showAddressEventCallback?.let {
            val selectedAccount = accountIdentifierProvider?.getSelectedExternalAccount() ?: return
            if (accountIdentifierProvider.isValidExternalAccount(selectedAccount)) {
                showAddressEventCallback.invoke(selectedAccount.address)
            }
        }
    }

    override fun onInputFocusChanged() {
        coroutineScope.launch {
            accountIdentifierProvider?.loadExternalAccounts(inputFlow.value)
        }
    }

    override fun onKeyboardGone() {
        accountIdentifierProvider?.loadExternalAccounts(inputFlow.value)
    }

    override fun selectExternalAccount(it: ExternalAccount) {
        accountIdentifierProvider?.selectExternalAccount(it)
    }

    override fun getExternalAccountIdentifier(): String? {
        return accountIdentifierProvider?.getIdentifier()
    }

    override suspend fun getAddress(): String {
        val externalAddress = selectedExternalIdentifierFlow.first().dataOrNull?.address
        return externalAddress ?: inputFlow.value
    }

    override fun isValidExternalAccount(externalAccount: ExternalAccount): Boolean {
        return accountIdentifierProvider?.isValidExternalAccount(externalAccount) ?: false
    }

    override fun clearExtendedAccount() {
        accountIdentifierProvider?.selectExternalAccount(null)
    }

    private suspend fun createState(
        myselfBehavior: MyselfBehavior,
        inputSpec: AddressInputSpec,
        input: String,
        clipboard: String?
    ): AddressInputState {
        val icon = generateIcon(inputSpec, input)

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
