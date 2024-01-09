package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.set

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressRequester
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyDepositWithQuantity
import io.novafoundation.nova.feature_staking_api.data.proxy.AddStakingProxyRepository
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.AddStakingProxyValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.AddStakingProxyValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.common.mapAddStakingProxyValidationFailureToUi
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.assetWithChain
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import io.novafoundation.nova.runtime.state.selectedChainFlow
import jp.co.soramitsu.fearless_utils.runtime.AccountId
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
    private val selectAddressRequester: SelectAddressRequester,
    private val addStakingProxyRepository: AddStakingProxyRepository,
    private val validationExecutor: ValidationExecutor,
    private val addStakingProxyValidationSystem: AddStakingProxyValidationSystem,
    private val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
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

    val isSelectAddressAvailable = flowOf { true }
        .shareInBackground()

    private val proxyDeposit: Flow<ProxyDepositWithQuantity> = flowOf {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = selectedAssetState.chain()
        val accountId = metaAccount.requireAccountIdIn(chain)
        addStakingProxyRepository.calculateDepositForAddProxy(chain, accountId)
    }
        .shareInBackground()

    val proxyDepositModel: Flow<AmountModel> = combine(proxyDeposit, selectedAssetFlow) { depositwithQuantity, asset ->
        mapAmountToAmountModel(depositwithQuantity.deposit, asset)
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

        subscribeOnSelectAddress()
    }

    fun showProxyDepositDescription() {
        descriptionBottomSheetLauncher.launchDescriptionBottomSheet(
            titleRes = R.string.add_proxy_deposit_description_title,
            descriptionRes = R.string.add_proxy_deposit_description_message
        )
    }

    fun selectAuthorityWallet() {
        launch {
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val chain = selectedAssetState.chain()
            val proxiedAccountId = metaAccount.requireAccountIdIn(chain)
            val selectedAddress = addressInputMixin.inputFlow.value
            val filter = filterMetaAccountsWithSameAccountId(chain, proxiedAccountId)
            val request = SelectAddressRequester.Request(chain.id, selectedAddress, filter)
            selectAddressRequester.openRequest(request)
        }
    }

    fun onConfirmClick() = launch {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = selectedAssetState.chain()
        val validationPayload = AddStakingProxyValidationPayload(
            chain = chain,
            asset = selectedAssetFlow.first(),
            address = addressInputMixin.inputFlow.value,
            proxiedAccountId = metaAccount.requireAccountIdIn(chain),
            fee = feeMixin.awaitDecimalFee(),
            depositWithQuantity = proxyDeposit.first()
        )

        validationExecutor.requireValid(
            validationSystem = addStakingProxyValidationSystem,
            payload = validationPayload,
            validationFailureTransformer = { mapAddStakingProxyValidationFailureToUi(resourceManager, it) },
            progressConsumer = _validationProgressFlow.progressConsumer()
        ) {
            openConfirmScreen()
        }
    }

    private fun openConfirmScreen() {
        TODO()
    }

    private fun subscribeOnSelectAddress() {
        selectAddressRequester.responseFlow
            .onEach {
                addressInputMixin.inputFlow.value = it.selectedAddress
            }
            .launchIn(this)
    }

    private fun runFeeUpdate() {
        addressInputMixin.inputFlow.onEach {
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val chain = selectedAssetState.chain()

            feeMixin.loadFee(
                coroutineScope = this,
                feeConstructor = { addStakingProxyRepository.estimateFee(chain, metaAccount.requireAccountIdIn(chain)) },
                onRetryCancelled = {}
            )
        }.launchIn(this)
    }

    private fun showAccountDetails(address: String) {
        launch {
            val chain = selectedAssetState.chain()
            externalActions.showExternalActions(ExternalActions.Type.Address(address), chain)
        }
    }

    private suspend fun filterMetaAccountsWithSameAccountId(chain: Chain, accountId: AccountId): SelectAddressRequester.Request.Filter {
        val filteredMetaAccounts = accountRepository.activeMetaAccounts()
            .filter { it.accountIdIn(chain)?.intoKey() == accountId.intoKey() }
            .map { it.id }

        return SelectAddressRequester.Request.Filter.ExcludeMetaIds(filteredMetaAccounts)
    }
}
