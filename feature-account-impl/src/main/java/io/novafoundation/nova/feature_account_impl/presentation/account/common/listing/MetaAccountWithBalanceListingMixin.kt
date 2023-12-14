package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountWithTotalBalance
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class MetaAccountWithBalanceListingMixinFactory(
    private val walletUiUseCase: WalletUiUseCase,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    private val proxyFormatter: ProxyFormatter,
    private val resourceManager: ResourceManager
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
            accountTypePresentationMapper = accountTypePresentationMapper,
            proxyFormatter = proxyFormatter,
            resourceManager = resourceManager
        )
    }
}

private class MetaAccountWithBalanceListingMixin(
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val walletUiUseCase: WalletUiUseCase,
    private val isMetaAccountSelected: suspend (MetaAccount) -> Boolean,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    private val proxyFormatter: ProxyFormatter,
    private val resourceManager: ResourceManager,
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
            id = metaAccount.id,
            title = metaAccount.name,
            subtitle = mapSubtitle(this),
            isSelected = isMetaAccountSelected(metaAccount),
            isClickable = true,
            picture = walletUiUseCase.walletIcon(metaAccount),
            chainIconUrl = proxyChain?.icon,
            subtitleIconRes = null,
            enabled = true,
            updateIndicator = hasUpdates
        )
    }

    private suspend fun mapSubtitle(
        metaAccountWithBalance: MetaAccountWithTotalBalance
    ): CharSequence = with(metaAccountWithBalance) {
        when (metaAccount.type) {
            LightMetaAccount.Type.SECRETS,
            LightMetaAccount.Type.WATCH_ONLY,
            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.LEDGER,
            LightMetaAccount.Type.POLKADOT_VAULT -> formattedTotalBalance()

            LightMetaAccount.Type.PROXIED -> mapProxyTypeToSubtitle(metaAccountWithBalance)
        }
    }

    private suspend fun mapProxyTypeToSubtitle(
        metaAccountWithBalance: MetaAccountWithTotalBalance
    ): CharSequence = with(metaAccountWithBalance) {
        val proxy = metaAccount.proxy ?: return formattedTotalBalance()
        val proxyMetaAccount = proxyMetaAccount ?: return formattedTotalBalance()

        return proxyFormatter.mapProxyMetaAccountSubtitle(proxyMetaAccount, proxy)
    }

    private fun MetaAccountWithTotalBalance.formattedTotalBalance(): String {
        return totalBalance.formatAsCurrency(currency)
    }
}
