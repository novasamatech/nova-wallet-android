package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.remove.RemoveStakingProxyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.remove.RemoveStakingProxyValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.remove.RemoveStakingProxyValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.common.mapRemoveStakingProxyValidationFailureToUi
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
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

class ConfirmRemoveStakingProxyViewModel(
    private val router: StakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val payload: ConfirmRemoveStakingProxyPayload,
    private val accountRepository: AccountRepository,
    private val resourceManager: ResourceManager,
    private val externalActions: ExternalActions.Presentation,
    private val validationExecutor: ValidationExecutor,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val assetUseCase: ArbitraryAssetUseCase,
    private val removeStakingProxyValidationSystem: RemoveStakingProxyValidationSystem,
    private val walletUiUseCase: WalletUiUseCase,
    private val removeStakingProxyInteractor: RemoveStakingProxyInteractor,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory
) : BaseViewModel(),
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

    val feeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(assetFlow)

    val chainModel = chainFlow.map { chain ->
        mapChainToUi(chain)
    }

    val walletUiFlow = selectedMetaAccountFlow.map { walletUiUseCase.walletUiFor(it) }

    val proxiedAccountModel = combine(selectedMetaAccountFlow, chainFlow) { metaAccount, chain ->
        val address = metaAccount.requireAddressIn(chain)

        generateAccountAddressModel(chain, address)
    }

    val proxyAccountModel = chainFlow.map { chain ->
        generateAccountAddressModel(chain, payload.proxyAddress)
    }

    val validationProgressFlow = MutableStateFlow(false)

    init {
        loadFee()
    }

    fun backClicked() {
        router.back()
    }

    fun proxiedAccountClicked() {
        launch {
            showExternalActions(proxiedAccountModel.first().address)
        }
    }

    fun proxyAccountClicked() {
        showExternalActions(payload.proxyAddress)
    }

    fun confirmClicked() = launch {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = selectedAssetState.chain()
        val validationPayload = RemoveStakingProxyValidationPayload(
            chain = chain,
            asset = assetFlow.first(),
            proxyAddress = payload.proxyAddress,
            proxiedAccountId = metaAccount.requireAccountIdIn(chain),
            fee = feeMixin.awaitDecimalFee()
        )

        validationExecutor.requireValid(
            validationSystem = removeStakingProxyValidationSystem,
            payload = validationPayload,
            validationFailureTransformer = { mapRemoveStakingProxyValidationFailureToUi(resourceManager, it) },
            progressConsumer = validationProgressFlow.progressConsumer()
        ) {
            sendTransaction(it.chain, it.proxiedAccountId, it.chain.accountIdOf(it.proxyAddress))
        }
    }

    private fun loadFee() {
        launch {
            val metaAccount = selectedMetaAccountFlow.first()
            val chain = selectedAssetState.chain()
            val proxiedAccountId = metaAccount.requireAccountIdIn(chain)

            feeMixin.loadFee(
                this,
                chain.id,
                feeConstructor = { removeStakingProxyInteractor.estimateFee(chain, proxiedAccountId) },
                onRetryCancelled = ::backClicked
            )
        }
    }

    private fun sendTransaction(chain: Chain, proxiedAccount: AccountId, proxyAccount: AccountId) = launch {
        val result = removeStakingProxyInteractor.removeProxy(chain, proxiedAccount, proxyAccount)

        validationProgressFlow.value = false

        result.onSuccess { router.returnToStakingMain() }
            .onFailure(::showError)
    }

    private suspend fun generateAccountAddressModel(chain: Chain, address: String) = addressIconGenerator.createAccountAddressModel(
        chain = chain,
        address = address,
    )

    private fun showExternalActions(address: String) = launch {
        externalActions.showExternalActions(ExternalActions.Type.Address(address), selectedAssetState.chain())
    }
}
