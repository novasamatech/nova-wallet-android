package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindFixedI64
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindPerbill
import io.novafoundation.nova.common.data.network.runtime.binding.bindString
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.decodedValue
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.referenda
import io.novafoundation.nova.common.utils.scheduler
import io.novafoundation.nova.common.utils.toByteArray
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ConfirmingSource
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ConfirmingStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.DecidingStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumDeposit
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackQueue
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.empty
import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.thresold.gov2.Gov2VotingThreshold
import io.novafoundation.nova.feature_governance_api.data.thresold.gov2.curve.LinearDecreasingCurve
import io.novafoundation.nova.feature_governance_api.data.thresold.gov2.curve.ReciprocalCurve
import io.novafoundation.nova.feature_governance_api.data.thresold.gov2.curve.SteppedDecreasingCurve
import io.novafoundation.nova.feature_governance_impl.data.repository.common.bindProposal
import io.novafoundation.nova.feature_governance_impl.data.repository.common.bindTally
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import io.novafoundation.nova.runtime.repository.getActiveIssuance
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.u32
import io.novasama.substrate_sdk_android.runtime.definitions.types.toByteArray
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.scale.dataType.string
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

private const val ASSEMBLY_ID = "assembly"

class GovV2OnChainReferendaRepository(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val totalIssuanceRepository: TotalIssuanceRepository,
) : OnChainReferendaRepository {

    override suspend fun electorate(chainId: ChainId): Balance {
        return totalIssuanceRepository.getActiveIssuance(chainId)
    }

    override suspend fun undecidingTimeout(chainId: ChainId): BlockNumber {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.referenda().numberConstant("UndecidingTimeout", runtime)
    }

    override suspend fun getTracks(chainId: ChainId): Collection<TrackInfo> {
        val runtime = chainRegistry.getRuntime(chainId)
        val tracksConstant = runtime.metadata.referenda().constant("Tracks").decodedValue(runtime)

        return bindTracks(tracksConstant)
    }

    override suspend fun getTrackQueues(trackIds: Set<TrackId>, chainId: ChainId): Map<TrackId, TrackQueue> {
        if (trackIds.isEmpty()) return emptyMap()
        return remoteStorageSource.query(chainId) {
            runtime.metadata.referenda().storage("TrackQueue").entries(
                keysArguments = trackIds.map { listOf(it.value) },
                keyExtractor = { (trackIdRaw: BigInteger) -> TrackId(trackIdRaw) },
                binding = { decoded, _ -> bindTrackQueue(decoded) }
            )
        }
    }

    override suspend fun getAllOnChainReferenda(chainId: ChainId): Collection<OnChainReferendum> {
        return remoteStorageSource.query(chainId) {
            val allTracks = getTracksById(chainId)

            runtime.metadata.referenda().storage("ReferendumInfoFor").entries(
                prefixArgs = emptyArray(),
                keyExtractor = { (id: BigInteger) -> ReferendumId(id) },
                binding = { decoded, id -> bindReferendum(decoded, id, allTracks, runtime) }
            ).values.filterNotNull()
        }
    }

    override suspend fun getOnChainReferenda(chainId: ChainId, referendaIds: Collection<ReferendumId>): Map<ReferendumId, OnChainReferendum> {
        return remoteStorageSource.query(chainId) {
            val allTracks = getTracksById(chainId)

            runtime.metadata.referenda().storage("ReferendumInfoFor").entries(
                keysArguments = referendaIds.map { id -> listOf(id.value) },
                keyExtractor = { (id: BigInteger) -> ReferendumId(id) },
                binding = { decoded, id -> bindReferendum(decoded, id, allTracks, runtime) }
            )
        }.filterNotNull()
    }

    override suspend fun onChainReferendumFlow(chainId: ChainId, referendumId: ReferendumId): Flow<OnChainReferendum?> {
        return remoteStorageSource.subscribe(chainId) {
            val allTracks = getTracksById(chainId)

            runtime.metadata.referenda().storage("ReferendumInfoFor").observe(
                referendumId.value,
                binding = { bindReferendum(it, referendumId, allTracks, runtime) }
            )
        }
    }

    override suspend fun getReferendaExecutionBlocks(
        chainId: ChainId,
        approvedReferendaIds: Collection<ReferendumId>
    ): Map<ReferendumId, BlockNumber> {
        if (approvedReferendaIds.isEmpty()) return emptyMap()

        return remoteStorageSource.query(chainId) {
            val referendaIdBySchedulerId = approvedReferendaIds.associateBy { it.enactmentSchedulerId(runtime).toHexString() }

            runtime.metadata.scheduler().storage("Lookup").entries(
                keysArguments = referendaIdBySchedulerId.keys.map { schedulerIdHex -> listOf(schedulerIdHex.fromHex()) },
                keyExtractor = { (schedulerId: ByteArray) -> referendaIdBySchedulerId.getValue(schedulerId.toHexString()) },
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
        tracksById: Map<TrackId, TrackInfo>,
        runtime: RuntimeSnapshot
    ): OnChainReferendum? = runCatching {
        val asDictEnum = decoded.castToDictEnum()

        val referendumStatus = when (asDictEnum.name) {
            "Ongoing" -> {
                val status = asDictEnum.value.castToStruct()
                val trackId = TrackId(bindNumber(status["track"]))
                val track = tracksById.getValue(trackId)

                OnChainReferendumStatus.Ongoing(
                    track = trackId,
                    proposal = bindProposal(status["proposal"], runtime),
                    submitted = bindBlockNumber(status["submitted"]),
                    submissionDeposit = bindReferendumDeposit(status["submissionDeposit"])!!,
                    decisionDeposit = bindReferendumDeposit(status["decisionDeposit"]),
                    deciding = bindDecidingStatus(status["deciding"]),
                    tally = bindTally(status.getTyped("tally")),
                    inQueue = bindBoolean(status["inQueue"]),
                    threshold = Gov2VotingThreshold(track),
                )
            }

            "Approved" -> OnChainReferendumStatus.Approved(bindCompletedReferendumSince(asDictEnum.value))
            "Rejected" -> OnChainReferendumStatus.Rejected(bindCompletedReferendumSince(asDictEnum.value))
            "Cancelled" -> OnChainReferendumStatus.Cancelled(bindCompletedReferendumSince(asDictEnum.value))
            "TimedOut" -> OnChainReferendumStatus.TimedOut(bindCompletedReferendumSince(asDictEnum.value))
            "Killed" -> OnChainReferendumStatus.Killed(bindCompletedReferendumSince(asDictEnum.value))
            else -> throw IllegalArgumentException("Unsupported referendum status")
        }

        OnChainReferendum(
            id = id,
            status = referendumStatus
        )
    }
        .onFailure { Log.e(this.LOG_TAG, "Failed to decode on-chain referendum", it) }
        .getOrNull()

    private fun bindDecidingStatus(decoded: Any?): DecidingStatus? {
        if (decoded == null) return null
        val decodedStruct = decoded.castToStruct()

        val confirming = decodedStruct.get<Any?>("confirming")?.let {
            ConfirmingStatus(
                till = bindBlockNumber(it)
            )
        }
        return DecidingStatus(
            since = bindBlockNumber(decodedStruct["since"]),
            confirming = ConfirmingSource.OnChain(confirming)
        )
    }

    private fun bindCompletedReferendumSince(decoded: Any?): BlockNumber {
        // first element in tuple
        val since = decoded.castToList().first()

        return bindNumber(since)
    }

    private fun bindReferendumDeposit(decoded: Struct.Instance?): ReferendumDeposit? {
        return decoded?.let {
            ReferendumDeposit(
                who = bindAccountId(it["who"]),
                amount = bindNumber(it["amount"])
            )
        }
    }

    private fun bindTracks(decoded: Any?): List<TrackInfo> {
        return bindList(decoded) {
            val (id, content) = it.castToList()
            val trackInfoStruct = content.castToStruct()

            TrackInfo(
                id = TrackId(bindNumber(id)),
                name = bindString(trackInfoStruct["name"]),
                preparePeriod = bindBlockNumber(trackInfoStruct["preparePeriod"]),
                decisionPeriod = bindBlockNumber(trackInfoStruct["decisionPeriod"]),
                minApproval = bindCurve(trackInfoStruct.getTyped("minApproval")),
                minSupport = bindCurve(trackInfoStruct.getTyped("minSupport"))
            )
        }
    }

    private fun bindCurve(decoded: DictEnum.Entry<*>): VotingCurve {
        val valueStruct = decoded.value.castToStruct()

        return when (decoded.name) {
            "Reciprocal" -> {
                ReciprocalCurve(
                    factor = bindFixedI64(valueStruct["factor"]),
                    xOffset = bindFixedI64(valueStruct["x_offset"]),
                    yOffset = bindFixedI64(valueStruct["y_offset"])
                )
            }

            "LinearDecreasing" -> {
                LinearDecreasingCurve(
                    length = bindPerbill(valueStruct["length"]),
                    floor = bindPerbill(valueStruct["floor"]),
                    ceil = bindPerbill(valueStruct["ceil"])
                )
            }

            "SteppedDecreasing" -> {
                SteppedDecreasingCurve(
                    begin = bindPerbill(valueStruct["begin"]),
                    end = bindPerbill(valueStruct["end"]),
                    step = bindPerbill(valueStruct["step"]),
                    period = bindPerbill(valueStruct["period"])
                )
            }

            else -> incompatible()
        }
    }

    private fun bindTrackQueue(decoded: Any?): TrackQueue {
        if (decoded == null) return TrackQueue.empty()

        val referendumIds = bindList(decoded) {
            val (referendumIndex, _) = it.castToList()

            ReferendumId(bindNumber(referendumIndex))
        }

        return TrackQueue(referendumIds)
    }

    // https://github.com/paritytech/substrate/blob/fc67cbb66d8c484bc7b7506fc1300344d12ecbad/frame/referenda/src/lib.rs#L716
    private fun ReferendumId.enactmentSchedulerId(runtime: RuntimeSnapshot): ByteArray {
        val encodedAssemblyId = ASSEMBLY_ID.encodeToByteArray() // 'const bytes' in rust
        val encodedEnactment = string.toByteArray("enactment") // 'string' in rust
        val encodedIndex = u32.toByteArray(runtime, value)

        val toHash = encodedAssemblyId + encodedEnactment + encodedIndex

        return toHash.blake2b256()
    }
}
