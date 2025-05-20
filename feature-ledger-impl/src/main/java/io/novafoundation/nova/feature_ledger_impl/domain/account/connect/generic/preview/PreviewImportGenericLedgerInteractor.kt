package io.novafoundation.nova.feature_ledger_impl.domain.account.connect.generic.preview

import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.preview.model.ChainAccountPreview
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.findDeviceOrThrow
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.GenericSubstrateLedgerApplication
import io.novafoundation.nova.runtime.ext.addressScheme
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
    ): GroupedList<AddressScheme, ChainAccountPreview>

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
    ): GroupedList<AddressScheme, ChainAccountPreview> {
        return ledgerMigrationTracker.supportedChainsByGenericApp()
            .groupBy(Chain::addressScheme)
            .mapValuesNotNull { (scheme, chains) ->
                val accountId = when(scheme) {
                    AddressScheme.EVM -> evmAccountId ?: return@mapValuesNotNull null
                    AddressScheme.SUBSTRATE -> substrateAccountId
                }

                chains
                    .sortedWith(Chain.defaultComparator())
                    .map { chain -> ChainAccountPreview(chain, accountId) }
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
