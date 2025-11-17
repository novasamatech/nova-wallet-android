package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.set

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.domain.filter.selectAddress.SelectAccountFilter
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.common.SelectWalletFilterPayload
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.AddStakingProxyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.AddStakingProxyValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.AddStakingProxyValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.confirm.ConfirmAddStakingProxyPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.common.launchProxyDepositDescription
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.common.mapAddStakingProxyValidationFailureToUi
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.assetWithChain
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import io.novafoundation.nova.runtime.state.selectedChainFlow
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AddStakingProxyViewModel(
    addressInputMixinFactory: AddressInputMixinFactory,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val externalActions: ExternalActions.Presentation,
    private val interactor: StakingInteractor,
    private val accountRepository: AccountRepository,
    private val assetUseCase: ArbitraryAssetUseCase,
    private val resourceManager: ResourceManager,
    private val addStakingProxyInteractor: AddStakingProxyInteractor,
    private val validationExecutor: ValidationExecutor,
    private val addStakingProxyValidationSystem: AddStakingProxyValidationSystem,
    private val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val stakingRouter: StakingRouter,
    private val selectAddressMixinFactory: SelectAddressMixin.Factory,
    private val getProxyRepository: GetProxyRepository,
    private val amountFormat: AmountFormatter
) : BaseViewModel(),
    DescriptionBottomSheetLauncher by descriptionBottomSheetLauncher,
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val commissionAssetFlow = selectedAssetState.selectedChainFlow()
        .flatMapLatest { assetUseCase.assetFlow(it.id, it.commissionAsset.id) }
        .shareInBackground()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedAssetFlow = selectedAssetState.selectedAssetFlow()
        .flatMapLatest { assetUseCase.assetFlow(it) }
        .shareInBackground()

    private val selectAddressPayloadFlow = flowOf {
        val selectedMetaAccount = accountRepository.getSelectedMetaAccount()
        val chain = selectedAssetState.chain()
        val filter = metaAccountsFilter(chain, selectedMetaAccount.requireAccountIdIn(chain))
        SelectAddressMixin.Payload(chain, filter)
    }

    val selectAddressMixin = selectAddressMixinFactory.create(this, selectAddressPayloadFlow, ::onAddressSelect)

    val titleFlow = selectedAssetFlow.map {
        resourceManager.getString(R.string.fragment_set_staking_proxy_title, it.token.configuration.symbol)
    }
        .shareInBackground()

    val addressInputMixin = with(addressInputMixinFactory) {
        val inputSpec = singleChainInputSpec(selectedAssetState.selectedChainFlow())

        create(
            inputSpecProvider = inputSpec,
            myselfBehaviorProvider = noMyself(),
            accountIdentifierProvider = web3nIdentifiers(
                destinationChainFlow = selectedAssetState.assetWithChain,
                inputSpecProvider = inputSpec,
                coroutineScope = this@AddStakingProxyViewModel,
            ),
            errorDisplayer = this@AddStakingProxyViewModel::showError,
            showAccountEvent = this@AddStakingProxyViewModel::showAccountDetails,
            coroutineScope = this@AddStakingProxyViewModel,
        )
    }

    val isSelectAddressAvailable = flowOf {
        val selectedMetaAccount = accountRepository.getSelectedMetaAccount()
        val chain = selectedAssetState.chain()
        val filter = metaAccountsFilter(chain, selectedMetaAccount.requireAccountIdIn(chain))
        metaAccountGroupingInteractor.hasAvailableMetaAccountsForChain(selectedAssetState.chainId(), filter)
    }
        .shareInBackground()

    private val proxyDepositDelta: Flow<Balance> = flowOf {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = selectedAssetState.chain()
        val accountId = metaAccount.requireAccountIdIn(chain)
        addStakingProxyInteractor.calculateDeltaDepositForAddProxy(chain, accountId)
    }
        .shareInBackground()

    val proxyDepositModel: Flow<AmountModel> = combine(proxyDepositDelta, selectedAssetFlow) { depositDelta, asset ->
        amountFormat.formatAmountToAmountModel(depositDelta, asset)
    }
        .shareInBackground()

    val feeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(commissionAssetFlow)

    private val _validationProgressFlow = MutableStateFlow(false)

    val continueButtonState = combine(
        addressInputMixin.inputFlow,
        _validationProgressFlow
    ) { addressInput, validationInProgress ->
        when {
            validationInProgress -> DescriptiveButtonState.Loading

            addressInput.isEmpty() -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_address))

            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }

    init {
        runFeeUpdate()
    }

    fun backClicked() {
        stakingRouter.back()
    }

    fun showProxyDepositDescription() {
        descriptionBottomSheetLauncher.launchProxyDepositDescription()
    }

    fun selectAuthorityWallet() {
        launch {
            val selectedAddress = addressInputMixin.getAddress()
            selectAddressMixin.openSelectAddress(selectedAddress)
        }
    }

    fun onConfirmClick() = launch {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = selectedAssetState.chain()
        val proxiedAccountId = metaAccount.requireAccountIdIn(chain)
        val validationPayload = AddStakingProxyValidationPayload(
            chain = chain,
            asset = selectedAssetFlow.first(),
            proxyAddress = addressInputMixin.getAddress(),
            proxiedAccountId = metaAccount.requireAccountIdIn(chain),
            fee = feeMixin.awaitFee(),
            deltaDeposit = proxyDepositDelta.first(),
            currentQuantity = getProxyRepository.getProxiesQuantity(chain.id, proxiedAccountId)
        )

        validationExecutor.requireValid(
            validationSystem = addStakingProxyValidationSystem,
            payload = validationPayload,
            validationFailureTransformer = { mapAddStakingProxyValidationFailureToUi(resourceManager, it) },
            progressConsumer = _validationProgressFlow.progressConsumer()
        ) {
            openConfirmScreen(it)
            _validationProgressFlow.value = false
        }
    }

    private fun openConfirmScreen(validationPayload: AddStakingProxyValidationPayload) {
        val screenPayload = validationPayload.run {
            ConfirmAddStakingProxyPayload(
                fee = mapFeeToParcel(validationPayload.fee),
                proxyAddress = proxyAddress,
                deltaDeposit = deltaDeposit,
                currentQuantity = currentQuantity
            )
        }
        stakingRouter.openConfirmAddStakingProxy(screenPayload)
    }

    private fun onAddressSelect(address: String) {
        addressInputMixin.inputFlow.value = address
    }

    private fun runFeeUpdate() {
        addressInputMixin.inputFlow.onEach {
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val chain = selectedAssetState.chain()

            feeMixin.loadFee(
                coroutineScope = this,
                feeConstructor = { addStakingProxyInteractor.estimateFee(chain, metaAccount.requireAccountIdIn(chain)) },
                onRetryCancelled = {}
            )
        }.launchIn(this)
    }

    private fun showAccountDetails(address: String) {
        launch {
            val chain = selectedAssetState.chain()
            externalActions.showAddressActions(address, chain)
        }
    }

    private suspend fun getMetaAccountsFilterPayload(chain: Chain, accountId: AccountId): SelectWalletFilterPayload.ExcludeMetaIds {
        val filteredMetaAccounts = accountRepository.getActiveMetaAccounts()
            .filter { it.accountIdIn(chain)?.intoKey() == accountId.intoKey() }
            .map { it.id }

        return SelectWalletFilterPayload.ExcludeMetaIds(filteredMetaAccounts)
    }

    private suspend fun metaAccountsFilter(chain: Chain, accountId: AccountId): SelectAccountFilter {
        val metaAccountsFilterPayload = getMetaAccountsFilterPayload(chain, accountId)

        return SelectAccountFilter.ExcludeMetaAccounts(
            metaAccountsFilterPayload.metaIds
        )
    }
}
