package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindByteArray
import io.novafoundation.nova.common.data.network.runtime.binding.bindDispatchTime
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
import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.decodedValue
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.referenda
import io.novafoundation.nova.common.utils.scheduler
import io.novafoundation.nova.common.utils.toByteArray
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ConfirmingStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.DecidingStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.DecisionDeposit
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve
import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository
import io.novafoundation.nova.feature_governance_impl.data.model.curve.LinearDecreasingCurve
import io.novafoundation.nova.feature_governance_impl.data.model.curve.ReciprocalCurve
import io.novafoundation.nova.feature_governance_impl.data.model.curve.SteppedDecreasingCurve
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.primitives.u32
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.toByteArray
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.scale.dataType.string
import java.math.BigInteger

private const val ASSEMBLY_ID = "assembly"

class GovV2OnChainReferendaRepository(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : OnChainReferendaRepository {

    override suspend fun undecidingTimeout(chainId: ChainId): BlockNumber {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.referenda().numberConstant("UndecidingTimeout", runtime)
    }

    override suspend fun getTracks(chainId: ChainId): Collection<TrackInfo> {
        val runtime = chainRegistry.getRuntime(chainId)
        val tracksConstant = runtime.metadata.referenda().constant("Tracks").decodedValue(runtime)

        return bindTracks(tracksConstant)
    }

    override suspend fun getOnChainReferenda(chainId: ChainId): Collection<OnChainReferendum> {
        return remoteStorageSource.query(chainId) {
            runtime.metadata.referenda().storage("ReferendumInfoFor").entries(
                prefixArgs = emptyArray(),
                keyExtractor = { (id: BigInteger) -> ReferendumId(id) },
                binding = ::bindReferendum
            ).values.filterNotNull()
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

    private fun bindReferendum(decoded: Any?, id: ReferendumId): OnChainReferendum? = runCatching {
        val asDictEnum = decoded.castToDictEnum()

        val referendumStatus = when (asDictEnum.name) {
            "Ongoing" -> {
                val status = asDictEnum.value.castToStruct()

                OnChainReferendumStatus.Ongoing(
                    track = TrackId(bindNumber(status["track"])),
                    proposalHash = bindByteArray(status["proposalHash"]),
                    desiredEnactment = bindDispatchTime(status.getTyped("enactment")),
                    submitted = bindBlockNumber(status["submitted"]),
                    decisionDeposit = bindDecisionDeposit(status["decisionDeposit"]),
                    deciding = bindDecidingStatus(status.getTyped("deciding")),
                    tally = bindTally(status.getTyped("tally")),
                    inQueue = bindBoolean(status["inQueue"])
                )
            }
            "Approved" -> OnChainReferendumStatus.Approved
            "Rejected" -> OnChainReferendumStatus.Rejected
            "Cancelled" -> OnChainReferendumStatus.Cancelled
            "TimedOut" -> OnChainReferendumStatus.TimedOut
            "Killed" -> OnChainReferendumStatus.Killed
            else -> throw IllegalArgumentException("Unsupported referendum status")
        }

        OnChainReferendum(
            id = id,
            status = referendumStatus
        )
    }.getOrNull()

    private fun bindDecidingStatus(decoded: Struct.Instance): DecidingStatus {
        return DecidingStatus(
            since = bindBlockNumber(decoded["since"]),
            confirming = decoded.get<Any?>("confirming")?.let {
                ConfirmingStatus(
                    till = bindBlockNumber(it)
                )
            }
        )
    }

    private fun bindTally(decoded: Struct.Instance): Tally {
        return Tally(
            ayes = bindNumber(decoded["ayes"]),
            nays = bindNumber(decoded["nays"]),
            support = bindNumber(decoded["support"])
        )
    }

    private fun bindDecisionDeposit(decoded: Struct.Instance?): DecisionDeposit? {
        return decoded?.let {
            DecisionDeposit(
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
                maxDeciding = bindNumber(trackInfoStruct["maxDeciding"]),
                decisionDeposit = bindNumber(trackInfoStruct["decisionDeposit"]),
                preparePeriod = bindBlockNumber(trackInfoStruct["preparePeriod"]),
                decisionPeriod = bindBlockNumber(trackInfoStruct["decisionPeriod"]),
                confirmPeriod = bindBlockNumber(trackInfoStruct["confirmPeriod"]),
                minEnactmentPeriod = bindBlockNumber(trackInfoStruct["minEnactmentPeriod"]),
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

    // https://github.com/paritytech/substrate/blob/fc67cbb66d8c484bc7b7506fc1300344d12ecbad/frame/referenda/src/lib.rs#L716
    private fun ReferendumId.enactmentSchedulerId(runtime: RuntimeSnapshot): ByteArray {
        val encodedAssemblyId = ASSEMBLY_ID.encodeToByteArray() // 'const bytes' in rust
        val encodedEnactment = string.toByteArray("enactment") // 'string' in rust
        val encodedIndex = u32.toByteArray(runtime, value)

        val toHash = encodedAssemblyId + encodedEnactment + encodedIndex

        return toHash
        // TODO use hashed version once testnet will be updated to latest pallet-referenda version
//        return toHash.blake2b256()
    }
}
