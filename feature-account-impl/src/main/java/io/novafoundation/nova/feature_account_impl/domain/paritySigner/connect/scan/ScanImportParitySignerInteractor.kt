package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan

import jp.co.soramitsu.fearless_utils.encrypt.qr.QrFormat
import jp.co.soramitsu.fearless_utils.encrypt.qr.formats.SubstrateQrFormat
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ParitySignerAccount(
    val publicKey: ByteArray,
    val accountId: AccountId,
    val address: String,
    val name: String?,
)

interface ScanImportParitySignerInteractor {

    suspend fun decodeScanResult(scanResult: String): Result<ParitySignerAccount>
}

class RealScanImportParitySignerInteractor(
    private val addressQrFormat: QrFormat = SubstrateQrFormat()
) : ScanImportParitySignerInteractor {

    override suspend fun decodeScanResult(scanResult: String): Result<ParitySignerAccount> = runCatching {
        withContext(Dispatchers.Default) {
            val parsed = addressQrFormat.decode(scanResult)

            ParitySignerAccount(
                name = parsed.name,
                publicKey = parsed.publicKey!!,
                address = parsed.address,
                accountId = parsed.address.toAccountId()
            )
        }
    }
}
