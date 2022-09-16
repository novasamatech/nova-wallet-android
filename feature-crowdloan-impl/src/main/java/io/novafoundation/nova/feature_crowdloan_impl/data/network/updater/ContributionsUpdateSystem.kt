package io.novafoundation.nova.feature_crowdloan_impl.data.network.updater

import io.novafoundation.nova.common.data.network.StorageSubscriptionBuilder
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdateSystemFactory
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdaterFactory
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge

class RealContributionsUpdateSystemFactory(
    private val chainRegistry: ChainRegistry,
    private val accountUpdateScope: AccountUpdateScope,
    private val contributionsRepository: ContributionsRepository,
    private val contributionsUpdaterFactory: ContributionsUpdaterFactory
) : ContributionsUpdateSystemFactory {

    override fun create(): UpdateSystem {
        return ContributionsUpdateSystem(
            chainRegistry,
            accountUpdateScope,
            contributionsRepository,
            contributionsUpdaterFactory
        )
    }
}

class ContributionsUpdateSystem(
    private val chainRegistry: ChainRegistry,
    private val accountUpdateScope: AccountUpdateScope,
    private val contributionsRepository: ContributionsRepository,
    private val contributionsUpdaterFactory: ContributionsUpdaterFactory
) : UpdateSystem {

    override fun start(): Flow<Updater.SideEffect> {
        return accountUpdateScope.invalidationFlow().flatMapLatest {
            chainRegistry.currentChains.first()
                .map { chain -> run(chain) }
                .merge()
        }.flowOn(Dispatchers.Default)
    }

    private fun run(chain: Chain): Flow<Updater.SideEffect> {
        return flow {
            val isCrowdloansAvailable = contributionsRepository.isCrowdloansAvailable(chain)
            if (!isCrowdloansAvailable) {
                return@flow
            }

            val socket = chainRegistry.getSocket(chain.id)
            val subscriptionBuilder = StorageSubscriptionBuilder.create(socket)
            val updater = contributionsUpdaterFactory.create(chain)

            kotlin.runCatching {
                updater.listenForUpdates(subscriptionBuilder)
            }.onSuccess { updaterFlow ->
                emitAll(updaterFlow)
            }
        }
    }
}
