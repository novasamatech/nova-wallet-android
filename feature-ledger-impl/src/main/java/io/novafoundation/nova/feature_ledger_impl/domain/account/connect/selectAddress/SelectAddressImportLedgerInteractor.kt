package io.novafoundation.nova.feature_ledger_impl.domain.account.connect.selectAddress

import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.findDevice
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class LedgerAccountWithBalance(
    val index: Int,
    val account: LedgerSubstrateAccount,
    val balance: BigInteger,
    val token: Token
)

interface SelectAddressImportLedgerInteractor {

    suspend fun loadLedgerAccount(chain: Chain, deviceId: String, accountIndex: Int): Result<LedgerAccountWithBalance>
}

class RealSelectAddressImportLedgerInteractor(
    private val substrateLedgerApplication: SubstrateLedgerApplication,
    private val ledgerDeviceDiscoveryService: LedgerDeviceDiscoveryService,
    private val tokenRepository: TokenRepository,
    private val assetSourceRegistry: AssetSourceRegistry,
) : SelectAddressImportLedgerInteractor {

    override suspend fun loadLedgerAccount(chain: Chain, deviceId: String, accountIndex: Int) = runCatching {
        val device = ledgerDeviceDiscoveryService.findDevice(deviceId) ?: throw IllegalArgumentException("Device not found")
        val ledgerAccount = substrateLedgerApplication.getAccount(device, chain.id, accountIndex, confirmAddress = false)

        val utilityAsset = chain.utilityAsset

        val accountId = chain.accountIdOf(ledgerAccount.publicKey)

        val balanceSource = assetSourceRegistry.sourceFor(utilityAsset).balance
        val balance = balanceSource.queryTotalBalance(chain, utilityAsset, accountId)

        val token = tokenRepository.getToken(utilityAsset)

        LedgerAccountWithBalance(accountIndex, ledgerAccount, balance, token)
    }
}
