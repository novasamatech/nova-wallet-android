package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan

import jp.co.soramitsu.fearless_utils.encrypt.qr.QrFormat
import jp.co.soramitsu.fearless_utils.encrypt.qr.formats.SubstrateQrFormat
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ParitySignerAccount(
    val accountId: AccountId,
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

            ParitySignerAccount(parsed.address.toAccountId())
        }
    }
}
