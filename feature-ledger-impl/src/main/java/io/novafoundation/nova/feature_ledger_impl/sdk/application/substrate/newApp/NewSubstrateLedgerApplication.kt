package io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp

import android.util.Log
import io.novafoundation.nova.common.utils.chunked
import io.novafoundation.nova.common.utils.littleEndianBytes
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransport
import io.novafoundation.nova.feature_ledger_api.sdk.transport.send
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.CHUNK_SIZE
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.CryptoScheme
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.defaultCryptoScheme
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.encodeDerivationPath
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.parseSubstrateAccountResponse
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.InheritedImplication

abstract class NewSubstrateLedgerApplication(
    private val transport: LedgerTransport,
    private val metadataShortenerService: MetadataShortenerService,
    private val chainRegistry: ChainRegistry,
) : SubstrateLedgerApplication {

    abstract val cla: UByte

    abstract suspend fun getDerivationPath(chainId: ChainId, accountIndex: Int): String

    abstract suspend fun getDerivationPath(metaId: Long, chainId: ChainId): String

    abstract suspend fun getAddressPrefix(chainId: ChainId): Short

    override suspend fun getSubstrateAccount(device: LedgerDevice, chainId: ChainId, accountIndex: Int, confirmAddress: Boolean): LedgerSubstrateAccount {
        val displayVerificationDialog = SubstrateLedgerAppCommon.DisplayVerificationDialog.fromBoolean(confirmAddress)

        val derivationPath = getDerivationPath(chainId, accountIndex)
        val encodedDerivationPath = encodeDerivationPath(derivationPath)

        val addressPrefix = getAddressPrefix(chainId)

        val payload = encodedDerivationPath + addressPrefix.littleEndianBytes

        val rawResponse = transport.send(
            cla = cla,
            ins = SubstrateLedgerAppCommon.Instruction.GET_ADDRESS.code,
            p1 = displayVerificationDialog.code,
            p2 = defaultCryptoScheme().code,
            data = payload,
            device = device
        )

        return parseSubstrateAccountResponse(rawResponse, derivationPath)
    }

    override suspend fun getSignature(
        device: LedgerDevice,
        metaId: Long,
        chainId: ChainId,
        payload: InheritedImplication
    ): SignatureWrapper {
        val chain = chainRegistry.getChain(chainId)

        return if (chain.isEthereumBased) {
            getEvmSignature(device, metaId, chainId, payload)
        } else {
            getSubstrateSignature(device, metaId, chainId, payload)
        }
    }

    private suspend fun getSubstrateSignature(
        device: LedgerDevice,
        metaId: Long,
        chainId: ChainId,
        payload: InheritedImplication
    ): SignatureWrapper {
        val multiSignature = sendSignChunks(device, metaId, chainId, payload, defaultCryptoScheme())
        return SubstrateLedgerAppCommon.parseMultiSignature(multiSignature)
    }

    private suspend fun getEvmSignature(
        device: LedgerDevice,
        metaId: Long,
        chainId: ChainId,
        payload: InheritedImplication
    ): SignatureWrapper {
        val signature = sendSignChunks(device, metaId, chainId, payload, CryptoScheme.ECDSA)
        return SubstrateLedgerAppCommon.parseSignature(signature, CryptoScheme.ECDSA)
    }

    private suspend fun sendSignChunks(
        device: LedgerDevice,
        metaId: Long,
        chainId: ChainId,
        payload: InheritedImplication,
        cryptoScheme: CryptoScheme
    ): ByteArray {
        val chunks = prepareExtrinsicChunks(metaId, chainId, payload)

        val results = chunks.mapIndexed { index, chunk ->
            val chunkType = SubstrateLedgerAppCommon.SignPayloadType(index, chunks.size)

            val rawResponse = transport.send(
                cla = cla,
                ins = SubstrateLedgerAppCommon.Instruction.SIGN.code,
                p1 = chunkType.code,
                p2 = cryptoScheme.code,
                data = chunk,
                device = device
            )

            SubstrateLedgerAppCommon.processResponseCode(rawResponse)
        }

        return results.last()
    }

    private suspend fun prepareExtrinsicChunks(
        metaId: Long,
        chainId: ChainId,
        payload: InheritedImplication
    ): List<ByteArray> {
        val payloadBytes = payload.encoded()

        val derivationPath = getDerivationPath(metaId, chainId)
        val encodedDerivationPath = encodeDerivationPath(derivationPath)

        val encodedTxPayloadLength = payloadBytes.size.toShort().littleEndianBytes

        val extrinsicProof = metadataShortenerService.generateExtrinsicProof(payload).value

        val wholePayload = payloadBytes + extrinsicProof

        Log.d("Ledger", "Whole payload size: ${wholePayload.size}, metadata proof size: ${extrinsicProof.size}")

        val firstChunk = encodedDerivationPath + encodedTxPayloadLength
        val nextChunks = wholePayload.chunked(CHUNK_SIZE)

        return listOf(firstChunk) + nextChunks
    }
}
