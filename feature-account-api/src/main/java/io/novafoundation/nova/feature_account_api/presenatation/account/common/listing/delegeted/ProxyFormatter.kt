package io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted

import android.graphics.drawable.Drawable
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType

interface ProxyFormatter {

    fun mapProxyMetaAccountSubtitle(proxyAccountName: String, proxyAccountIcon: Drawable, proxyAccount: ProxyAccount): CharSequence

    fun mapProxyMetaAccount(proxyAccountName: String, proxyAccountIcon: Drawable): CharSequence

    fun mapProxyTypeToString(type: ProxyType): String

    suspend fun makeAccountDrawable(metaAccount: MetaAccount): Drawable
}
