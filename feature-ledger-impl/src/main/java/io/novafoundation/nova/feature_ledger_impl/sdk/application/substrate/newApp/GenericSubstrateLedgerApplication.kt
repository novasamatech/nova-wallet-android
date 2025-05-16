package io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp

import io.novafoundation.nova.common.utils.GENERIC_ADDRESS_PREFIX
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerEvmAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateApplicationConfig
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransport
import io.novafoundation.nova.feature_ledger_api.sdk.transport.send
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.CryptoScheme
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.DisplayVerificationDialog
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.Instruction
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.buildDerivationPath
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.encodeDerivationPath
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.getConfig
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.processResponseCode
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.json.copyBytes
import io.novasama.substrate_sdk_android.extensions.asEthereumPublicKey
import io.novasama.substrate_sdk_android.extensions.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder

class GenericSubstrateLedgerApplication(
    private val transport: LedgerTransport,
    chainRegistry: ChainRegistry,
    metadataShortenerService: MetadataShortenerService,
    private val ledgerRepository: LedgerRepository,
    private val legacyApplicationConfigs: List<SubstrateApplicationConfig> = SubstrateApplicationConfig.all()
) : NewSubstrateLedgerApplication(transport, chainRegistry, metadataShortenerService) {

    private val universalConfig = legacyApplicationConfigs.getConfig(Chain.Geneses.POLKADOT)

    override val cla: UByte = CLA

    suspend fun getUniversalSubstrateAccount(
        device: LedgerDevice,
        accountIndex: Int,
        confirmAddress: Boolean
    ): LedgerSubstrateAccount {
        return getSubstrateAccount(device, Chain.Geneses.POLKADOT, accountIndex = accountIndex, confirmAddress)
    }

    override suspend fun getDerivationPath(chainId: ChainId, accountIndex: Int): String {
        return buildDerivationPath(universalConfig.coin, accountIndex)
    }

    override suspend fun getDerivationPath(metaId: Long, chainId: ChainId): String {
        return ledgerRepository.getGenericDerivationPath(metaId)
    }

    override suspend fun getAddressPrefix(chainId: ChainId): Short {
        return SS58Encoder.GENERIC_ADDRESS_PREFIX
    }

    override suspend fun getEvmAccount(
        device: LedgerDevice,
        accountIndex: Int,
        confirmAddress: Boolean
    ): LedgerEvmAccount? {
        val displayVerificationDialog = DisplayVerificationDialog.fromBoolean(confirmAddress)

        val derivationPath = buildDerivationPath(universalConfig.coin, accountIndex)
        val encodedDerivationPath = encodeDerivationPath(derivationPath)
        val payload = encodedDerivationPath

        // TODO handle not upgraded generic app response

        val rawResponse = transport.send(
            cla = cla,
            ins = Instruction.GET_ADDRESS.code,
            p1 = displayVerificationDialog.code,
            p2 = CryptoScheme.ECDSA.code,
            data = payload,
            device = device
        )

        return parseEvmAccountResponse(rawResponse)
    }

    companion object {

        const val CLA: UByte = 0xf9u

        private const val EVM_PUBLIC_KEY_LENGTH = 33
    }


    private fun parseEvmAccountResponse(raw: ByteArray): LedgerEvmAccount {
        val dataWithoutResponseCode = processResponseCode(raw)

        val publicKey = dataWithoutResponseCode.copyBytes(0, EVM_PUBLIC_KEY_LENGTH)
        require(publicKey.size == EVM_PUBLIC_KEY_LENGTH) {
            "No public key"
        }

        return LedgerEvmAccount(
            publicKey = publicKey,
            accountId = publicKey.asEthereumPublicKey().toAccountId().value,
        )
    }
}
