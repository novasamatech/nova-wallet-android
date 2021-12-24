package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.DappSignExtrinsicInteractor
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayloadJSON
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.map

class DAppSignExtrinsicViewModel(
    private val router: DAppRouter,
    private val interactor: DappSignExtrinsicInteractor,
    private val commonInteractor: DappInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val chainRegistry: ChainRegistry,
    private val payload: DAppSignExtrinsicPayload,
    selectedAccountUseCase: SelectedAccountUseCase,
    tokenUseCase: TokenUseCase,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory
) : BaseViewModel(), WithFeeLoaderMixin {

    override val feeLoaderMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(tokenUseCase.currentTokenFlow())

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .share()

    private val chain = flowOf { chainRegistry.getChain(payload.signerPayloadJSON.genesisHash) }
        .inBackground()
        .share()

    val addressModel = selectedAccount.map {
        addressIconGenerator.createAddressModel(payload.signerPayloadJSON.address, AddressIconGenerator.SIZE_MEDIUM, it.name)
    }
        .inBackground()
        .share()

    val chainUi = chain.map { mapChainToUi(it) }
        .share()

    val dAppInfo = flowOf { commonInteractor.getDAppInfo(payload.dappUrl) }
        .inBackground()
        .share()

    init {
        loadFee()
    }

    private fun loadFee() = feeLoaderMixin.loadFee(
        coroutineScope = this,
        feeConstructor = { interactor.calculateFee(payload.signerPayloadJSON) },
        onRetryCancelled = {}
    )
}
