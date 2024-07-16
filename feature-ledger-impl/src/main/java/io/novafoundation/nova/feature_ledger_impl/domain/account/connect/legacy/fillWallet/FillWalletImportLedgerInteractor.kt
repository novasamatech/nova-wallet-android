package io.novafoundation.nova.feature_ledger_impl.domain.account.connect.legacy.fillWallet

import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateApplicationConfig
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.enabledChainById

interface FillWalletImportLedgerInteractor {

    suspend fun availableLedgerChains(): List<Chain>
}

class RealFillWalletImportLedgerInteractor(
    private val chainRegistry: ChainRegistry
) : FillWalletImportLedgerInteractor {

    override suspend fun availableLedgerChains(): List<Chain> {
        val supportedLedgerApps = SubstrateApplicationConfig.all()
        val supportedChainIds = supportedLedgerApps.mapToSet { it.chainId }

        return chainRegistry.enabledChainById()
            .filterKeys { it in supportedChainIds }
            .values
            .toList()
    }
}
