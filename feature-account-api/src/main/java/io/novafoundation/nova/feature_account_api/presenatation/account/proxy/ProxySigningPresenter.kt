package io.novafoundation.nova.feature_account_api.presenatation.account.proxy

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ProxySigningPresenter {

    suspend fun acknowledgeProxyOperation(proxiedMetaAccount: MetaAccount, proxyMetaAccount: MetaAccount): Boolean

    suspend fun notEnoughPermission(proxiedMetaAccount: MetaAccount, proxyMetaAccount: MetaAccount, proxyTypes: List<ProxyType>)

    suspend fun signingIsNotSupported()

    suspend fun notEnoughFee(metaAccount: MetaAccount, chainAsset: Chain.Asset, availableBalance: String, fee: String)
}
