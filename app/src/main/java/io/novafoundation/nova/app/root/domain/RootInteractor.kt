package io.novafoundation.nova.app.root.domain

import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_impl.data.buyToken.ExternalProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class RootInteractor(
    private val updateSystem: UpdateSystem,
    private val walletRepository: WalletRepository,
) {

    fun runBalancesUpdate(): Flow<Updater.SideEffect> = updateSystem.start()

    fun isBuyProviderRedirectLink(link: String) = ExternalProvider.REDIRECT_URL_BASE in link

    fun stakingAvailableFlow() = flowOf(true) // TODO remove this logic

    suspend fun updatePhishingAddresses() {
        runCatching {
            walletRepository.updatePhishingAddresses()
        }
    }
}
