package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.preview

import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan.ParitySignerAccount
import io.novafoundation.nova.runtime.ext.defaultComparator
import io.novafoundation.nova.runtime.ext.type
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChains

interface PreviewImportParitySignerInteractor {

    suspend fun deriveSubstrateChainAccounts(account: ParitySignerAccount): List<ParitySignerAccountInChain>
}

class RealPreviewImportParitySignerInteractor(
    private val chainRegistry: ChainRegistry
) : PreviewImportParitySignerInteractor {

    override suspend fun deriveSubstrateChainAccounts(account: ParitySignerAccount): List<ParitySignerAccountInChain> {
        val relevantChainType = account.chainType()
        val relevantChains = chainRegistry.findChains { it.type == relevantChainType }

        return relevantChains
            .sortedWith(Chain.defaultComparator())
            .map { chain -> ParitySignerAccountInChain(chain, account.accountId) }
    }

    private fun ParitySignerAccount.chainType() = when (accountType) {
        ParitySignerAccount.Type.SUBSTRATE -> Chain.Type.SUBSTRATE
        ParitySignerAccount.Type.ETHEREUM -> Chain.Type.ETHEREUM
    }
}
