package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.DAppSignInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator.Response
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DAppSignViewModel(
    private val router: DAppRouter,
    private val responder: DAppSignResponder,
    private val interactor: DAppSignInteractor,
    private val commonInteractor: DappInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val payload: DAppSignPayload,
    private val walletUiUseCase: WalletUiUseCase,
    selectedAccountUseCase: SelectedAccountUseCase,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory
) : BaseViewModel(), WithFeeLoaderMixin {

    override val feeLoaderMixin: FeeLoaderMixin.Presentation? = interactor.commissionTokenFlow()
        ?.let(feeLoaderMixinFactory::create)

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .share()

    val walletUi = walletUiUseCase.selectedWalletUiFlow(showAddressIcon = true)
        .shareInBackground()

    val requestedAccountModel = selectedAccount.map {
        interactor.createAccountAddressModel()
    }
        .shareInBackground()

    val maybeChainUi = flowOf {
        interactor.chainUi()
    }
        .shareInBackground()

    val dAppInfo = flowOf { commonInteractor.getDAppInfo(payload.dappUrl) }
        .shareInBackground()

    init {
        maybeLoadFee()
    }

    fun cancelled() = rejectClicked()

    fun rejectClicked() {
        responder.respond(Response.Rejected(payload.body.id))

        exit()
    }

    fun acceptClicked() = launch {
        val response = interactor.performOperation()

        responder.respond(response)

        exit()
    }

    private fun maybeLoadFee() {
        feeLoaderMixin?.loadFee(
            coroutineScope = this,
            feeConstructor = { interactor.calculateFee() },
            onRetryCancelled = {}
        )
    }

    fun detailsClicked() {
        launch {
            val extrinsicContent = interactor.readableOperationContent()

            router.openExtrinsicDetails(extrinsicContent)
        }
    }

    fun exit() {
        interactor.shutdown()
        router.back()
    }
}
