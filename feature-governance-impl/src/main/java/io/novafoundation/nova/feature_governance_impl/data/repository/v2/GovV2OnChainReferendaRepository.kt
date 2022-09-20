package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindByteArray
import io.novafoundation.nova.common.data.network.runtime.binding.bindDispatchTime
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.utils.referenda
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ConfirmingStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.DecidingStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.DecisionDeposit
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import java.math.BigInteger

class GovV2OnChainReferendaRepository(
    private val remoteStorageSource: StorageDataSource,
) : OnChainReferendaRepository {

    override suspend fun getOnChainReferenda(chainId: ChainId): Collection<OnChainReferendum> {
        return remoteStorageSource.query(chainId) {
            runtime.metadata.referenda().storage("ReferendumInfoFor").entries(
                prefixArgs = emptyArray(),
                keyExtractor = { (id: BigInteger) -> ReferendumId(id) },
                binding = ::bindReferendum
            ).values.filterNotNull()
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
}
