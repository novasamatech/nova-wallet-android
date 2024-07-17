package io.novafoundation.nova.feature_governance_impl.data.repository.v1

import android.util.Log
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindCollectionEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.democracy
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.padEnd
import io.novafoundation.nova.common.utils.scheduler
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ConfirmingSource
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.DecidingStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackQueue
import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository
import io.novafoundation.nova.feature_governance_api.data.thresold.gov1.Gov1DelayedThresholdPassing
import io.novafoundation.nova.feature_governance_api.data.thresold.gov1.Gov1VotingThreshold
import io.novafoundation.nova.feature_governance_impl.data.repository.common.bindProposal
import io.novafoundation.nova.feature_governance_impl.data.repository.common.bindTally
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.extensions.pad
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.FixedByteArray
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.u32
import io.novasama.substrate_sdk_android.runtime.definitions.types.toByteArray
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.keys
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

private const val SCHEDULER_KEY_BOUND = 32

private enum class SchedulerVersion {
    V3, V4
}

class GovV1OnChainReferendaRepository(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val totalIssuanceRepository: TotalIssuanceRepository,
) : OnChainReferendaRepository {

    override suspend fun electorate(chainId: ChainId): Balance {
        return totalIssuanceRepository.getTotalIssuance(chainId)
    }

    override suspend fun undecidingTimeout(chainId: ChainId): BlockNumber {
        // we do not support `in queue` status for gov v1 yet
        return Balance.ZERO
    }

    override suspend fun getTracks(chainId: ChainId): Collection<TrackInfo> {
        val runtime = chainRegistry.getRuntime(chainId)

        val track = TrackInfo(
            id = DemocracyTrackId,
            name = "root",
            preparePeriod = Balance.ZERO,
            decisionPeriod = runtime.votingPeriod(),
            minSupport = null,
            minApproval = null
        )

        return listOf(track)
    }

    override suspend fun getTrackQueues(trackIds: Set<TrackId>, chainId: ChainId): Map<TrackId, TrackQueue> {
        // we do not support `in queue` status for gov v1 yet
        return emptyMap()
    }

    override suspend fun getAllOnChainReferenda(chainId: ChainId): Collection<OnChainReferendum> {
        return remoteStorageSource.query(chainId) {
            val votingPeriod = runtime.votingPeriod()

            runtime.metadata.democracy().storage("ReferendumInfoOf").entries(
                keyExtractor = { (id: BigInteger) -> ReferendumId(id) },
                binding = { decoded, id -> bindReferendum(decoded, id, votingPeriod, runtime) }
            )
        }.values.filterNotNull()
    }

    override suspend fun getOnChainReferenda(chainId: ChainId, referendaIds: Collection<ReferendumId>): Map<ReferendumId, OnChainReferendum> {
        return remoteStorageSource.query(chainId) {
            val votingPeriod = runtime.votingPeriod()

            runtime.metadata.democracy().storage("ReferendumInfoOf").entries(
                keysArguments = referendaIds.map { listOf(it.value) },
                keyExtractor = { (id: BigInteger) -> ReferendumId(id) },
                binding = { decoded, id -> bindReferendum(decoded, id, votingPeriod, runtime) }
            )
        }.filterNotNull()
    }

    override suspend fun onChainReferendumFlow(chainId: ChainId, referendumId: ReferendumId): Flow<OnChainReferendum?> {
        return remoteStorageSource.subscribe(chainId) {
            val votingPeriod = runtime.votingPeriod()

            runtime.metadata.democracy().storage("ReferendumInfoOf").observe(
                referendumId.value,
                binding = { bindReferendum(it, referendumId, votingPeriod, runtime) }
            )
        }
    }

    override suspend fun getReferendaExecutionBlocks(
        chainId: ChainId,
        approvedReferendaIds: Collection<ReferendumId>
    ): Map<ReferendumId, BlockNumber> {
        if (approvedReferendaIds.isEmpty()) return emptyMap()

        return remoteStorageSource.query(chainId) {
            val schedulerVersion = runtime.metadata.schedulerVersion()

            val referendaIdBySchedulerId = approvedReferendaIds.flatMap { referendumId ->
                referendumId.versionedEnactmentSchedulerIdVariants(runtime, schedulerVersion).map { enactmentKeyVariant ->
                    enactmentKeyVariant.intoKey() to referendumId
                }
            }.toMap()

            // We do not extract referendumId as a key here since we request multiple keys per each referendumId (pre- and post- v4 migration)
            // Thus, we should firstly filter out null values from map.
            // Otherwise key candidate that resulted in null value may shadow another which resulted in value and we will loose that value
            val schedulerKeysBySchedulerKey = runtime.metadata.scheduler().storage("Lookup").entries(
                keysArguments = referendaIdBySchedulerId.keys.map { schedulerIdKey -> listOf(schedulerIdKey.value) },
                keyExtractor = { (schedulerId: ByteArray) -> schedulerId.intoKey() },
                binding = { decoded, _ ->
                    decoded?.let {
                        val (blockNumber, _) = decoded.castToList()

                        bindBlockNumber(blockNumber)
                    }
                }
            ).filterNotNull()

            schedulerKeysBySchedulerKey.mapKeys { (schedulerKey, _) -> referendaIdBySchedulerId.getValue(schedulerKey) }
        }
    }

    private fun bindReferendum(
        decoded: Any?,
        id: ReferendumId,
        votingPeriod: BlockNumber,
        runtime: RuntimeSnapshot,
    ): OnChainReferendum? = runCatching {
        val asDictEnum = decoded.castToDictEnum()

        val referendumStatus = when (asDictEnum.name) {
            "Ongoing" -> {
                val status = asDictEnum.value.castToStruct()
                val end = bindBlockNumber(status["end"])
                val submittedIn = end - votingPeriod

                val threshold = bindThreshold(status["threshold"])

                OnChainReferendumStatus.Ongoing(
                    track = DemocracyTrackId,
                    proposal = bindProposal(status["proposalHash"] ?: status["proposal"], runtime),
                    submitted = submittedIn,
                    submissionDeposit = null,
                    decisionDeposit = null,
                    deciding = DecidingStatus(
                        since = submittedIn,
                        confirming = ConfirmingSource.FromThreshold(end = end)
                    ),
                    tally = bindTally(status.getTyped("tally")),
                    inQueue = false,
                    threshold = bindThreshold(status["threshold"]),
                    delayedPassing = Gov1DelayedThresholdPassing(threshold)
                )
            }

            "Finished" -> {
                val status = asDictEnum.value.castToStruct()
                val approved = bindBoolean(status["approved"])
                val end = bindBlockNumber(status["end"])

                if (approved) {
                    OnChainReferendumStatus.Approved(end)
                } else {
                    OnChainReferendumStatus.Rejected(end)
                }
            }

            else -> throw IllegalArgumentException("Unsupported referendum status")
        }

        OnChainReferendum(
            id = id,
            status = referendumStatus
        )
    }
        .onFailure { Log.e(this.LOG_TAG, "Failed to decode on-chain referendum", it) }
        .getOrNull()

    private fun bindThreshold(decoded: Any?) = bindCollectionEnum(decoded) { name ->
        when (name) {
            "SimpleMajority" -> Gov1VotingThreshold.SIMPLE_MAJORITY
            "SuperMajorityApprove" -> Gov1VotingThreshold.SUPER_MAJORITY_APPROVE
            "SuperMajorityAgainst" -> Gov1VotingThreshold.SUPER_MAJORITY_AGAINST
            else -> incompatible()
        }
    }

    private fun RuntimeSnapshot.votingPeriod(): BlockNumber {
        return metadata.democracy().numberConstant("VotingPeriod", this)
    }

    private fun ReferendumId.versionedEnactmentSchedulerIdVariants(runtime: RuntimeSnapshot, schedulerVersion: SchedulerVersion): List<ByteArray> {
        return when (schedulerVersion) {
            SchedulerVersion.V3 -> listOf(v3EnactmentSchedulerId(runtime))
            SchedulerVersion.V4 -> listOf(v4EnactmentSchedulerId(runtime), v4MigratedEnactmentSchedulerId(runtime))
        }
    }

    // https:github.com/paritytech/substrate/blob/0d64ba4268106fffe430d41b541c1aeedd4f8da5/frame/democracy/src/lib.rs#L1476
    private fun ReferendumId.v3EnactmentSchedulerId(runtime: RuntimeSnapshot): ByteArray {
        val encodedAssemblyId = DEMOCRACY_ID.encodeToByteArray() // 'const bytes' in rust
        val encodedIndex = u32.toByteArray(runtime, value)

        return encodedAssemblyId + encodedIndex
    }

    private fun ReferendumId.v4EnactmentSchedulerId(runtime: RuntimeSnapshot): ByteArray {
        val oldId = v3EnactmentSchedulerId(runtime)

        return if (oldId.size > SCHEDULER_KEY_BOUND) {
            oldId.blake2b256().pad(SCHEDULER_KEY_BOUND, padding = 0)
        } else {
            oldId.padEnd(SCHEDULER_KEY_BOUND, padding = 0)
        }
    }

    private fun ReferendumId.v4MigratedEnactmentSchedulerId(runtime: RuntimeSnapshot): ByteArray {
        return v3EnactmentSchedulerId(runtime).blake2b256()
    }

    private fun RuntimeMetadata.schedulerVersion(): SchedulerVersion {
        val lookupStorage = scheduler().storage("Lookup")
        val lookupArgumentType = lookupStorage.keys.first()

        return if (lookupArgumentType is FixedByteArray) {
            // bounded type is used
            SchedulerVersion.V4
        } else {
            SchedulerVersion.V3
        }
    }
}
