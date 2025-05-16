package io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.legacyApp

import io.novafoundation.nova.common.utils.chunked
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerRepository
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerEvmAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateApplicationConfig
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransport
import io.novafoundation.nova.feature_ledger_api.sdk.transport.send
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.CHUNK_SIZE
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.buildDerivationPath
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.getConfig
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.encodedSignaturePayload

class LegacySubstrateLedgerApplication(
    private val transport: LedgerTransport,
    private val ledgerRepository: LedgerRepository,
    private val supportedApplications: List<SubstrateApplicationConfig> = SubstrateApplicationConfig.all(),
) : SubstrateLedgerApplication {

    override suspend fun getSubstrateAccount(
        device: LedgerDevice,
        chainId: ChainId,
        accountIndex: Int,
        confirmAddress: Boolean
    ): LedgerSubstrateAccount {
        val applicationConfig = supportedApplications.getConfig(chainId)
        val displayVerificationDialog = SubstrateLedgerAppCommon.DisplayVerificationDialog.fromBoolean(confirmAddress)

        val derivationPath = buildDerivationPath(applicationConfig.coin, accountIndex)
        val encodedDerivationPath = SubstrateLedgerAppCommon.encodeDerivationPath(derivationPath)

        val rawResponse = transport.send(
            cla = applicationConfig.cla,
            ins = SubstrateLedgerAppCommon.Instruction.GET_ADDRESS.code,
            p1 = displayVerificationDialog.code,
            p2 = SubstrateLedgerAppCommon.defaultCryptoScheme().code,
            data = encodedDerivationPath,
            device = device
        )

        return SubstrateLedgerAppCommon.parseSubstrateAccountResponse(rawResponse, derivationPath)
    }

    override suspend fun getEvmAccount(device: LedgerDevice, chainId: ChainId, accountIndex: Int, confirmAddress: Boolean): LedgerEvmAccount? {
        return null
    }

    override suspend fun getSignature(
        device: LedgerDevice,
        metaId: Long,
        chainId: ChainId,
        payload: SignerPayloadExtrinsic,
    ): SignatureWrapper {
        val payloadBytes = payload.encodedSignaturePayload(hashBigPayloads = false)
        val applicationConfig = supportedApplications.getConfig(chainId)

        val derivationPath = ledgerRepository.getChainAccountDerivationPath(metaId, chainId)
        val encodedDerivationPath = SubstrateLedgerAppCommon.encodeDerivationPath(derivationPath)

        val chunks = listOf(encodedDerivationPath) + payloadBytes.chunked(CHUNK_SIZE)

        val results = chunks.mapIndexed { index, chunk ->
            val chunkType = SubstrateLedgerAppCommon.SignPayloadType(index, chunks.size)

            val rawResponse = transport.send(
                cla = applicationConfig.cla,
                ins = SubstrateLedgerAppCommon.Instruction.SIGN.code,
                p1 = chunkType.code,
                p2 = SubstrateLedgerAppCommon.defaultCryptoScheme().code,
                data = chunk,
                device = device
            )

            SubstrateLedgerAppCommon.processResponseCode(rawResponse)
        }

        val signatureWithType = results.last()

        return SubstrateLedgerAppCommon.parseMultiSignature(signatureWithType)
    }
}
