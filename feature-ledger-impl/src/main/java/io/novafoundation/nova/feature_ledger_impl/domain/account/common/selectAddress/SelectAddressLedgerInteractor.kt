package io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress

import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerEvmAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.findDeviceOrThrow
import io.novafoundation.nova.feature_ledger_impl.domain.migration.LedgerMigrationUseCase
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class LedgerAccount(
    val index: Int,
    val substrate: LedgerSubstrateAccount,
    val evm: LedgerEvmAccount?,
)

interface SelectAddressLedgerInteractor {

    suspend fun getDevice(deviceId: String): LedgerDevice

    suspend fun loadLedgerAccount(substrateChain: Chain, deviceId: String, accountIndex: Int, ledgerVariant: LedgerVariant): Result<LedgerAccount>

    suspend fun verifyLedgerAccount(
        substrateChain: Chain,
        deviceId: String,
        accountIndex: Int,
        ledgerVariant: LedgerVariant,
        addressSchemes: List<AddressScheme>
    ): Result<Unit>
}

class RealSelectAddressLedgerInteractor(
    private val migrationUseCase: LedgerMigrationUseCase,
    private val ledgerDeviceDiscoveryService: LedgerDeviceDiscoveryService,
) : SelectAddressLedgerInteractor {

    override suspend fun getDevice(deviceId: String): LedgerDevice {
        return ledgerDeviceDiscoveryService.findDeviceOrThrow(deviceId)
    }

    override suspend fun loadLedgerAccount(
        substrateChain: Chain,
        deviceId: String,
        accountIndex: Int,
        ledgerVariant: LedgerVariant,
    ) = runCatching {
        val device = ledgerDeviceDiscoveryService.findDeviceOrThrow(deviceId)
        val app = migrationUseCase.determineLedgerApp(substrateChain.id, ledgerVariant)

        val substrateAccount = app.getSubstrateAccount(device, substrateChain.id, accountIndex, confirmAddress = false)
        val evmAccount = app.getEvmAccount(device, accountIndex, confirmAddress = false)

        LedgerAccount(accountIndex, substrateAccount, evmAccount)
    }

    override suspend fun verifyLedgerAccount(
        substrateChain: Chain,
        deviceId: String,
        accountIndex: Int,
        ledgerVariant: LedgerVariant,
        addressSchemes: List<AddressScheme>
    ): Result<Unit> = runCatching {
        val device = ledgerDeviceDiscoveryService.findDeviceOrThrow(deviceId)
        val app = migrationUseCase.determineLedgerApp(substrateChain.id, ledgerVariant)

        val verificationPerScheme = mapOf(
            AddressScheme.SUBSTRATE to suspend { app.getSubstrateAccount(device, substrateChain.id, accountIndex, confirmAddress = true) },
            AddressScheme.EVM to suspend { app.getEvmAccount(device, accountIndex, confirmAddress = true) }
        )

        addressSchemes.forEach {
            verificationPerScheme.getValue(it).invoke()
        }
    }
}
