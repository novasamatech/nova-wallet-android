package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.set

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.domain.proxy.AddProxyInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressForTransactionRequester
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.transferableAmountModel
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.assetWithChain
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import io.novafoundation.nova.runtime.state.selectedChainFlow
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SetStakingProxyViewModel(
    addressInputMixinFactory: AddressInputMixinFactory,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val externalActions: ExternalActions.Presentation,
    private val interactor: StakingInteractor,
    private val accountRepository: AccountRepository,
    private val assetUseCase: ArbitraryAssetUseCase,
    private val resourceManager: ResourceManager,
    private val selectAddressRequester: SelectAddressForTransactionRequester,
    private val addProxyInteractor: AddProxyInteractor,
    private val validationExecutor: ValidationExecutor,
) : BaseViewModel(),
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
                coroutineScope = this@SetStakingProxyViewModel,
            ),
            errorDisplayer = this@SetStakingProxyViewModel::showError,
            showAccountEvent = this@SetStakingProxyViewModel::showAccountDetails,
            coroutineScope = this@SetStakingProxyViewModel,
        )
    }

    val isSelectAddressAvailable = flowOf { true }
        .shareInBackground()

    val proxyDeposit: Flow<AmountModel> = selectedAssetFlow
        .map { asset ->
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val chain = selectedAssetState.chain()
            val accountId = metaAccount.requireAccountIdIn(chain)
            val deposit = addProxyInteractor.calculateDepositForAddProxy(chain, accountId)
            mapAmountToAmountModel(deposit, asset)
        }
        .shareInBackground()

    val feeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(commissionAssetFlow)

    private val validationProgressFlow = MutableStateFlow(false)

    val continueButtonState = combine(
        addressInputMixin.inputFlow,
        validationProgressFlow
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

    fun selectRecipientWallet() {
        launch {
            val selectedAddress = addressInputMixin.inputFlow.value
            val chain = selectedAssetState.chain()
            val request = SelectAddressForTransactionRequester.Request(chain.id, chain.id, selectedAddress)
            selectAddressRequester.openRequest(request)
        }
    }

    fun onConfirmClick() {

    }

    private fun runFeeUpdate() {
        addressInputMixin.inputFlow.onEach {
            val chain = selectedAssetState.chain()
            val accountId = chain.accountIdOrNull(it)

            if (accountId == null) {
                feeMixin.setFee(null)
            } else {
                feeMixin.loadFee(
                    coroutineScope = this,
                    feeConstructor = { addProxyInteractor.estimateFee(chain, accountId, ProxyAccount.ProxyType.Staking) },
                    onRetryCancelled = {}
                )
            }
        }.launchIn(this)
    }

    private fun showAccountDetails(address: String) {
        launch {
            val chain = selectedAssetState.chain()
            externalActions.showExternalActions(ExternalActions.Type.Address(address), chain)
        }
    }
}
