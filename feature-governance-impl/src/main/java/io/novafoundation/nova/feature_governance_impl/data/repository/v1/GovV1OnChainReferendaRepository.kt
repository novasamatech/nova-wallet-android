package io.novafoundation.nova.feature_governance_impl.data.repository.v1

import android.util.Log
import io.novafoundation.nova.common.address.getValue
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindByteArray
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
import io.novafoundation.nova.common.utils.scheduler
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ConfirmingSource
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.DecidingStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Proposal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackQueue
import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository
import io.novafoundation.nova.feature_governance_api.data.thresold.gov1.Gov1VotingThreshold
import io.novafoundation.nova.feature_governance_impl.data.repository.common.bindTally
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.primitives.u32
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.toByteArray
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

class GovV1OnChainReferendaRepository(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : OnChainReferendaRepository {

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
                binding = { decoded, id -> bindReferendum(decoded, id, votingPeriod) }
            )
        }.values.filterNotNull()
    }

    override suspend fun getOnChainReferenda(chainId: ChainId, referendaIds: Collection<ReferendumId>): Map<ReferendumId, OnChainReferendum> {
        return remoteStorageSource.query(chainId) {
            val votingPeriod = runtime.votingPeriod()

            runtime.metadata.democracy().storage("ReferendumInfoOf").entries(
                keysArguments = referendaIds.map { listOf(it.value) },
                keyExtractor = { (id: BigInteger) -> ReferendumId(id) },
                binding = { decoded, id -> bindReferendum(decoded, id, votingPeriod) }
            )
        }.filterNotNull()
    }

    override suspend fun onChainReferendumFlow(chainId: ChainId, referendumId: ReferendumId): Flow<OnChainReferendum> {
        return remoteStorageSource.subscribe(chainId) {
            val votingPeriod = runtime.votingPeriod()

            runtime.metadata.democracy().storage("ReferendumInfoOf").observe(
                referendumId.value,
                binding = { bindReferendum(it, referendumId, votingPeriod)!! }
            )
        }
    }

    override suspend fun getReferendaExecutionBlocks(
        chainId: ChainId,
        approvedReferendaIds: Collection<ReferendumId>
    ): Map<ReferendumId, BlockNumber> {
        if (approvedReferendaIds.isEmpty()) return emptyMap()

        return remoteStorageSource.query(chainId) {
            val referendaIdBySchedulerId = approvedReferendaIds.associateBy { it.enactmentSchedulerId(runtime).intoKey() }

            runtime.metadata.scheduler().storage("Lookup").entries(
                keysArguments = referendaIdBySchedulerId.keys.map { schedulerIdKey -> listOf(schedulerIdKey.value) },
                keyExtractor = { (schedulerId: ByteArray) -> referendaIdBySchedulerId.getValue(schedulerId) },
                binding = { decoded, _ ->
                    decoded?.let {
                        val (blockNumber, _) = decoded.castToList()

                        bindBlockNumber(blockNumber)
                    }
                }
            ).filterNotNull()
        }
    }

    private fun bindReferendum(
        decoded: Any?,
        id: ReferendumId,
        votingPeriod: BlockNumber,
    ): OnChainReferendum? = runCatching {
        val asDictEnum = decoded.castToDictEnum()

        val referendumStatus = when (asDictEnum.name) {
            "Ongoing" -> {
                val status = asDictEnum.value.castToStruct()
                val end = bindBlockNumber(status["end"])
                val submittedIn = end - votingPeriod

                OnChainReferendumStatus.Ongoing(
                    track = DemocracyTrackId,
                    proposal = bindProposal(status["proposalHash"]),
                    submitted = submittedIn,
                    submissionDeposit = null,
                    decisionDeposit = null,
                    deciding = DecidingStatus(
                        since = submittedIn,
                        confirming = ConfirmingSource.FromThreshold(end = end)
                    ),
                    tally = bindTally(status.getTyped("tally")),
                    inQueue = false,
                    threshold = bindThreshold(status["threshold"])
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

    private fun bindProposal(decoded: Any?): Proposal {
        return Proposal.Legacy(bindByteArray(decoded))
    }

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

    // https:github.com/paritytech/substrate/blob/0d64ba4268106fffe430d41b541c1aeedd4f8da5/frame/democracy/src/lib.rs#L1476
    private fun ReferendumId.enactmentSchedulerId(runtime: RuntimeSnapshot): ByteArray {
        val encodedAssemblyId = DEMOCRACY_ID.encodeToByteArray() // 'const bytes' in rust
        val encodedIndex = u32.toByteArray(runtime, value)

        return encodedAssemblyId + encodedIndex
    }
}
