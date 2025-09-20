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
        val multisig: MultisigMetaAccount,
        val signatory: MetaAccount,
        val singleChain: Chain?, // null in case multisig is universal
    ) : AccountDelegation {

        override val delegator = multisig
    }

    class Derivative(
        val derivative: DerivativeMetaAccount,
        val parent: MetaAccount,
        val singleChain: Chain?, // null in case derivative is universal
    ): AccountDelegation {

        override val delegator = derivative
    }
}

fun AccountDelegation.getChainOrNull(): Chain? {
    return when (this) {
        is AccountDelegation.Multisig -> singleChain
        is AccountDelegation.Proxy -> chain
        is AccountDelegation.Derivative -> singleChain
    }
}
