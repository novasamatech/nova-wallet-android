package io.novafoundation.nova.feature_ledger_impl.domain.account.connect.generic.preview

import io.novafoundation.nova.feature_account_api.presenatation.account.chain.preview.model.ChainAccountPreview
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.findDeviceOrThrow
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.GenericSubstrateLedgerApplication
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PreviewImportGenericLedgerInteractor {

    suspend fun getDevice(deviceId: String): LedgerDevice

    suspend fun availableChainAccounts(addressFromLedger: String): List<ChainAccountPreview>

    suspend fun verifyAddressOnLedger(deviceId: String): Result<Unit>
}

class RealPreviewImportGenericLedgerInteractor(
    private val ledgerMigrationTracker: LedgerMigrationTracker,
    private val genericSubstrateLedgerApplication: GenericSubstrateLedgerApplication,
    private val ledgerDiscoveryService: LedgerDeviceDiscoveryService
) : PreviewImportGenericLedgerInteractor {

    override suspend fun getDevice(deviceId: String): LedgerDevice {
        return ledgerDiscoveryService.findDeviceOrThrow(deviceId)
    }

    override suspend fun availableChainAccounts(addressFromLedger: String): List<ChainAccountPreview> {
        val accountId = addressFromLedger.toAccountId()

        return ledgerMigrationTracker.supportedChainsByGenericApp().map { chain ->
            ChainAccountPreview(
                chain = chain,
                accountId = accountId
            )
        }
    }

    override suspend fun verifyAddressOnLedger(deviceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val device = ledgerDiscoveryService.findDeviceOrThrow(deviceId)

            genericSubstrateLedgerApplication.getUniversalAccount(device, confirmAddress = true)

            Unit
        }
    }
}
