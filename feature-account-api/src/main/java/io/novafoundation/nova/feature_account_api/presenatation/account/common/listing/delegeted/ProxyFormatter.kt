package io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted

import android.graphics.drawable.Drawable
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType

interface ProxyFormatter {

    suspend fun formatProxiedMetaAccountSubtitle(proxy: MetaAccount, proxyAccount: ProxyAccount): CharSequence

    suspend fun formatProxyMetaAccount(proxy: MetaAccount): CharSequence

    fun mapProxyTypeToString(type: ProxyType): String

    suspend fun makeProxyDrawable(proxy: MetaAccount): Drawable
}
