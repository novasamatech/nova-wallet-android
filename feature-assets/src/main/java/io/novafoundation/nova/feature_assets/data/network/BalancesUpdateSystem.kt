package io.novafoundation.nova.feature_assets.data.network

import android.util.Log
import io.novafoundation.nova.common.data.repository.AutoEnableTokensRepository
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.common.utils.transformLatestDiffed
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_api.data.network.blockhain.updaters.PooledBalanceUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.BalanceLocksUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.PaymentUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AssetEnabledUpdate
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ethereum.subscribe
import io.novafoundation.nova.runtime.ext.isDisabled
import io.novafoundation.nova.runtime.ext.isFullSync
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import java.math.BigDecimal
import kotlin.coroutines.coroutineContext

class BalancesUpdateSystem(
    private val chainRegistry: ChainRegistry,
    private val paymentUpdaterFactory: PaymentUpdaterFactory,
    private val balanceLocksUpdater: BalanceLocksUpdaterFactory,
    private val pooledBalanceUpdaterFactory: PooledBalanceUpdaterFactory,
    private val accountUpdateScope: AccountUpdateScope,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val walletRepository: WalletRepository,
    private val chainAssetRepository: ChainAssetRepository,
    private val autoEnableTokensRepository: AutoEnableTokensRepository,
) : UpdateSystem {

    override fun start(): Flow<Updater.SideEffect> {
        return accountUpdateScope.invalidationFlow().flatMapLatest { metaAccount ->
            merge(
                chainRegistry.currentChains.transformLatestDiffed { chain ->
                    emitAll(balancesSync(chain, metaAccount))
                },
                autoEnableTokensWithBalance(metaAccount)
            )
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Shows tokens the wallet turns out to hold. Runs here rather than inside the payment updater
     * because a balance is only known once it has been written to the cache, and it has to see
     * assets across every chain to react to the ones that are still hidden.
     */
    private fun autoEnableTokensWithBalance(metaAccount: MetaAccount): Flow<Updater.SideEffect> {
        return autoEnableTokensRepository.autoEnableTokensFlow()
            .flatMapLatest { autoEnable ->
                if (autoEnable) walletRepository.syncedAssetsFlow(metaAccount.id) else emptyFlow()
            }
            .onEach(::enableAssetsWithPositiveBalance)
            .transform { }
    }

    private suspend fun enableAssetsWithPositiveBalance(assets: List<Asset>) {
        val toEnable = assets.filter { asset ->
            val chainAsset = asset.token.configuration

            // enabledOverriddenByUser is what keeps this from undoing a token the user hid on purpose
            !chainAsset.enabled && !chainAsset.enabledOverriddenByUser && asset.total > BigDecimal.ZERO
        }

        if (toEnable.isEmpty()) return

        val updates = toEnable.map { AssetEnabledUpdate.automatic(it.token.configuration.fullId, enabled = true) }

        chainAssetRepository.setAssetsEnabled(updates)
    }

    private suspend fun balancesSync(chain: Chain, metaAccount: MetaAccount): Flow<Updater.SideEffect> {
        return when {
            !metaAccount.hasAccountIn(chain) -> emptyFlow()
            chain.connectionState.isDisabled -> emptyFlow()
            chain.canPerformFullSync() -> fullBalancesSync(chain, metaAccount)
            else -> lightBalancesSync(chain, metaAccount)
        }
    }

    private suspend fun fullBalancesSync(
        chain: Chain,
        metaAccount: MetaAccount,
    ): Flow<Updater.SideEffect> {
        return launchChainUpdaters(
            chain = chain,
            metaAccount = metaAccount,
            createUpdaters = { createFullSyncUpdaters(chain) }
        )
    }

    private suspend fun lightBalancesSync(
        chain: Chain,
        metaAccount: MetaAccount,
    ): Flow<Updater.SideEffect> {
        return launchChainUpdaters(
            chain = chain,
            metaAccount = metaAccount,
            createUpdaters = { createLightSyncUpdaters(chain) }
        )
    }

    private suspend fun launchChainUpdaters(
        chain: Chain,
        metaAccount: MetaAccount,
        createUpdaters: suspend () -> List<Updater<MetaAccount>>
    ): Flow<Updater.SideEffect> {
        return flow {
            val subscriptionBuilder = storageSharedRequestsBuilderFactory.create(chain.id)

            val updaters = createUpdaters()

            val sideEffectFlows = updaters.map { updater ->
                try {
                    updater.listenForUpdates(subscriptionBuilder, metaAccount).catch { logError(chain, it) }
                } catch (e: Exception) {
                    emptyFlow()
                }
            }

            subscriptionBuilder.subscribe(coroutineContext)
            val resultFlow = sideEffectFlows.mergeIfMultiple()

            emitAll(resultFlow)
        }.catch { logError(chain, it) }
    }

    private fun Chain.canPerformFullSync(): Boolean {
        return connectionState.isFullSync || !hasSubstrateRuntime
    }

    private fun createFullSyncUpdaters(chain: Chain): List<Updater<MetaAccount>> {
        return listOf(
            paymentUpdaterFactory.createFullSync(chain),
            balanceLocksUpdater.create(chain),
            pooledBalanceUpdaterFactory.create(chain)
        )
    }

    private fun createLightSyncUpdaters(chain: Chain): List<Updater<MetaAccount>> {
        return listOf(
            paymentUpdaterFactory.createLightSync(chain),
        )
    }

    private fun logError(chain: Chain, error: Throwable) {
        Log.e(LOG_TAG, "Failed to subscribe to balances in ${chain.name}: ${error.message}", error)
    }
}
