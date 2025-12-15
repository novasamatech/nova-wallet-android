package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan

import io.novafoundation.nova.feature_account_impl.domain.utils.ScanSecret
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ParitySignerAccount(val accountId: AccountId) {

    class Public(accountId: AccountId) : ParitySignerAccount(accountId)

    class Secret(accountId: AccountId, val secret: ScanSecret) : ParitySignerAccount(accountId)
}

interface ScanImportParitySignerInteractor {

    suspend fun decodeScanResult(scanResult: String): Result<ParitySignerAccount>
}

class RealScanImportParitySignerInteractor(
    private val polkadotVaultScanFormat: PolkadotVaultScanFormat
) : ScanImportParitySignerInteractor {

    override suspend fun decodeScanResult(scanResult: String): Result<ParitySignerAccount> {
        return withContext(Dispatchers.Default) {
            polkadotVaultScanFormat.decode(scanResult)
        }
    }
}
