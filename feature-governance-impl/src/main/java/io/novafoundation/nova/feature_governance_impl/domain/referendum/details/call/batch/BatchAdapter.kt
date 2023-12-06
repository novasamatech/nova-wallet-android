package io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.batch

import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumCallAdapter
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumCallParseContext
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

class BatchAdapter : ReferendumCallAdapter {

    override suspend fun fromCall(call: GenericCall.Instance, context: ReferendumCallParseContext): ReferendumCall? {
        if (!call.instanceOf(Modules.UTILITY, "batch", "batch_all", "force_batch")) return null

        val innerCalls = call.arguments["calls"].cast<List<GenericCall.Instance>>()

        return innerCalls.mapNotNull { context.parse(it) }
            .reduceOrNull { acc, referendumCall -> acc.combineWith(referendumCall) }
    }
}
