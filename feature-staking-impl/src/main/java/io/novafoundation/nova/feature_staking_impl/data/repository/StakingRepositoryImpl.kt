package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.NonNullBinderWithType
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.numberConstantOrNull
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.AccountStakingDao
import io.novafoundation.nova.core_db.model.AccountStakingLocal
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.ExposureOverview
import io.novafoundation.nova.feature_staking_api.domain.model.ExposurePage
import io.novafoundation.nova.feature_staking_api.domain.model.IndividualExposure
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_api.domain.model.SlashingSpans
import io.novafoundation.nova.feature_staking_api.domain.model.StakingLedger
import io.novafoundation.nova.feature_staking_api.domain.model.StakingStory
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.erasStartSessionIndex
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.ledger
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.staking
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindActiveEra
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindCurrentEra
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindExposure
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindExposureOverview
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindExposurePage
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindHistoryDepth
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindMaxNominators
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindMinBond
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindNominations
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindNominatorsCount
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindRewardDestination
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindSlashDeferDuration
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindSlashingSpans
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindValidatorPrefs
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.ValidatorExposureUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.activeEraStorageKey
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.StakingStoriesDataSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.api.queryNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novafoundation.nova.runtime.storage.source.queryNonNull
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.math.BigInteger
import kotlinx.coroutines.flow.map

class StakingRepositoryImpl(
    private val accountStakingDao: AccountStakingDao,
    private val remoteStorage: StorageDataSource,
    private val localStorage: StorageDataSource,
    private val walletConstants: WalletConstants,
    private val chainRegistry: ChainRegistry,
    private val stakingStoriesDataSource: StakingStoriesDataSource,
    private val storageCache: StorageCache,
) : StakingRepository {

    override suspend fun eraStartSessionIndex(chainId: ChainId, currentEra: BigInteger): EraIndex {
        return localStorage.query(chainId) {
            metadata.staking.erasStartSessionIndex.queryNonNull(currentEra)
        }
    }

    override suspend fun eraLength(chainId: ChainId): BigInteger {
        val runtime = runtimeFor(chainId)

        return runtime.metadata.staking().numberConstant("SessionsPerEra", runtime) // How many sessions per era
    }

    override suspend fun getActiveEraIndex(chainId: ChainId): EraIndex = localStorage.queryNonNull(
        keyBuilder = { it.metadata.activeEraStorageKey() },
        binding = ::bindActiveEra,
        chainId = chainId
    )

    override suspend fun getCurrentEraIndex(chainId: ChainId): EraIndex = localStorage.queryNonNull(
        keyBuilder = { it.metadata.staking().storage("CurrentEra").storageKey() },
        binding = ::bindCurrentEra,
        chainId = chainId
    )

    override suspend fun getHistoryDepth(chainId: ChainId): BigInteger {
        val runtime = runtimeFor(chainId)
        val fromConstants = runtime.metadata.staking().numberConstantOrNull("HistoryDepth", runtime)

        return fromConstants ?: localStorage.queryNonNull(
            keyBuilder = { it.metadata.staking().storage("HistoryDepth").storageKey() },
            binding = ::bindHistoryDepth,
            chainId = chainId
        )
    }

    override fun observeActiveEraIndex(chainId: String) = localStorage.observeNonNull(
        chainId = chainId,
        keyBuilder = { it.metadata.activeEraStorageKey() },
        binding = { scale, runtime -> bindActiveEra(scale, runtime) }
    )

    override suspend fun getElectedValidatorsExposure(chainId: ChainId, eraIndex: EraIndex): AccountIdMap<Exposure> {
        val isPagedExposures = isPagedExposuresUsed(chainId)

        return if (isPagedExposures) {
            fetchPagedEraStakers(chainId, eraIndex)
        } else {
            fetchLegacyEraStakers(chainId, eraIndex)
        }
    }

    private suspend fun fetchPagedEraStakers(chainId: ChainId, eraIndex: EraIndex): AccountIdMap<Exposure> = localStorage.query(chainId) {
        val eraStakersOverview = metadata.staking().storage("ErasStakersOverview").entries(
            eraIndex,
            keyExtractor = { (_: BigInteger, accountId: ByteArray) -> accountId.toHexString() },
            binding = { instance, _ -> bindExposureOverview(instance) }
        )

        val eraStakersPaged = runtime.metadata.staking().storage("ErasStakersPaged").entries(
            eraIndex,
            keyExtractor = { (_: BigInteger, accountId: ByteArray, page: BigInteger) -> accountId.toHexString() to page.toInt() },
            binding = { instance, _ -> bindExposurePage(instance) }
        )

        mergeOverviewsAndPagedOthers(eraStakersOverview, eraStakersPaged)
    }

    private fun mergeOverviewsAndPagedOthers(
        eraStakerOverviews: AccountIdMap<ExposureOverview>,
        othersPaged: Map<Pair<String, Int>, ExposurePage>
    ): AccountIdMap<Exposure> {
        return eraStakerOverviews.mapValues { (accountId, overview) ->
            // avoid unnecessary growth allocations by pre-allocating exact needed number of elements
            val others = ArrayList<IndividualExposure>(overview.nominatorCount.toInt())

            (0 until overview.pageCount.toInt()).forEach { pageNumber ->
                val page = othersPaged[accountId to pageNumber]?.others.orEmpty()
                others.addAll(page)
            }

            Exposure(
                total = overview.total,
                own = overview.own,
                others = others
            )
        }
    }

    private suspend fun fetchLegacyEraStakers(chainId: ChainId, eraIndex: EraIndex): AccountIdMap<Exposure> = localStorage.query(chainId) {
        runtime.metadata.staking().storage("ErasStakers").entries(
            eraIndex,
            keyExtractor = { (_: BigInteger, accountId: ByteArray) -> accountId.toHexString() },
            binding = { instance, _ -> bindExposure(instance) }
        )
    }

    override suspend fun getValidatorPrefs(
        chainId: ChainId,
        accountIdsHex: Collection<String>,
    ): AccountIdMap<ValidatorPrefs?> {
        return remoteStorage.query(chainId) {
            runtime.metadata.staking().storage("Validators").entries(
                keysArguments = accountIdsHex.map { listOf(it.fromHex()) },
                keyExtractor = { (accountId: AccountId) -> accountId.toHexString() },
                binding = { decoded, _ -> decoded?.let { bindValidatorPrefs(decoded) } }
            )
        }
    }

    override suspend fun getSlashes(chainId: ChainId, accountIdsHex: Collection<String>): AccountIdMap<Boolean> = withContext(Dispatchers.Default) {
        remoteStorage.query(chainId) {
            val activeEraIndex = getActiveEraIndex(chainId)

            val slashDeferDurationConstant = runtime.metadata.staking().constant("SlashDeferDuration")
            val slashDeferDuration = bindSlashDeferDuration(slashDeferDurationConstant, runtime)

            runtime.metadata.staking().storage("SlashingSpans").entries(
                keysArguments = accountIdsHex.map { listOf(it.fromHex()) },
                keyExtractor = { (accountId: AccountId) -> accountId.toHexString() },
                binding = { decoded, _ ->
                    val span = decoded?.let { bindSlashingSpans(it) }

                    isSlashed(span, activeEraIndex, slashDeferDuration)
                }
            )
        }
    }

    override suspend fun getSlashingSpan(chainId: ChainId, accountId: AccountId): SlashingSpans? {
        return remoteStorage.query(
            keyBuilder = { it.metadata.staking().storage("SlashingSpans").storageKey(it, accountId) },
            binding = { scale, runtimeSnapshot -> scale?.let { bindSlashingSpans(it, runtimeSnapshot) } },
            chainId = chainId
        )
    }

    override fun stakingStateFlow(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId
    ): Flow<StakingState> {
        return accountStakingDao.observeDistinct(chain.id, chainAsset.id, accountId)
            .flatMapLatest { accountStaking ->
                val accessInfo = accountStaking.stakingAccessInfo

                if (accessInfo == null) {
                    flowOf(StakingState.NonStash(chain, chainAsset))
                } else {
                    observeStashState(chain, chainAsset, accessInfo, accountId)
                }
            }
    }

    override suspend fun getRewardDestination(stakingState: StakingState.Stash) = localStorage.queryNonNull(
        keyBuilder = { it.metadata.staking().storage("Payee").storageKey(it, stakingState.stashId) },
        binding = { scale, runtime -> bindRewardDestination(scale, runtime, stakingState.stashId, stakingState.controllerId) },
        chainId = stakingState.chain.id
    )

    override suspend fun minimumNominatorBond(chainId: ChainId): BigInteger {
        val minBond = queryStorageIfExists(
            storageName = "MinNominatorBond",
            binder = ::bindMinBond,
            chainId = chainId
        ) ?: BigInteger.ZERO

        val existentialDeposit = walletConstants.existentialDeposit(chainId)

        return minBond.max(existentialDeposit)
    }

    override suspend fun maxNominators(chainId: ChainId): BigInteger? = queryStorageIfExists(
        storageName = "MaxNominatorsCount",
        binder = ::bindMaxNominators,
        chainId = chainId
    )

    override suspend fun nominatorsCount(chainId: ChainId): BigInteger? = queryStorageIfExists(
        storageName = "CounterForNominators",
        binder = ::bindNominatorsCount,
        chainId = chainId
    )

    private suspend fun <T> queryStorageIfExists(
        chainId: ChainId,
        storageName: String,
        binder: NonNullBinderWithType<T>,
    ): T? {
        val runtime = runtimeFor(chainId)

        return runtime.metadata.staking().storageOrNull(storageName)?.let { storageEntry ->
            localStorage.query(
                keyBuilder = { storageEntry.storageKey() },
                binding = { scale, _ -> scale?.let { binder(scale, runtime, storageEntry.returnType()) } },
                chainId = chainId
            )
        }
    }

    override fun stakingStoriesFlow(): Flow<List<StakingStory>> {
        return stakingStoriesDataSource.getStoriesFlow()
    }

    override fun ledgerFlow(stakingState: StakingState.Stash): Flow<StakingLedger> {
        return localStorage.subscribe(stakingState.chain.id) {
            metadata.staking.ledger.observe(stakingState.controllerId)
        }.filterNotNull()
    }

    override suspend fun ledger(chainId: ChainId, accountId: AccountId): StakingLedger? = remoteStorage.query(chainId) {
        metadata.staking.ledger.query(accountId)
    }

    private fun observeStashState(
        chain: Chain,
        chainAsset: Chain.Asset,
        accessInfo: AccountStakingLocal.AccessInfo,
        accountId: AccountId,
    ): Flow<StakingState.Stash> {
        val stashId = accessInfo.stashId
        val controllerId = accessInfo.controllerId

        return combine(
            observeAccountNominations(chain.id, stashId),
            observeAccountValidatorPrefs(chain.id, stashId)
        ) { nominations, prefs ->
            when {
                prefs != null -> StakingState.Stash.Validator(
                    chain,
                    chainAsset,
                    accountId,
                    controllerId,
                    stashId,
                    prefs
                )
                nominations != null -> StakingState.Stash.Nominator(
                    chain,
                    chainAsset,
                    accountId,
                    controllerId,
                    stashId,
                    nominations
                )

                else -> StakingState.Stash.None(chain, chainAsset, accountId, controllerId, stashId)
            }
        }
    }

    private suspend fun isPagedExposuresUsed(chainId: ChainId): Boolean {
        val isPagedExposuresValue = storageCache.getEntry(ValidatorExposureUpdater.STORAGE_KEY_PAGED_EXPOSURES, chainId)

        return ValidatorExposureUpdater.decodeIsPagedExposuresValue(isPagedExposuresValue.content)
    }

    private fun observeAccountValidatorPrefs(chainId: ChainId, stashId: AccountId): Flow<ValidatorPrefs?> {
        return localStorage.subscribe(chainId) {
            runtime.metadata.staking().storage("Validators").observe(
                stashId,
                binding = { decoded -> decoded?.let { bindValidatorPrefs(decoded) } }
            )
        }
    }

    private fun observeAccountNominations(chainId: ChainId, stashId: AccountId): Flow<Nominations?> {
        return localStorage.observe(
            chainId = chainId,
            keyBuilder = { it.metadata.staking().storage("Nominators").storageKey(it, stashId) },
            binder = { scale, runtime -> scale?.let { bindNominations(it, runtime) } }
        )
    }

    private fun isSlashed(
        slashingSpans: SlashingSpans?,
        activeEraIndex: BigInteger,
        slashDeferDuration: BigInteger,
    ) = slashingSpans != null && activeEraIndex - slashingSpans.lastNonZeroSlash < slashDeferDuration

    private suspend fun runtimeFor(chainId: String) = chainRegistry.getRuntime(chainId)
}
