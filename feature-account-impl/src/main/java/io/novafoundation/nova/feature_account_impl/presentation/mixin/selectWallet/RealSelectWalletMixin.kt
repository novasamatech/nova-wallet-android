package io.novafoundation.nova.feature_account_impl.presentation.mixin.selectWallet

import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountWithTotalBalance
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletRequester
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectedWalletModel
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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
) : SelectWalletMixin.Factory {

    override fun create(coroutineScope: CoroutineScope): SelectWalletMixin {
        return RealSelectWalletMixin(
            coroutineScope = coroutineScope,
            accountRepository = accountRepository,
            accountGroupingInteractor = accountGroupingInteractor,
            walletUiUseCase = walletUiUseCase,
            requester = requester
        )
    }
}

internal class RealSelectWalletMixin(
    coroutineScope: CoroutineScope,
    private val accountRepository: AccountRepository,
    private val accountGroupingInteractor: MetaAccountGroupingInteractor,
    private val walletUiUseCase: WalletUiUseCase,
    private val requester: SelectWalletRequester,
) : SelectWalletMixin,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val selectedMetaId = requester.responseFlow
        .map { it.newMetaId }
        .onStart { emit(accountRepository.getSelectedMetaAccount().id) }
        .shareInBackground()

    private val metaAccountWithBalanceFlow = selectedMetaId.flatMapLatest { metaId ->
        accountGroupingInteractor.metaAccountWithTotalBalanceFlow(metaId)
    }.shareInBackground()

    override val selectedMetaAccountFlow: Flow<MetaAccount> = metaAccountWithBalanceFlow
        .map { it.metaAccount }
        .shareInBackground()

    override val selectedWalletModelFlow: Flow<SelectedWalletModel> = metaAccountWithBalanceFlow
        .map(::mapSelectedMetaAccountToUi)
        .shareInBackground()

    override fun walletSelectorClicked() {
        launch {
            val currentMetaId = selectedMetaId.first()
            requester.openRequest(SelectWalletCommunicator.Payload(currentMetaId))
        }
    }

    private suspend fun mapSelectedMetaAccountToUi(metaAccountWithTotalBalance: MetaAccountWithTotalBalance): SelectedWalletModel {
        return with(metaAccountWithTotalBalance) {
            SelectedWalletModel(
                title = metaAccount.name,
                subtitle = totalBalance.formatAsCurrency(currency),
                icon = walletUiUseCase.walletIcon(metaAccount)
            )
        }
    }
}
