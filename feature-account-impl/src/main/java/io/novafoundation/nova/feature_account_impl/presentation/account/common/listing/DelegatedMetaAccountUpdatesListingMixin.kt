package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedAndProxyMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountTitleGroupRvItem
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_impl.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class DelegatedMetaAccountUpdatesListingMixinFactory(
    private val walletUiUseCase: WalletUiUseCase,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val proxyFormatter: ProxyFormatter,
    private val resourceManager: ResourceManager
) {

    fun create(coroutineScope: CoroutineScope): MetaAccountListingMixin {
        return DelegatedMetaAccountUpdatesListingMixin(
            walletUiUseCase = walletUiUseCase,
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            proxyFormatter = proxyFormatter,
            resourceManager = resourceManager,
            coroutineScope = coroutineScope
        )
    }
}

private class DelegatedMetaAccountUpdatesListingMixin(
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val walletUiUseCase: WalletUiUseCase,
    private val proxyFormatter: ProxyFormatter,
    private val resourceManager: ResourceManager,
    coroutineScope: CoroutineScope,
) : MetaAccountListingMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val metaAccountsFlow = metaAccountGroupingInteractor.updatedProxieds()
        .map { list ->
            list.toListWithHeaders(
                keyMapper = { type, _ -> mapHeader(type) },
                valueMapper = { mapProxiedToUi(it) }
            )
        }
        .shareInBackground()

    private fun mapHeader(state: LightMetaAccount.State): AccountTitleGroupRvItem {
        val text = when (state) {
            LightMetaAccount.State.ACTIVE -> resourceManager.getString(R.string.account_proxieds)
            LightMetaAccount.State.DEACTIVATED -> resourceManager.getString(R.string.proxieds_updates_deactivated_title)
        }

        return AccountTitleGroupRvItem(text)
    }

    private suspend fun mapProxiedToUi(proxiedWithProxy: ProxiedAndProxyMetaAccount) = with(proxiedWithProxy) {
        AccountUi(
            id = proxied.id,
            title = proxied.name,
            subtitle = mapSubtitle(this),
            isSelected = false,
            isClickable = true,
            picture = walletUiUseCase.walletIcon(proxied),
            chainIconUrl = proxiedWithProxy.chain.icon,
            subtitleIconRes = null,
            enabled = proxiedWithProxy.proxied.state == LightMetaAccount.State.ACTIVE,
            updateIndicator = false
        )
    }

    private suspend fun mapSubtitle(
        proxiedWithProxy: ProxiedAndProxyMetaAccount
    ): CharSequence {
        val proxy = proxiedWithProxy.proxied.proxy ?: return proxiedWithProxy.proxiedAddress() // fallback
        return proxyFormatter.mapProxyMetaAccountSubtitle(proxiedWithProxy.proxied, proxy)
    }

    private fun ProxiedAndProxyMetaAccount.proxiedAddress(): String {
        return proxied.requireAddressIn(chain)
    }
}
