package io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate

import io.novafoundation.nova.common.utils.chunked
import io.novafoundation.nova.common.utils.dropBytes
import io.novafoundation.nova.common.utils.dropBytesLast
import io.novafoundation.nova.common.utils.isValidSS58Address
import io.novafoundation.nova.common.utils.toBigEndianU16
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplicationError
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransport
import io.novafoundation.nova.feature_ledger_api.sdk.transport.send
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.DisplayVerificationDialog.NO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.encrypt.json.copyBytes
import jp.co.soramitsu.fearless_utils.encrypt.junction.BIP32JunctionDecoder
import jp.co.soramitsu.fearless_utils.extensions.copyLast

private enum class Instruction(val code: UByte) {
    GET_ADDRESS(0x01u), SIGN(0x02u)
}

private enum class CryptoScheme(val code: UByte) {
    ED25519(0x01u), SR25519(0x02u)
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
    private val supportedApplications: List<SubstrateApplicationConfig>,
) : SubstrateLedgerApplication {

    override suspend fun getAccount(device: LedgerDevice, chainId: ChainId, accountIndex: Int): LedgerSubstrateAccount {
        val applicationConfig = getConfig(chainId)
        val displayVerificationDialog = NO

        val rawResponse = transport.send(
            cla = applicationConfig.cla,
            ins = Instruction.GET_ADDRESS.code,
            p1 = displayVerificationDialog.code,
            p2 = defaultCryptoScheme().code,
            data = buildDerivationPath(applicationConfig.coin, accountIndex),
            device = device
        )

        return parseAccountResponse(rawResponse)
    }

    override suspend fun getSignature(device: LedgerDevice, chainId: ChainId, accountIndex: Int, payload: ByteArray): ByteArray {
        val applicationConfig = getConfig(chainId)
        val derivationPath = buildDerivationPath(applicationConfig.coin, accountIndex)

        val chunks = listOf(derivationPath) + payload.chunked(CHUNK_SIZE)

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

        return results.last()
    }

    private fun parseAccountResponse(raw: ByteArray): LedgerSubstrateAccount {
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

        return LedgerSubstrateAccount(address = address, publicKey = publicKey)
    }

    private fun defaultCryptoScheme() = CryptoScheme.ED25519

    private fun getConfig(chainId: ChainId): SubstrateApplicationConfig {
        return supportedApplications.find { it.chainId == chainId }
            ?: throw SubstrateLedgerApplicationError.UnsupportedApp(chainId)
    }

    private fun buildDerivationPath(coin: Int, accountIndex: Int): ByteArray {
        val pathAsString = "//44//$coin//0/0/$accountIndex"
        val junctions = BIP32JunctionDecoder.decode(pathAsString).junctions

        return junctions.encodeToByteArray()
    }

    /**
     * Process response code and return data without response code
     */
    private fun processResponseCode(raw: ByteArray): ByteArray {
        val responseCodeData = raw.copyLast(RESPONSE_CODE_LENGTH)
        require(responseCodeData.size == RESPONSE_CODE_LENGTH) {
            "No response code"
        }
        val responseCode = responseCodeData.toBigEndianU16()
        val response = LedgerApplicationResponse.fromCode(responseCode)

        if (response != LedgerApplicationResponse.noError) {
            throw SubstrateLedgerApplicationError.Response(response)
        }

        return raw.dropBytesLast(RESPONSE_CODE_LENGTH)
    }
}
