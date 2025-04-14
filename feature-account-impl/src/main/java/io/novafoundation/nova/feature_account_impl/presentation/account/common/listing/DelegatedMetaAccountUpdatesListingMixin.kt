package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.utils.withAlphaDrawable
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedAndProxyMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountTitleGroupRvItem
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.chain.iconOrFallback
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

private const val DISABLED_ICON_ALPHA = 0.56f
private const val ENABLED_ICON_ALPHA = 1.0f

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

    private fun mapHeader(status: LightMetaAccount.Status): AccountTitleGroupRvItem {
        val text = when (status) {
            LightMetaAccount.Status.ACTIVE -> resourceManager.getString(R.string.account_proxieds)
            LightMetaAccount.Status.DEACTIVATED -> resourceManager.getString(R.string.proxieds_updates_deactivated_title)
        }

        return AccountTitleGroupRvItem(text)
    }

    private suspend fun mapProxiedToUi(proxiedWithProxy: ProxiedAndProxyMetaAccount) = with(proxiedWithProxy) {
        val isEnabled = proxiedWithProxy.proxied.status == LightMetaAccount.Status.ACTIVE
        val secondaryColor = resourceManager.getColor(R.color.text_secondary)
        val title = proxied.name
        val subtitle = mapSubtitle(this, isEnabled)
        val walletIcon = walletUiUseCase.walletIcon(proxied)
        AccountUi(
            id = proxied.id,
            title = if (isEnabled) title else title.toSpannable(colorSpan(secondaryColor)),
            subtitle = if (isEnabled) subtitle else subtitle.toSpannable(colorSpan(secondaryColor)),
            isSelected = false,
            isClickable = true,
            picture = if (isEnabled) walletIcon else walletIcon.withAlphaDrawable(DISABLED_ICON_ALPHA),
            chainIcon = proxiedWithProxy.chain.iconOrFallback(),
            updateIndicator = false,
            subtitleIconRes = null,
            chainIconOpacity = if (isEnabled) ENABLED_ICON_ALPHA else DISABLED_ICON_ALPHA,
            isEditable = false
        )
    }

    private suspend fun mapSubtitle(
        proxiedWithProxy: ProxiedAndProxyMetaAccount,
        isEnabled: Boolean
    ): CharSequence {
        val proxy = proxiedWithProxy.proxied.proxy
        val proxyIcon = proxyFormatter.makeAccountDrawable(proxiedWithProxy.proxy)
        return proxyFormatter.mapProxyMetaAccountSubtitle(
            proxiedWithProxy.proxy.name,
            if (isEnabled) proxyIcon else proxyIcon.withAlphaDrawable(DISABLED_ICON_ALPHA),
            proxy
        )
    }

    private fun ProxiedAndProxyMetaAccount.proxiedAddress(): String {
        return proxied.requireAddressIn(chain)
    }
}
