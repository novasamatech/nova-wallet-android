package io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp

import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerEvmAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateApplicationConfig
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransport
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.buildDerivationPath
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.getConfig
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class MigrationSubstrateLedgerApplication(
    transport: LedgerTransport,
    metadataShortenerService: MetadataShortenerService,
    private val chainRegistry: ChainRegistry,
    private val ledgerRepository: LedgerRepository,
    private val legacyApplicationConfigs: List<SubstrateApplicationConfig> = SubstrateApplicationConfig.all()
) : NewSubstrateLedgerApplication(transport, chainRegistry, metadataShortenerService) {

    override val cla: UByte = GenericSubstrateLedgerApplication.CLA

    override suspend fun getDerivationPath(chainId: ChainId, accountIndex: Int): String {
        val applicationConfig = legacyApplicationConfigs.getConfig(chainId)

        return buildDerivationPath(applicationConfig.coin, accountIndex)
    }

    override suspend fun getAddressPrefix(chainId: ChainId): Short {
        val chain = chainRegistry.getChain(chainId)

        return chain.addressPrefix.toShort()
    }

    override suspend fun getDerivationPath(metaId: Long, chainId: ChainId): String {
        return ledgerRepository.getChainAccountDerivationPath(metaId, chainId)
    }

    override suspend fun getEvmAccount(device: LedgerDevice, chainId: ChainId, accountIndex: Int, confirmAddress: Boolean): LedgerEvmAccount? {
        return null
    }
}
