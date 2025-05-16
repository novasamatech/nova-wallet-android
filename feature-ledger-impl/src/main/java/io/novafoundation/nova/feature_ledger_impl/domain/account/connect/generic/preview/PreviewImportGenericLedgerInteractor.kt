package io.novafoundation.nova.feature_ledger_impl.domain.account.connect.generic.preview

import io.novafoundation.nova.feature_account_api.presenatation.account.chain.preview.model.ChainAccountPreview
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.findDeviceOrThrow
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.GenericSubstrateLedgerApplication
import io.novafoundation.nova.runtime.ext.defaultComparator
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PreviewImportGenericLedgerInteractor {

    suspend fun getDevice(deviceId: String): LedgerDevice

    suspend fun availableChainAccounts(
        substrateAccountId: AccountId,
        evmAccountId: AccountId?,
    ): List<ChainAccountPreview>

    suspend fun verifyAddressOnLedger(accountIndex: Int, deviceId: String): Result<Unit>
}

class RealPreviewImportGenericLedgerInteractor(
    private val ledgerMigrationTracker: LedgerMigrationTracker,
    private val genericSubstrateLedgerApplication: GenericSubstrateLedgerApplication,
    private val ledgerDiscoveryService: LedgerDeviceDiscoveryService
) : PreviewImportGenericLedgerInteractor {

    override suspend fun getDevice(deviceId: String): LedgerDevice {
        return ledgerDiscoveryService.findDeviceOrThrow(deviceId)
    }

    override suspend fun availableChainAccounts(
        substrateAccountId: AccountId,
        evmAccountId: AccountId?,
    ): List<ChainAccountPreview> {
        return ledgerMigrationTracker.supportedChainsByGenericApp()
            .sortedWith(Chain.defaultComparator())
            .mapNotNull { chain ->
                if (chain.isEthereumBased) {
                    ChainAccountPreview(
                        chain = chain,
                        accountId = evmAccountId ?: return@mapNotNull null
                    )
                } else {
                    ChainAccountPreview(
                        chain = chain,
                        accountId = substrateAccountId
                    )
                }
            }
    }

    override suspend fun verifyAddressOnLedger(accountIndex: Int, deviceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val device = ledgerDiscoveryService.findDeviceOrThrow(deviceId)

            genericSubstrateLedgerApplication.getUniversalSubstrateAccount(device, accountIndex, confirmAddress = true)
            genericSubstrateLedgerApplication.getEvmAccount(device, accountIndex, confirmAddress = true)

            Unit
        }
    }
}
