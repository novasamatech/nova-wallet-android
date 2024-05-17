package io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate

import android.util.Log
import io.novafoundation.nova.common.utils.dropBytes
import io.novafoundation.nova.common.utils.dropBytesLast
import io.novafoundation.nova.common.utils.isValidSS58Address
import io.novafoundation.nova.common.utils.toBigEndianU16
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateApplicationConfig
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplicationError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.encrypt.json.copyBytes
import io.novasama.substrate_sdk_android.encrypt.junction.BIP32JunctionDecoder
import io.novasama.substrate_sdk_android.extensions.copyLast

object SubstrateLedgerAppCommon {

    const val CHUNK_SIZE = 250


    private const val PUBLIC_KEY_LENGTH = 32
    private const val RESPONSE_CODE_LENGTH = 2

    enum class Instruction(val code: UByte) {
        GET_ADDRESS(0x01u), SIGN(0x02u)
    }

    enum class CryptoScheme(val code: UByte) {
        ED25519(0x00u), SR25519(0x01u);

        companion object {
            fun fromCode(code: UByte): CryptoScheme {
                return values().first { it.code == code }
            }
        }
    }

    enum class DisplayVerificationDialog(val code: UByte) {
        YES(0x01u), NO(0x00u)
    }

    enum class SignPayloadType(val code: UByte) {
        FIRST(0x00u), ADD(0x01u), LAST(0x02u);
    }

    fun defaultCryptoScheme() = CryptoScheme.ED25519

    fun SignPayloadType(chunkIndex: Int, total: Int): SignPayloadType {
        return when {
            chunkIndex == 0 -> SignPayloadType.FIRST
            chunkIndex < total - 1 -> SignPayloadType.ADD
            else -> SignPayloadType.LAST
        }
    }

    fun parseSignature(raw: ByteArray): SignatureWrapper {
        val cryptoSchemeByte = raw[0]
        val signature = raw.dropBytes(1)

        return when (CryptoScheme.fromCode(cryptoSchemeByte.toUByte())) {
            CryptoScheme.ED25519 -> SignatureWrapper.Ed25519(signature)
            CryptoScheme.SR25519 -> SignatureWrapper.Sr25519(signature)
        }
    }

    fun encodeDerivationPath(derivationPath: String): ByteArray {
        val junctions = BIP32JunctionDecoder.decode(derivationPath).junctions

        return junctions.serializeInLedgerFormat()
    }

    fun parseAccountResponse(raw: ByteArray, requestDerivationPath: String): LedgerSubstrateAccount {
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

    /**
     * Process response code and return data without response code
     */
    fun processResponseCode(raw: ByteArray): ByteArray {
        val responseCodeData = raw.copyLast(RESPONSE_CODE_LENGTH)
        require(responseCodeData.size == RESPONSE_CODE_LENGTH) {
            "No response code"
        }
        val responseData = raw.dropBytesLast(RESPONSE_CODE_LENGTH)

        val responseCode = responseCodeData.toBigEndianU16()

        Log.d("Ledger", "Ledger response code: $responseCode")

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

    fun List<SubstrateApplicationConfig>.getConfig(chainId: ChainId): SubstrateApplicationConfig {
        return find { it.chainId == chainId }
            ?: throw SubstrateLedgerApplicationError.UnsupportedApp(chainId)
    }

    fun buildDerivationPath(coin: Int, accountIndex: Int): String {
        return "//44//$coin//$accountIndex//0//0"
    }

    private fun mapCryptoSchemeToEncryptionType(cryptoScheme: CryptoScheme): EncryptionType {
        return when (cryptoScheme) {
            CryptoScheme.ED25519 -> EncryptionType.ED25519
            CryptoScheme.SR25519 -> EncryptionType.SR25519
        }
    }
}

