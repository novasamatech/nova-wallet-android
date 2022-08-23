package io.novafoundation.nova.feature_ledger_impl.domain.account.connect.fillWallet

import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateApplicationConfig
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChains

interface FillWalletImportLedgerInteractor {

    suspend fun availableLedgerChains(): List<Chain>
}

class RealFillWalletImportLedgerInteractor(
    private val chainRegistry: ChainRegistry
) : FillWalletImportLedgerInteractor {

    override suspend fun availableLedgerChains(): List<Chain> {
        val supportedLedgerApps = SubstrateApplicationConfig.all()
        val supportedChainIds = supportedLedgerApps.mapToSet { it.chainId }

        return chainRegistry.findChains { it.id in supportedChainIds }
    }
}
