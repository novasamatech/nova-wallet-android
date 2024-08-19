package io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp

import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateApplicationConfig
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransport
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.buildDerivationPath
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.getConfig
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class GenericSubstrateLedgerApplication(
    transport: LedgerTransport,
    chainRegistry: ChainRegistry,
    metadataShortenerService: MetadataShortenerService,
    private val ledgerRepository: LedgerRepository,
    private val legacyApplicationConfigs: List<SubstrateApplicationConfig> = SubstrateApplicationConfig.all()
) : NewSubstrateLedgerApplication(transport, chainRegistry, metadataShortenerService) {

    private val universalConfig = legacyApplicationConfigs.getConfig(Chain.Geneses.POLKADOT)

    override val cla: UByte = CLA

    suspend fun getUniversalAccount(
        device: LedgerDevice,
        accountIndex: Int,
        confirmAddress: Boolean
    ): LedgerSubstrateAccount {
        return getAccount(device, Chain.Geneses.POLKADOT, accountIndex = accountIndex, confirmAddress)
    }

    override suspend fun getDerivationPath(chainId: ChainId, accountIndex: Int): String {
        return buildDerivationPath(universalConfig.coin, accountIndex)
    }

    override suspend fun getDerivationPath(metaId: Long, chainId: ChainId): String {
        return ledgerRepository.getGenericDerivationPath(metaId)
    }

    companion object {

        const val CLA: UByte = 0xf9u
    }
}
