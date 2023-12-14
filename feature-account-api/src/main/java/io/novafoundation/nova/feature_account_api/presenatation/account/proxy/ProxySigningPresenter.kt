package io.novafoundation.nova.feature_account_api.presenatation.account.proxy

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount

interface ProxySigningPresenter {

    suspend fun requestResume(proxiedMetaAccount: MetaAccount, proxyMetaAccount: MetaAccount): Boolean

    suspend fun notEnoughPermission(proxiedMetaAccount: MetaAccount, proxyMetaAccount: MetaAccount, proxyTypes: List<ProxyAccount.ProxyType>)

    suspend fun signingIsNotSupported()
}
