package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.confirm

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyDepositWithQuantity
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.AddStakingProxyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.AddStakingProxyValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.AddStakingProxyValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.common.launchProxyDepositDescription
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.common.mapAddStakingProxyValidationFailureToUi
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import io.novafoundation.nova.runtime.state.selectedChainFlow
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmAddStakingProxyViewModel(
    private val router: StakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val payload: ConfirmAddStakingProxyPayload,
    private val accountRepository: AccountRepository,
    private val resourceManager: ResourceManager,
    private val externalActions: ExternalActions.Presentation,
    private val validationExecutor: ValidationExecutor,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val assetUseCase: ArbitraryAssetUseCase,
    private val addStakingProxyValidationSystem: AddStakingProxyValidationSystem,
    private val addStakingProxyInteractor: AddStakingProxyInteractor,
    private val walletUiUseCase: WalletUiUseCase,
    private val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
) : BaseViewModel(),
    DescriptionBottomSheetLauncher by descriptionBottomSheetLauncher,
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    private val selectedMetaAccountFlow = accountRepository.selectedMetaAccountFlow()
        .shareInBackground()

    private val chainFlow = selectedAssetState.selectedChainFlow()
        .shareInBackground()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val assetFlow = selectedAssetState.selectedAssetFlow()
        .flatMapLatest { assetUseCase.assetFlow(it) }
        .shareInBackground()

    private val decimalFeeFlow = flowOf { mapFeeFromParcel(payload.fee) }
        .shareInBackground()

    val chainModel = chainFlow.map { chain ->
        mapChainToUi(chain)
    }

    val walletUiFlow = selectedMetaAccountFlow.map { walletUiUseCase.walletUiFor(it) }

    val proxiedAccountModel = combine(selectedMetaAccountFlow, chainFlow) { metaAccount, chain ->
        val address = metaAccount.requireAddressIn(chain)

        generateAccountAddressModel(chain, address)
    }

    val proxyDeposit = assetFlow.map { asset ->
        mapAmountToAmountModel(payload.newProxyDeposit, asset)
    }

    val feeModelFlow = combine(assetFlow, decimalFeeFlow) { asset, decimalFee ->
        mapAmountToAmountModel(decimalFee.networkFee.amount, asset)
    }

    val proxyAccountModel = chainFlow.map { chain ->
        generateAccountAddressModel(chain, payload.proxyAddress)
    }

    val validationProgressFlow = MutableStateFlow(false)

    fun back() {
        router.back()
    }

    fun confirmClicked() = launch {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = selectedAssetState.chain()
        val validationPayload = AddStakingProxyValidationPayload(
            chain = chain,
            asset = assetFlow.first(),
            proxyAddress = payload.proxyAddress,
            proxiedAccountId = metaAccount.requireAccountIdIn(chain),
            fee = decimalFeeFlow.first(),
            depositWithQuantity = ProxyDepositWithQuantity(
                deposit = payload.newProxyDeposit,
                quantity = payload.newProxyQuantity
            )
        )

        validationExecutor.requireValid(
            validationSystem = addStakingProxyValidationSystem,
            payload = validationPayload,
            validationFailureTransformer = { mapAddStakingProxyValidationFailureToUi(resourceManager, it) },
            progressConsumer = validationProgressFlow.progressConsumer()
        ) {
            sendTransaction(it.chain, it.proxiedAccountId, it.chain.accountIdOf(it.proxyAddress))
        }
    }

    private fun sendTransaction(chain: Chain, proxiedAccount: AccountId, proxyAccount: AccountId) = launch {
        val result = addStakingProxyInteractor.addProxy(chain, proxiedAccount, proxyAccount)

        validationProgressFlow.value = false

        if (result.isSuccess) {
            router.returnToStakingMain()
        }
    }

    private suspend fun generateAccountAddressModel(chain: Chain, address: String) = addressIconGenerator.createAccountAddressModel(
        chain = chain,
        address = address,
    )

    fun proxiedAccountClicked() {
        launch {
            showExternalActions(proxiedAccountModel.first().address)
        }
    }

    fun depositClicked() {
        descriptionBottomSheetLauncher.launchProxyDepositDescription()
    }

    fun proxyAccountClicked() {
        showExternalActions(payload.proxyAddress)
    }

    private fun showExternalActions(address: String) = launch {
        externalActions.showExternalActions(ExternalActions.Type.Address(address), selectedAssetState.chain())
    }
}
