package io.novafoundation.nova.app.root.domain

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigDiscoveryService
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.data.network.BalancesUpdateSystem
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class RootInteractor(
    private val updateSystem: BalancesUpdateSystem,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val proxySyncService: ProxySyncService,
    private val multisigDiscoveryService: MultisigDiscoveryService,
    private val multisigPendingOperationsService: MultisigPendingOperationsService,
) {

    fun runBalancesUpdate(): Flow<Updater.SideEffect> = updateSystem.start()

    suspend fun updatePhishingAddresses() {
        runCatching {
            walletRepository.updatePhishingAddresses()
        }
    }

    suspend fun isAccountSelected(): Boolean {
        return accountRepository.isAccountSelected()
    }

    suspend fun isPinCodeSet(): Boolean {
        return accountRepository.isCodeSet()
    }

    fun syncProxies(): Flow<*> {
        return proxySyncService.proxySyncTrigger().mapLatest {
            proxySyncService.startSyncingSuspend()
        }
    }

    fun syncMultisigs(): Flow<*> {
        return multisigDiscoveryService.automaticAccountDiscoverySync()
    }

    context(ComputationalScope)
    fun syncPendingMultisigOperations(): Flow<Unit> {
        return multisigPendingOperationsService.performMultisigOperationsSync()
    }
}
