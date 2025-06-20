package io.novafoundation.nova.runtime.extrinsic.visitor.call.impl.nodes.batch

import io.novafoundation.nova.common.utils.Modules
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

internal class BatchCallNode : BaseBatchNode() {

    override fun canVisit(call: GenericCall.Instance): Boolean {
        return call.module.name == Modules.UTILITY && call.function.name == "batch"
    }
}
