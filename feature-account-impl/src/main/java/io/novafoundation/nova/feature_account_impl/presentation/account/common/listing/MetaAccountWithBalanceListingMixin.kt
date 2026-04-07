package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountListingItem
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.MetaAccountTypePresentationMapper
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.common.view.ChipLabelModel
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountChipGroupRvItem
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.chain.iconOrFallback
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.MultisigFormatter
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.ProxyFormatter
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MetaAccountWithBalanceListingMixinFactory(
    private val walletUiUseCase: WalletUiUseCase,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val accountInteractor: AccountInteractor,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    private val multisigFormatter: MultisigFormatter,
    private val proxyFormatter: ProxyFormatter,
) {

    fun create(
        coroutineScope: CoroutineScope,
        showUpdatedMetaAccountsBadge: Boolean = true,
        showFavouriteToggle: Boolean = false,
        searchQueryFlow: Flow<String> = flowOf { "" },
        metaAccountSelectedFlow: Flow<SelectedMetaAccountState> = flowOf { SelectedMetaAccountState.CurrentlySelected }
    ): MetaAccountListingMixin {
        return MetaAccountWithBalanceListingMixin(
            walletUiUseCase = walletUiUseCase,
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            accountInteractor = accountInteractor,
            chainRegistry = chainRegistry,
            resourceManager = resourceManager,
            coroutineScope = coroutineScope,
            metaAccountSelectedFlow = metaAccountSelectedFlow,
            accountTypePresentationMapper = accountTypePresentationMapper,
            proxyFormatter = proxyFormatter,
            showUpdatedMetaAccountsBadge = showUpdatedMetaAccountsBadge,
            showFavouriteToggle = showFavouriteToggle,
            searchQueryFlow = searchQueryFlow,
            multisigFormatter = multisigFormatter
        )
    }
}

private class MetaAccountWithBalanceListingMixin(
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val accountInteractor: AccountInteractor,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
    private val walletUiUseCase: WalletUiUseCase,
    private val metaAccountSelectedFlow: Flow<SelectedMetaAccountState>,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    private val proxyFormatter: ProxyFormatter,
    private val multisigFormatter: MultisigFormatter,
    private val showUpdatedMetaAccountsBadge: Boolean,
    private val showFavouriteToggle: Boolean,
    private val searchQueryFlow: Flow<String>,
    coroutineScope: CoroutineScope,
) : MetaAccountListingMixin, CoroutineScope by coroutineScope {

    override val metaAccountsFlow = combine(
        metaAccountGroupingInteractor.metaAccountsWithTotalBalanceFlow(),
        metaAccountSelectedFlow,
        accountInteractor.observeFavouriteMetaIds(),
        searchQueryFlow,
        // Project chains to (id → name) so we only re-emit when the names we
        // actually search against change. Avoids re-running the lambda on
        // every chain icon / runtime metadata refresh.
        chainRegistry.currentChains
            .map { chains -> chains.toSearchableProjection() }
            .distinctUntilChanged()
    ) { groupedList, selected, favouriteIds, query, chainsProjection ->
        val favSet = favouriteIds.toSet()
        val q = query.trim().lowercase()
        val metaById = groupedList.values
            .flatten()
            .associateBy { it.metaAccount.id }

        val baseItems = buildBaseItems(groupedList, selected, favSet)

        if (q.isNotEmpty()) {
            filterForSearch(baseItems, metaById, chainsProjection, q)
        } else {
            prependFavourites(baseItems, metaById, favSet)
        }
    }
        .shareInBackground()

    private suspend fun buildBaseItems(
        groupedList: GroupedList<LightMetaAccount.Type, MetaAccountListingItem>,
        selected: SelectedMetaAccountState,
        favSet: Set<Long>
    ): List<Any> = groupedList.toListWithHeaders(
        keyMapper = { type, _ ->
            accountTypePresentationMapper.mapMetaAccountTypeToUi(type)
                ?: if (type == LightMetaAccount.Type.SECRETS) {
                    AccountChipGroupRvItem(
                        ChipLabelModel(
                            title = resourceManager.getString(io.novafoundation.nova.common.R.string.wallets_section_header),
                            icon = null
                        )
                    )
                } else {
                    null
                }
        },
        valueMapper = { mapMetaAccountToUi(it, selected, favSet) }
    )

    private fun filterForSearch(
        baseItems: List<Any>,
        metaById: Map<Long, MetaAccountListingItem>,
        chainsProjection: ChainSearchProjection,
        query: String
    ): List<Any> {
        val normalizedQuery = query.replace("-", "").replace(" ", "")
        val isProxyTypeQuery = !normalizedQuery.isEmpty() &&
            PROXY_TYPE_KEYWORDS.any { it.contains(normalizedQuery) }

        return baseItems.filter { item ->
            if (item !is AccountUi) return@filter false
            val meta = metaById[item.id]?.metaAccount ?: return@filter false

            if (isProxyTypeQuery) {
                if (meta !is ProxiedMetaAccount) return@filter false
                return@filter meta.proxy.proxyType.name.lowercase().contains(normalizedQuery)
            }

            // Name match
            if (item.title.toString().lowercase().contains(query)) return@filter true

            // Chain name match — wallet's explicit chain accounts
            val chainAccountNames = meta.chainAccounts.keys
                .mapNotNull { chainsProjection.namesById[it]?.lowercase() }
            if (chainAccountNames.any { it.contains(query) }) return@filter true

            // Chain name match — substrate / EVM base wallets implicitly
            // support any chain of that type
            val supportsSubstrate = meta.substrateAccountId != null
            val supportsEvm = meta.ethereumAddress != null
            if (supportsSubstrate && chainsProjection.matchesSubstrate(query)) return@filter true
            if (supportsEvm && chainsProjection.matchesEvm(query)) return@filter true

            // Proxied wallets without substrate base — match by proxy type
            if (meta is ProxiedMetaAccount &&
                meta.proxy.proxyType.name.lowercase().contains(normalizedQuery)
            ) {
                return@filter true
            }

            false
        }
    }

    private fun prependFavourites(
        baseItems: List<Any>,
        metaById: Map<Long, MetaAccountListingItem>,
        favSet: Set<Long>
    ): List<Any> {
        if (favSet.isEmpty()) return baseItems

        val favouriteUis = baseItems
            .filterIsInstance<AccountUi>()
            .filter { it.id in favSet }
            .map { ui -> wrapForFavouritesSection(ui, metaById[ui.id]) }

        if (favouriteUis.isEmpty()) return baseItems

        val favouritesHeader = AccountChipGroupRvItem(
            ChipLabelModel(
                title = resourceManager.getString(io.novafoundation.nova.common.R.string.favourites_section_header),
                icon = null
            )
        )
        return listOf<Any>(favouritesHeader) + favouriteUis + baseItems
    }

    private fun wrapForFavouritesSection(
        ui: AccountUi,
        meta: MetaAccountListingItem?
    ): AccountUi {
        // For multisigs without a network-specific chain icon, surface the
        // multisig icon over the identicon so the type stays visible in the
        // Favourites section.
        val chainIcon = if (
            meta is MetaAccountListingItem.Multisig && ui.chainIcon == null
        ) {
            io.novafoundation.nova.common.R.drawable.ic_multisig.asIcon()
        } else {
            ui.chainIcon
        }
        return AccountUi(
            id = ui.id,
            title = ui.title,
            subtitle = ui.subtitle,
            isSelected = ui.isSelected,
            isEditable = ui.isEditable,
            isClickable = ui.isClickable,
            picture = ui.picture,
            chainIcon = chainIcon,
            updateIndicator = ui.updateIndicator,
            subtitleIconRes = ui.subtitleIconRes,
            chainIconOpacity = ui.chainIconOpacity,
            isFavourite = true,
            showFavouriteToggle = ui.showFavouriteToggle,
            typeBadge = ui.typeBadge,
            groupTag = "favourites"
        )
    }

    companion object {
        private val PROXY_TYPE_KEYWORDS = setOf(
            "any", "nontransfer", "governance", "staking",
            "identityjudgement", "cancelproxy", "auction", "nominationpools"
        )
    }

    private suspend fun mapMetaAccountToUi(
        metaAccountWithBalance: MetaAccountListingItem,
        selected: SelectedMetaAccountState,
        favouriteIds: Set<Long>
    ) =
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
                subtitleIconRes = null,
                isFavourite = favouriteIds.contains(metaAccount.id),
                showFavouriteToggle = showFavouriteToggle
            )
        }

    private fun MetaAccountListingItem.chainIcon(): Icon? {
        return when (this) {
            is MetaAccountListingItem.Proxied -> proxyChain.iconOrFallback()

            is MetaAccountListingItem.Multisig -> singleChain?.iconOrFallback()

            is MetaAccountListingItem.TotalBalance -> null
        }
    }

    private suspend fun MetaAccountListingItem.formatSubtitle(): CharSequence = when (this) {
        is MetaAccountListingItem.Proxied -> formatSubtitle()
        is MetaAccountListingItem.TotalBalance -> formatSubtitle()
        is MetaAccountListingItem.Multisig -> formatSubtitle()
    }

    private suspend fun MetaAccountListingItem.Proxied.formatSubtitle(): CharSequence {
        return proxyFormatter.mapProxyMetaAccountSubtitle(
            proxyMetaAccount.name,
            proxyFormatter.makeAccountDrawable(proxyMetaAccount),
            metaAccount.proxy
        )
    }

    private suspend fun MetaAccountListingItem.Multisig.formatSubtitle(): CharSequence {
        return multisigFormatter.formatSignatorySubtitle(signatory)
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

            LightMetaAccount.Type.PROXIED,
            LightMetaAccount.Type.MULTISIG -> false
        }
    }
}

/**
 * Lightweight projection of the chain registry that only carries the fields
 * the wallet search filter actually needs (id, name, ethereum-or-substrate
 * type). Used with `distinctUntilChanged()` to avoid re-running the listing
 * combine on unrelated chain field updates (icon refreshes, runtime metadata,
 * etc.).
 */
private data class ChainSearchProjection(
    val namesById: Map<String, String>,
    private val substrateNames: List<String>,
    private val evmNames: List<String>
) {
    fun matchesSubstrate(query: String): Boolean = substrateNames.any { it.contains(query) }
    fun matchesEvm(query: String): Boolean = evmNames.any { it.contains(query) }
}

private fun List<Chain>.toSearchableProjection(): ChainSearchProjection {
    val names = associate { it.id to it.name }
    val substrate = filterNot { it.isEthereumBased }.map { it.name.lowercase() }
    val evm = filter { it.isEthereumBased }.map { it.name.lowercase() }
    return ChainSearchProjection(
        namesById = names,
        substrateNames = substrate,
        evmNames = evm
    )
}
