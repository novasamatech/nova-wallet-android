package io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate

import io.novafoundation.nova.common.utils.chunked
import io.novafoundation.nova.common.utils.dropBytes
import io.novafoundation.nova.common.utils.dropBytesLast
import io.novafoundation.nova.common.utils.isValidSS58Address
import io.novafoundation.nova.common.utils.toBigEndianU16
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateApplicationConfig
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplicationError
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransport
import io.novafoundation.nova.feature_ledger_api.sdk.transport.send
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerRepository
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.DisplayVerificationDialog.NO
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.DisplayVerificationDialog.YES
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.encrypt.json.copyBytes
import io.novasama.substrate_sdk_android.encrypt.junction.BIP32JunctionDecoder
import io.novasama.substrate_sdk_android.extensions.copyLast
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.encodedSignaturePayload

private enum class Instruction(val code: UByte) {
    GET_ADDRESS(0x01u), SIGN(0x02u)
}

private enum class CryptoScheme(val code: UByte) {
    ED25519(0x00u), SR25519(0x01u);

    companion object {
        fun fromCode(code: UByte): CryptoScheme {
            return values().first { it.code == code }
        }
    }
}

private enum class DisplayVerificationDialog(val code: UByte) {
    YES(0x01u), NO(0x00u)
}

private enum class SignPayloadType(val code: UByte) {
    FIRST(0x00u), ADD(0x01u), LAST(0x02u);
}

private fun SignPayloadType(chunkIndex: Int, total: Int): SignPayloadType {
    return when {
        chunkIndex == 0 -> SignPayloadType.FIRST
        chunkIndex < total - 1 -> SignPayloadType.ADD
        else -> SignPayloadType.LAST
    }
}

const val PUBLIC_KEY_LENGTH = 32
const val RESPONSE_CODE_LENGTH = 2
const val CHUNK_SIZE = 250

class RealSubstrateLedgerApplication(
    private val transport: LedgerTransport,
    private val ledgerRepository: LedgerRepository,
    private val supportedApplications: List<SubstrateApplicationConfig> = SubstrateApplicationConfig.all(),
) : SubstrateLedgerApplication {

    override suspend fun getAccount(
        device: LedgerDevice,
        chainId: ChainId,
        accountIndex: Int,
        confirmAddress: Boolean
    ): LedgerSubstrateAccount {
        val applicationConfig = getConfig(chainId)
        val displayVerificationDialog = if (confirmAddress) YES else NO

        val derivationPath = buildDerivationPath(applicationConfig.coin, accountIndex)
        val encodedDerivationPath = encodeDerivationPath(derivationPath)

        val rawResponse = transport.send(
            cla = applicationConfig.cla,
            ins = Instruction.GET_ADDRESS.code,
            p1 = displayVerificationDialog.code,
            p2 = defaultCryptoScheme().code,
            data = encodedDerivationPath,
            device = device
        )

        return parseAccountResponse(rawResponse, derivationPath)
    }

    override suspend fun getSignature(
        device: LedgerDevice,
        metaId: Long,
        chainId: ChainId,
        payload: SignerPayloadExtrinsic,
    ): SignatureWrapper {
        val payloadBytes = payload.encodedSignaturePayload(hashBigPayloads = false)
        val applicationConfig = getConfig(chainId)

        val derivationPath = ledgerRepository.getChainAccountDerivationPath(metaId, chainId)
        val encodedDerivationPath = encodeDerivationPath(derivationPath)

        val chunks = listOf(encodedDerivationPath) + payloadBytes.chunked(CHUNK_SIZE)

        val results = chunks.mapIndexed { index, chunk ->
            val chunkType = SignPayloadType(index, chunks.size)

            val rawResponse = transport.send(
                cla = applicationConfig.cla,
                ins = Instruction.SIGN.code,
                p1 = chunkType.code,
                p2 = defaultCryptoScheme().code,
                data = chunk,
                device = device
            )

            processResponseCode(rawResponse)
        }

        val signatureWithType = results.last()

        return parseSignature(signatureWithType)
    }

    private fun parseAccountResponse(raw: ByteArray, requestDerivationPath: String): LedgerSubstrateAccount {
        val dataWithoutResponseCode = processResponseCode(raw)

        val publicKey = dataWithoutResponseCode.copyBytes(0, PUBLIC_KEY_LENGTH)
        require(publicKey.size == PUBLIC_KEY_LENGTH) {
            "No public key"
        }

        val accountAddressData = dataWithoutResponseCode.dropBytes(PUBLIC_KEY_LENGTH)
        val address = accountAddressData.decodeToString()

        require(address.isValidSS58Address()) {
            "Invalid address"
        }

        val encryptionType = mapCryptoSchemeToEncryptionType(defaultCryptoScheme())

        return LedgerSubstrateAccount(
            address = address,
            publicKey = publicKey,
            encryptionType = encryptionType,
            derivationPath = requestDerivationPath
        )
    }

    private fun parseSignature(raw: ByteArray): SignatureWrapper {
        val cryptoSchemeByte = raw[0]
        val signature = raw.dropBytes(1)

        return when (CryptoScheme.fromCode(cryptoSchemeByte.toUByte())) {
            CryptoScheme.ED25519 -> SignatureWrapper.Ed25519(signature)
            CryptoScheme.SR25519 -> SignatureWrapper.Sr25519(signature)
        }
    }

    private fun defaultCryptoScheme() = CryptoScheme.ED25519

    private fun getConfig(chainId: ChainId): SubstrateApplicationConfig {
        return supportedApplications.find { it.chainId == chainId }
            ?: throw SubstrateLedgerApplicationError.UnsupportedApp(chainId)
    }

    private fun buildDerivationPath(coin: Int, accountIndex: Int): String {
        return "//44//$coin//$accountIndex//0//0"
    }

    private fun encodeDerivationPath(derivationPath: String): ByteArray {
        val junctions = BIP32JunctionDecoder.decode(derivationPath).junctions

        return junctions.serializeInLedgerFormat()
    }

    /**
     * Process response code and return data without response code
     */
    private fun processResponseCode(raw: ByteArray): ByteArray {
        val responseCodeData = raw.copyLast(RESPONSE_CODE_LENGTH)
        require(responseCodeData.size == RESPONSE_CODE_LENGTH) {
            "No response code"
        }
        val responseData = raw.dropBytesLast(RESPONSE_CODE_LENGTH)

        val responseCode = responseCodeData.toBigEndianU16()
        val response = LedgerApplicationResponse.fromCode(responseCode)

        if (response != LedgerApplicationResponse.NO_ERROR) {
            val errorMessage = if (responseData.isNotEmpty()) {
                responseData.decodeToString()
            } else {
                null
            }

            throw SubstrateLedgerApplicationError.Response(response, errorMessage)
        }

        return responseData
    }

    private fun mapCryptoSchemeToEncryptionType(cryptoScheme: CryptoScheme): EncryptionType {
        return when (cryptoScheme) {
            CryptoScheme.ED25519 -> EncryptionType.ED25519
            CryptoScheme.SR25519 -> EncryptionType.SR25519
        }
    }
}
