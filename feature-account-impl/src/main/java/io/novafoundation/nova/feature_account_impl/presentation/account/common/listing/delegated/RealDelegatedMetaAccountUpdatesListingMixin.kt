package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.delegated

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.filterValueList
import io.novafoundation.nova.common.utils.isAllEquals
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.utils.withAlphaDrawable
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.AccountDelegation
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.getChainOrNull
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.MultisigFormatter
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.ProxyFormatter
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountTitleGroupRvItem
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.chain.iconOrFallback
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.delegated.DelegatedMetaAccountUpdatesListingMixin.FilterType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class DelegatedMetaAccountUpdatesListingMixinFactory(
    private val walletUiUseCase: WalletUiUseCase,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val proxyFormatter: ProxyFormatter,
    private val multisigFormatter: MultisigFormatter,
    private val resourceManager: ResourceManager
) {

    fun create(coroutineScope: CoroutineScope): DelegatedMetaAccountUpdatesListingMixin {
        return RealDelegatedMetaAccountUpdatesListingMixin(
            walletUiUseCase = walletUiUseCase,
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            proxyFormatter = proxyFormatter,
            multisigFormatter = multisigFormatter,
            resourceManager = resourceManager,
            coroutineScope = coroutineScope
        )
    }
}

private const val DISABLED_ICON_ALPHA = 0.56f
private const val ENABLED_ICON_ALPHA = 1.0f

private class RealDelegatedMetaAccountUpdatesListingMixin(
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val walletUiUseCase: WalletUiUseCase,
    private val proxyFormatter: ProxyFormatter,
    private val multisigFormatter: MultisigFormatter,
    private val resourceManager: ResourceManager,
    coroutineScope: CoroutineScope,
) : DelegatedMetaAccountUpdatesListingMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val accountsByStateFlow = metaAccountGroupingInteractor.updatedDelegates()
        .shareInBackground()

    private val desiredUserTypeFilter = MutableStateFlow<FilterType>(FilterType.Proxied)

    override val accountTypeFilter = combine(accountsByStateFlow, desiredUserTypeFilter) { accountsByState, filterType ->
        val accounts = accountsByState.values.flatten()
        val shouldIgnoreUserFilter = accounts.isAllEquals { it.delegator.type }
        if (shouldIgnoreUserFilter) {
            val accountsMatchedFilterType = accountMatchedFilterType(accounts.first())
            FilterType.UserIgnored(overriddenFilter = accountsMatchedFilterType)
        } else {
            filterType
        }
    }.distinctUntilChanged()
        .shareInBackground()

    override val metaAccountsFlow = combine(
        accountsByStateFlow,
        accountTypeFilter
    ) { accounts, filterType ->
        accounts.filterValueList { filterType.filter(it.delegator) }
            .toListWithHeaders(
                keyMapper = { accountStatus, _ -> mapHeader(accountStatus, filterType) },
                valueMapper = { mapProxiedToUi(it) }
            )
    }.shareInBackground()

    override fun filterBy(type: FilterType) {
        desiredUserTypeFilter.value = type
    }

    private fun mapHeader(status: LightMetaAccount.Status, type: FilterType): AccountTitleGroupRvItem {
        val text = when (status) {
            LightMetaAccount.Status.ACTIVE -> mapActiveHeader(type)

            LightMetaAccount.Status.DEACTIVATED -> resourceManager.getString(R.string.delegation_updates_deactivated_title)
        }

        return AccountTitleGroupRvItem(text)
    }

    private fun mapActiveHeader(type: FilterType): String {
        return when (type) {
            FilterType.Proxied -> resourceManager.getString(R.string.account_proxieds)
            FilterType.Multisig -> resourceManager.getString(R.string.active_multisig_title)
            is FilterType.UserIgnored -> mapActiveHeader(type.overriddenFilter)
        }
    }

    private suspend fun mapProxiedToUi(accountDelegation: AccountDelegation) = with(accountDelegation) {
        val isEnabled = delegator.status == LightMetaAccount.Status.ACTIVE
        val secondaryColor = resourceManager.getColor(R.color.text_secondary)
        val title = delegator.name
        val subtitle = mapSubtitle(this, isEnabled)
        val walletIcon = walletUiUseCase.walletIcon(delegator)
        AccountUi(
            id = delegator.id,
            title = if (isEnabled) title else title.toSpannable(colorSpan(secondaryColor)),
            subtitle = if (isEnabled) subtitle else subtitle.toSpannable(colorSpan(secondaryColor)),
            isSelected = false,
            isClickable = true,
            picture = if (isEnabled) walletIcon else walletIcon.withAlphaDrawable(DISABLED_ICON_ALPHA),
            chainIcon = getChainOrNull()?.iconOrFallback(),
            updateIndicator = false,
            subtitleIconRes = null,
            chainIconOpacity = if (isEnabled) ENABLED_ICON_ALPHA else DISABLED_ICON_ALPHA,
            isEditable = false
        )
    }

    private suspend fun mapSubtitle(
        accountDelegation: AccountDelegation,
        isEnabled: Boolean
    ): CharSequence {
        val icon = when (accountDelegation) {
            is AccountDelegation.Multisig -> multisigFormatter.makeAccountDrawable(accountDelegation.signatory)
            is AccountDelegation.Proxy -> proxyFormatter.makeAccountDrawable(accountDelegation.proxy)
        }

        val delegatorIcon = if (isEnabled) icon else icon.withAlphaDrawable(DISABLED_ICON_ALPHA)

        return when (accountDelegation) {
            is AccountDelegation.Multisig -> {
                multisigFormatter.formatSignatorySubtitle(accountDelegation.signatory, delegatorIcon)
            }

            is AccountDelegation.Proxy -> {
                val proxy = accountDelegation.proxied.proxy
                return proxyFormatter.mapProxyMetaAccountSubtitle(accountDelegation.proxy.name, delegatorIcon, proxy)
            }
        }
    }

    private fun accountMatchedFilterType(accountDelegation: AccountDelegation): FilterType {
        return when (accountDelegation) {
            is AccountDelegation.Multisig -> FilterType.Multisig
            is AccountDelegation.Proxy -> FilterType.Proxied
        }
    }
}
