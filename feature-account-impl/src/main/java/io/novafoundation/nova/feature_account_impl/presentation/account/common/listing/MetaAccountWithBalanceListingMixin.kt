package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountWithTotalBalance
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class MetaAccountWithBalanceListingMixinFactory(
    private val walletUiUseCase: WalletUiUseCase,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
) {

    fun create(
        coroutineScope: CoroutineScope,
        isMetaAccountSelected: suspend (MetaAccount) -> Boolean = { it.isSelected },
    ): MetaAccountListingMixin {
        return MetaAccountWithBalanceListingMixin(
            walletUiUseCase = walletUiUseCase,
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            coroutineScope = coroutineScope,
            isMetaAccountSelected = isMetaAccountSelected,
            accountTypePresentationMapper = accountTypePresentationMapper
        )
    }
}

private class MetaAccountWithBalanceListingMixin(
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val walletUiUseCase: WalletUiUseCase,
    private val isMetaAccountSelected: suspend (MetaAccount) -> Boolean,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    coroutineScope: CoroutineScope,
) : MetaAccountListingMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val metaAccountsFlow = metaAccountGroupingInteractor.metaAccountsWithTotalBalanceFlow().map { list ->
        list.toListWithHeaders(
            keyMapper = { type, _ -> accountTypePresentationMapper.mapMetaAccountTypeToUi(type) },
            valueMapper = { mapMetaAccountToUi(it) }
        )
    }
        .shareInBackground()

    private suspend fun mapMetaAccountToUi(metaAccountWithBalance: MetaAccountWithTotalBalance) = with(metaAccountWithBalance) {
        AccountUi(
            id = metaAccountWithBalance.metaAccount.id,
            title = metaAccountWithBalance.metaAccount.name,
            subtitle = totalBalance.formatAsCurrency(metaAccountWithBalance.currency),
            isSelected = isMetaAccountSelected(metaAccountWithBalance.metaAccount),
            isClickable = true,
            picture = walletUiUseCase.walletIcon(metaAccountWithBalance.metaAccount),
            subtitleIconRes = null,
        )
    }
}
