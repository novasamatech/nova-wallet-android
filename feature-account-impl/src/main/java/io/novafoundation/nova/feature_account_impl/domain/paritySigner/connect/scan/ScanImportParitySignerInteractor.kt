package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan

import io.novafoundation.nova.common.address.format.asAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ScanImportParitySignerInteractor {

    suspend fun decodeScanResult(scanResult: String): Result<ParitySignerAccount>
}

class RealScanImportParitySignerInteractor(
    private val connectQrDecoder: ParitySignerConnectQrDecoder,
) : ScanImportParitySignerInteractor {

    override suspend fun decodeScanResult(scanResult: String): Result<ParitySignerAccount> = withContext(Dispatchers.Default) {
        connectQrDecoder.decode(scanResult).mapCatching {
            val accountId = it.accountType.addressFormat.accountIdOf(it.address.asAddress())

            ParitySignerAccount(accountId.value, it.accountType)
        }
    }
}
