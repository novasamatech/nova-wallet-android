package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountWithTotalBalance
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.MetaAccountTypePresentationMapper
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class MetaAccountWithBalanceListingMixinFactory(
    private val walletUiUseCase: WalletUiUseCase,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    private val proxyFormatter: ProxyFormatter,
    private val resourceManager: ResourceManager
) {

    fun create(
        coroutineScope: CoroutineScope,
        showUpdatedMetaAccountsBadge: Boolean = true,
        metaAccountSelectedFlow: Flow<SelectedMetaAccountState> = flowOf { SelectedMetaAccountState.CurrentlySelected }
    ): MetaAccountListingMixin {
        return MetaAccountWithBalanceListingMixin(
            walletUiUseCase = walletUiUseCase,
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            coroutineScope = coroutineScope,
            metaAccountSelectedFlow = metaAccountSelectedFlow,
            accountTypePresentationMapper = accountTypePresentationMapper,
            proxyFormatter = proxyFormatter,
            resourceManager = resourceManager,
            showUpdatedMetaAccountsBadge = showUpdatedMetaAccountsBadge
        )
    }
}

private class MetaAccountWithBalanceListingMixin(
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val walletUiUseCase: WalletUiUseCase,
    private val metaAccountSelectedFlow: Flow<SelectedMetaAccountState>,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    private val proxyFormatter: ProxyFormatter,
    private val resourceManager: ResourceManager,
    private val showUpdatedMetaAccountsBadge: Boolean,
    coroutineScope: CoroutineScope,
) : MetaAccountListingMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val metaAccountsFlow = combine(
        metaAccountGroupingInteractor.metaAccountsWithTotalBalanceFlow(),
        metaAccountSelectedFlow
    ) { groupedList, selected ->
        groupedList.toListWithHeaders(
            keyMapper = { type, _ -> accountTypePresentationMapper.mapMetaAccountTypeToUi(type) },
            valueMapper = { mapMetaAccountToUi(it, selected) }
        )
    }
        .shareInBackground()

    private suspend fun mapMetaAccountToUi(metaAccountWithBalance: MetaAccountWithTotalBalance, selected: SelectedMetaAccountState) =
        with(metaAccountWithBalance) {
            AccountUi(
                id = metaAccount.id,
                title = metaAccount.name,
                subtitle = mapSubtitle(this),
                isSelected = selected.isSelected(metaAccount),
                isEditable = metaAccount.isEditable(),
                isClickable = true,
                picture = walletUiUseCase.walletIcon(metaAccount),
                chainIconUrl = proxyChain?.icon,
                updateIndicator = hasUpdates && showUpdatedMetaAccountsBadge,
                subtitleIconRes = null
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

        return proxyFormatter.mapProxyMetaAccountSubtitle(
            proxyMetaAccount.name,
            proxyFormatter.makeAccountDrawable(proxyMetaAccount),
            proxy
        )
    }

    private fun MetaAccount.isEditable(): Boolean {
        return when (type) {
            LightMetaAccount.Type.SECRETS,
            LightMetaAccount.Type.WATCH_ONLY,
            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.LEDGER,
            LightMetaAccount.Type.POLKADOT_VAULT -> true

            LightMetaAccount.Type.PROXIED -> false
        }
    }

    private fun MetaAccountWithTotalBalance.formattedTotalBalance(): String {
        return totalBalance.formatAsCurrency(currency)
    }
}
