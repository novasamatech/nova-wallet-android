package io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress

import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerEvmAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.findDeviceOrThrow
import io.novafoundation.nova.feature_ledger_impl.domain.migration.LedgerMigrationUseCase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class LedgerAccount(
    val index: Int,
    val substrate: LedgerSubstrateAccount,
    val evm: LedgerEvmAccount?,
)

interface SelectAddressLedgerInteractor {

    suspend fun getDevice(deviceId: String): LedgerDevice

    suspend fun loadLedgerAccount(chain: Chain, deviceId: String, accountIndex: Int, ledgerVariant: LedgerVariant): Result<LedgerAccount>

    suspend fun verifyLedgerAccount(chain: Chain, deviceId: String, accountIndex: Int, ledgerVariant: LedgerVariant): Result<Unit>
}

class RealSelectAddressLedgerInteractor(
    private val migrationUseCase: LedgerMigrationUseCase,
    private val ledgerDeviceDiscoveryService: LedgerDeviceDiscoveryService,
) : SelectAddressLedgerInteractor {

    override suspend fun getDevice(deviceId: String): LedgerDevice {
        return ledgerDeviceDiscoveryService.findDeviceOrThrow(deviceId)
    }

    override suspend fun loadLedgerAccount(chain: Chain, deviceId: String, accountIndex: Int, ledgerVariant: LedgerVariant) = runCatching {
        val device = ledgerDeviceDiscoveryService.findDeviceOrThrow(deviceId)
        val app = migrationUseCase.determineLedgerApp(chain.id, ledgerVariant)

        val substrateAccount = app.getSubstrateAccount(device, chain.id, accountIndex, confirmAddress = false)
        val evmAccount = app.getEvmAccount(device, chain.id, accountIndex, confirmAddress = false)

        LedgerAccount(accountIndex, substrateAccount, evmAccount)
    }

    override suspend fun verifyLedgerAccount(chain: Chain, deviceId: String, accountIndex: Int, ledgerVariant: LedgerVariant): Result<Unit> = runCatching {
        val device = ledgerDeviceDiscoveryService.findDeviceOrThrow(deviceId)
        val app = migrationUseCase.determineLedgerApp(chain.id, ledgerVariant)

        app.getSubstrateAccount(device, chain.id, accountIndex, confirmAddress = true)
    }
}
