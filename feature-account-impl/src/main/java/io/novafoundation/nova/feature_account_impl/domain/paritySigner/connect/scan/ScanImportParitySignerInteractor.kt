package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan

import io.novasama.substrate_sdk_android.encrypt.qr.QrFormat
import io.novasama.substrate_sdk_android.encrypt.qr.formats.SubstrateQrFormat
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
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
