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
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.defaultCryptoScheme
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.encodeDerivationPath
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.SubstrateLedgerAppCommon.parseAccountResponse
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.encodedSignaturePayload

abstract class NewSubstrateLedgerApplication(
    private val transport: LedgerTransport,
    private val chainRegistry: ChainRegistry,
    private val metadataShortenerService: MetadataShortenerService
) : SubstrateLedgerApplication {

    abstract val cla: UByte

    abstract suspend fun getDerivationPath(chainId: ChainId, accountIndex: Int): String

    abstract suspend fun getDerivationPath(metaId: Long, chainId: ChainId): String

    abstract suspend fun getAddressPrefix(chainId: ChainId): Short

    override suspend fun getAccount(device: LedgerDevice, chainId: ChainId, accountIndex: Int, confirmAddress: Boolean): LedgerSubstrateAccount {
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

        Log.w("Ledger", "Got response (${rawResponse.size} bytes): ${rawResponse.joinToString()}")

        return parseAccountResponse(rawResponse, derivationPath)
    }

    override suspend fun getSignature(
        device: LedgerDevice,
        metaId: Long,
        chainId: ChainId,
        payload: SignerPayloadExtrinsic
    ): SignatureWrapper {
        val chunks = prepareExtrinsicChunks(metaId, chainId, payload)

        val results = chunks.mapIndexed { index, chunk ->
            val chunkType = SubstrateLedgerAppCommon.SignPayloadType(index, chunks.size)

            val rawResponse = transport.send(
                cla = cla,
                ins = SubstrateLedgerAppCommon.Instruction.SIGN.code,
                p1 = chunkType.code,
                p2 = defaultCryptoScheme().code,
                data = chunk,
                device = device
            )

            SubstrateLedgerAppCommon.processResponseCode(rawResponse)
        }

        val signatureWithType = results.last()

        return SubstrateLedgerAppCommon.parseSignature(signatureWithType)
    }

    private suspend fun prepareExtrinsicChunks(
        metaId: Long,
        chainId: ChainId,
        payload: SignerPayloadExtrinsic
    ): List<ByteArray> {
        val payloadBytes = payload.encodedSignaturePayload(hashBigPayloads = false)

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
