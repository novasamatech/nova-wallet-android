package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountListingItem
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.MetaAccountTypePresentationMapper
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.chain.iconOrFallback
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class MetaAccountWithBalanceListingMixinFactory(
    private val walletUiUseCase: WalletUiUseCase,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    private val proxyFormatter: ProxyFormatter,
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

    private suspend fun mapMetaAccountToUi(metaAccountWithBalance: MetaAccountListingItem, selected: SelectedMetaAccountState) =
        with(metaAccountWithBalance) {
            AccountUi(
                id = metaAccount.id,
                title = metaAccount.name,
                subtitle = formatSubtitle(),
                isSelected = selected.isSelected(metaAccount),
                isEditable = metaAccount.isEditable(),
                isClickable = true,
                picture = walletUiUseCase.walletIcon(metaAccount),
                chainIcon = chainIcon(),
                updateIndicator = hasUpdates && showUpdatedMetaAccountsBadge,
                subtitleIconRes = null
            )
        }

    private fun MetaAccountListingItem.chainIcon(): Icon? {
        return when (this) {
            is MetaAccountListingItem.Proxied -> proxyChain.iconOrFallback()
            is MetaAccountListingItem.TotalBalance -> null
        }
    }

    private suspend fun MetaAccountListingItem.formatSubtitle(): CharSequence = when (this) {
        is MetaAccountListingItem.Proxied -> formatSubtitle()
        is MetaAccountListingItem.TotalBalance -> formatSubtitle()
    }

    private suspend fun MetaAccountListingItem.Proxied.formatSubtitle(): CharSequence {
        return proxyFormatter.mapProxyMetaAccountSubtitle(
            proxyMetaAccount.name,
            proxyFormatter.makeAccountDrawable(proxyMetaAccount),
            metaAccount.proxy
        )
    }

    private fun MetaAccountListingItem.TotalBalance.formatSubtitle(): String {
        return totalBalance.formatAsCurrency(currency)
    }

    private fun MetaAccount.isEditable(): Boolean {
        return when (type) {
            LightMetaAccount.Type.SECRETS,
            LightMetaAccount.Type.WATCH_ONLY,
            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.LEDGER_LEGACY,
            LightMetaAccount.Type.LEDGER,
            LightMetaAccount.Type.POLKADOT_VAULT -> true

            LightMetaAccount.Type.PROXIED -> false
        }
    }
}
