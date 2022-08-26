package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountWithTotalBalance
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class MetaAccountWithBalanceListingMixinFactory(
    private val walletUiUseCase: WalletUiUseCase,
    private val resourceManager: ResourceManager,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor
) {

    fun create(
        coroutineScope: CoroutineScope
    ): MetaAccountListingMixin {
        return MetaAccountWithBalanceListingMixin(
            walletUiUseCase = walletUiUseCase,
            resourceManager = resourceManager,
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            coroutineScope = coroutineScope
        )
    }
}

private class MetaAccountWithBalanceListingMixin(
    private val resourceManager: ResourceManager,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val walletUiUseCase: WalletUiUseCase,
    coroutineScope: CoroutineScope,
) : MetaAccountListingMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val metaAccountsFlow = metaAccountGroupingInteractor.metaAccountsWithTotalBalanceFlow().map { list ->
        list.toListWithHeaders(
            keyMapper = { mapMetaAccountTypeToUi(it, resourceManager) },
            valueMapper = { mapMetaAccountToUi(it) }
        )
    }
        .shareInBackground()

    private suspend fun mapMetaAccountToUi(metaAccount: MetaAccountWithTotalBalance) = with(metaAccount) {
        AccountUi(
            id = metaAccount.metaAccount.id,
            title = metaAccount.metaAccount.name,
            subtitle = totalBalance.formatAsCurrency(),
            isSelected = metaAccount.metaAccount.isSelected,
            isClickable = true,
            picture = walletUiUseCase.walletIcon(metaAccount.metaAccount),
            subtitleIconRes = null,
        )
    }
}
