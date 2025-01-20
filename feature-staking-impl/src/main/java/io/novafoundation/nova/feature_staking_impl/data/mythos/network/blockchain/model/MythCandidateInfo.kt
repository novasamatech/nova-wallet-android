package io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindInt
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_account_api.data.model.AccountIdKeyMap
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class MythCandidateInfo(
    val stake: Balance,
    val stakers: Int
)

typealias MythCandidateInfos = AccountIdKeyMap<MythCandidateInfo>

fun bindMythCandidateInfo(decoded: Any?): MythCandidateInfo {
    val asStruct = decoded.castToStruct()
    return MythCandidateInfo(
        stake = bindNumber(asStruct["stake"]),
        stakers = bindInt(asStruct["stakers"])
    )
}
