package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.defaultSubstrateAddress
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.DappSignExtrinsicInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator.Response
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayload
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.maybeSignExtrinsic
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DAppSignViewModel(
    private val router: DAppRouter,
    private val responder: DAppSignResponder,
    private val interactor: DappSignExtrinsicInteractor,
    private val commonInteractor: DappInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val chainRegistry: ChainRegistry,
    private val payload: DAppSignPayload,
    selectedAccountUseCase: SelectedAccountUseCase,
    tokenUseCase: TokenUseCase?,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory
) : BaseViewModel(), WithFeeLoaderMixin {

    override val feeLoaderMixin: FeeLoaderMixin.Presentation? = tokenUseCase?.let {
        feeLoaderMixinFactory.create(tokenUseCase.currentTokenFlow())
    }

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .share()

    val walletModel = selectedAccount.map {
        addressIconGenerator.createAddressModel(it.defaultSubstrateAddress, AddressIconGenerator.SIZE_MEDIUM, it.name)
    }

    val requestedAccountModel = selectedAccount.map {
        addressIconGenerator.createAddressModel(payload.signerPayload.address, AddressIconGenerator.SIZE_MEDIUM, null)
    }
        .inBackground()
        .share()

    val maybeChainUi = flowOf {
        payload.signerPayload.maybeSignExtrinsic()?.genesisHash?.let {
            chainRegistry.getChain(it)
        }
    }
        .share()

    val dAppInfo = flowOf { commonInteractor.getDAppInfo(payload.dappUrl) }
        .inBackground()
        .share()

    init {
        maybeLoadFee()
    }

    fun cancelled() = rejectClicked()

    fun rejectClicked() {
        responder.respond(Response.Rejected(payload.requestId))
        router.back()
    }

    fun acceptClicked() = launch {
        val response = interactor.buildSignature(payload.signerPayload)
            .fold(
                onSuccess = { Response.Signed(payload.requestId, it) },
                onFailure = {
                    it.printStackTrace()

                    Response.SigningFailed(payload.requestId)
                }
            )

        responder.respond(response)
        router.back()
    }

    private fun maybeLoadFee() {
        if (payload.signerPayload is SignerPayload.Json) {
            feeLoaderMixin!!.loadFee(
                coroutineScope = this,
                feeConstructor = { interactor.calculateFee(payload.signerPayload) },
                onRetryCancelled = {}
            )
        }
    }


    fun detailsClicked() {
        launch {
            val extrinsicContent = interactor.readableSignContent(payload.signerPayload)

            router.openExtrinsicDetails(extrinsicContent)
        }
    }
}
