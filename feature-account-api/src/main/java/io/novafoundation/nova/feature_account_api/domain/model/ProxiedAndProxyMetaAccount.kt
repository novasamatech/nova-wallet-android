package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ProxiedAndProxyMetaAccount(
    val proxied: ProxiedMetaAccount,
    val proxy: MetaAccount,
    val chain: Chain
)
