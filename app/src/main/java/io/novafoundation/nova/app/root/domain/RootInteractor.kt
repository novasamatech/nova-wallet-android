package io.novafoundation.nova.app.root.domain

import io.novafoundation.nova.core.updater.Updater
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
    private val proxySyncService: ProxySyncService
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
}
