package io.novafoundation.nova.feature_staking_impl.data.validators

import io.novafoundation.nova.common.utils.toHexAccountId
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface KnownNovaValidators {

    fun getValidatorIds(chainId: ChainId): Set<String>
}

class FixedKnownNovaValidators : KnownNovaValidators {

    private val novaValidators by lazy {
        val sharedAccounts = sharedValidatorsAccountIdsHex()

        mapOf(
            Chain.Geneses.POLKADOT to sharedAccounts,
            Chain.Geneses.KUSAMA to kusamaValidators(),
            Chain.Geneses.ALEPH_ZERO to sharedAccounts
        )
    }

    override fun getValidatorIds(chainId: ChainId): Set<String> {
        return novaValidators[chainId].orEmpty()
    }

    private fun sharedValidatorsAccountIdsHex(): Set<String> {
        return setOf(
            "127zarPDhVzmCXVQ7Kfr1yyaa9wsMuJ74GJW9Q7ezHfQEgh6".toHexAccountId()
        )
    }

    private fun kusamaValidators(): Set<String> {
        return setOf(
            "DhK6qU2U5kDWeJKvPRtmnWRs8ETUGZ9S9QmNmQFuzrNoKm4".toHexAccountId(),
            "EtETk1FbrDg7FoAfkREuXT7xHxCjbEf28sBvWf6zfB5wFyV".toHexAccountId()
        )
    }
}
