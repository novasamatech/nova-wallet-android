package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed interface AccountDelegation {

    val delegator: MetaAccount

    class Proxy(
        val proxied: ProxiedMetaAccount,
        val proxy: MetaAccount,
        val chain: Chain
    ) : AccountDelegation {

        override val delegator = proxied
    }

    class Multisig(
        val metaAccount: MultisigMetaAccount,
        val signatory: MetaAccount
    ) : AccountDelegation {

        override val delegator = metaAccount
    }
}

fun AccountDelegation.getChainOrNull(): Chain? {
    return when (this) {
        is AccountDelegation.Multisig -> null
        is AccountDelegation.Proxy -> chain
    }
}
