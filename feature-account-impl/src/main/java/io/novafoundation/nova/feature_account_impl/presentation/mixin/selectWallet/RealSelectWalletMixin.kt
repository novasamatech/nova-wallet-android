package io.novafoundation.nova.feature_account_impl.presentation.mixin.selectWallet

import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountListingItem
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletMixin.Factory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletMixin.InitialSelection
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletMixin.SelectionParams
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletRequester
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectedWalletModel
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

internal class RealRealSelectWalletMixinFactory(
    private val accountRepository: AccountRepository,
    private val accountGroupingInteractor: MetaAccountGroupingInteractor,
    private val walletUiUseCase: WalletUiUseCase,
    private val requester: SelectWalletRequester,
) : Factory {

    override fun create(
        coroutineScope: CoroutineScope,
        selectionParams: suspend () -> SelectionParams
    ): SelectWalletMixin {
        return RealSelectWalletMixin(
            coroutineScope = coroutineScope,
            accountRepository = accountRepository,
            accountGroupingInteractor = accountGroupingInteractor,
            walletUiUseCase = walletUiUseCase,
            requester = requester,
            selectionParamsAsync = selectionParams
        )
    }
}

internal class RealSelectWalletMixin(
    coroutineScope: CoroutineScope,
    private val accountRepository: AccountRepository,
    private val accountGroupingInteractor: MetaAccountGroupingInteractor,
    private val walletUiUseCase: WalletUiUseCase,
    private val requester: SelectWalletRequester,
    private val selectionParamsAsync: suspend () -> SelectionParams
) : SelectWalletMixin,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val selectionParams = flowOf { selectionParamsAsync() }
        .shareInBackground()

    private val selectableMetaId: Flow<Long> = requester.responseFlow
        .map { it.newMetaId }

    private val selectedMetaId = selectionParams.flatMapLatest { selectionParams ->
        selectionParams.metaIdChanges()
            .onStart { emit(selectionParams.initialMetaId()) }
    }
        .shareInBackground()

    private fun nonSelectableMetaId(): Flow<Long> = emptyFlow()

    private val metaAccountWithBalanceFlow = selectedMetaId.flatMapLatest { metaId ->
        accountGroupingInteractor.metaAccountWithTotalBalanceFlow(metaId)
    }.shareInBackground()

    override val selectedMetaAccountFlow: Flow<MetaAccount> = metaAccountWithBalanceFlow
        .map { it.metaAccount }
        .shareInBackground()

    override val selectedWalletModelFlow: Flow<SelectedWalletModel> = combine(
        metaAccountWithBalanceFlow,
        selectionParams,
        ::mapSelectedMetaAccountToUi
    )
        .shareInBackground()

    override fun walletSelectorClicked() {
        launch {
            val currentMetaId = selectedMetaId.first()
            requester.openRequest(SelectWalletCommunicator.Payload(currentMetaId))
        }
    }

    private suspend fun mapSelectedMetaAccountToUi(
        metaAccountListingItem: MetaAccountListingItem,
        selectionParams: SelectionParams
    ): SelectedWalletModel {
        return with(metaAccountListingItem) {
            SelectedWalletModel(
                title = metaAccount.name,
                subtitle = totalBalance.formatAsCurrency(currency),
                icon = walletUiUseCase.walletIcon(metaAccount),
                selectionAllowed = selectionParams.selectionAllowed
            )
        }
    }

    private suspend fun SelectionParams.initialMetaId(): Long = when (val selection = initialSelection) {
        InitialSelection.ActiveWallet -> accountRepository.getSelectedMetaAccount().id
        is InitialSelection.SpecificWallet -> selection.metaId
    }

    private fun SelectionParams.metaIdChanges() = if (selectionAllowed) {
        selectableMetaId
    } else {
        nonSelectableMetaId()
    }
}
