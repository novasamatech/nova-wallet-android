package io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress

import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
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

class LedgerAccountWithBalance(
    val index: Int,
    val account: LedgerSubstrateAccount,
    val balance: BigInteger,
    val chainAsset: Chain.Asset
)

interface SelectAddressLedgerInteractor {

    suspend fun getDevice(deviceId: String): LedgerDevice

    suspend fun loadLedgerAccount(chain: Chain, deviceId: String, accountIndex: Int, ledgerVariant: LedgerVariant): Result<LedgerAccountWithBalance>

    suspend fun verifyLedgerAccount(chain: Chain, deviceId: String, accountIndex: Int, ledgerVariant: LedgerVariant): Result<Unit>
}

class RealSelectAddressLedgerInteractor(
    private val migrationUseCase: LedgerMigrationUseCase,
    private val ledgerDeviceDiscoveryService: LedgerDeviceDiscoveryService,
    private val assetSourceRegistry: AssetSourceRegistry,
) : SelectAddressLedgerInteractor {

    override suspend fun getDevice(deviceId: String): LedgerDevice {
        return ledgerDeviceDiscoveryService.findDeviceOrThrow(deviceId)
    }

    override suspend fun loadLedgerAccount(chain: Chain, deviceId: String, accountIndex: Int, ledgerVariant: LedgerVariant) = runCatching {
        val device = ledgerDeviceDiscoveryService.findDeviceOrThrow(deviceId)
        val app = migrationUseCase.determineLedgerApp(chain.id, ledgerVariant)

        val ledgerAccount = app.getAccount(device, chain.id, accountIndex, confirmAddress = false)

        val utilityAsset = chain.utilityAsset

        val accountId = chain.accountIdOf(ledgerAccount.publicKey)

        val balanceSource = assetSourceRegistry.sourceFor(utilityAsset).balance
        val balance = balanceSource.queryTotalBalance(chain, utilityAsset, accountId)

        LedgerAccountWithBalance(accountIndex, ledgerAccount, balance, utilityAsset)
    }

    override suspend fun verifyLedgerAccount(chain: Chain, deviceId: String, accountIndex: Int, ledgerVariant: LedgerVariant): Result<Unit> = runCatching {
        val device = ledgerDeviceDiscoveryService.findDeviceOrThrow(deviceId)
        val app = migrationUseCase.determineLedgerApp(chain.id, ledgerVariant)

        app.getAccount(device, chain.id, accountIndex, confirmAddress = true)
    }
}
