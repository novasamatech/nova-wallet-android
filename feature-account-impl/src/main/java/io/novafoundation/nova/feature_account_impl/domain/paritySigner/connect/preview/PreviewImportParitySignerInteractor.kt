package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.preview

import io.novafoundation.nova.runtime.ext.isSubstrateBased
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.first

interface PreviewImportParitySignerInteractor {

    suspend fun deriveSubstrateChainAccounts(accountId: ByteArray): List<ParitySignerAccountInChain>
}

class RealPreviewImportParitySignerInteractor(
    private val chainRegistry: ChainRegistry
): PreviewImportParitySignerInteractor {

    override suspend fun deriveSubstrateChainAccounts(accountId: ByteArray): List<ParitySignerAccountInChain> {
        val substrateChains = chainRegistry.currentChains.first().filter { it.isSubstrateBased }

        return substrateChains.map { chain ->
            ParitySignerAccountInChain(chain, accountId)
        }
    }
}
