package io.novafoundation.nova.feature_governance_impl.data.repository.common

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct

fun bindTally(decoded: Struct.Instance): Tally {
    return Tally(
        ayes = bindNumber(decoded["ayes"]),
        nays = bindNumber(decoded["nays"]),
        support = bindNumber(decoded["support"] ?: decoded["turnout"])
    )
}
