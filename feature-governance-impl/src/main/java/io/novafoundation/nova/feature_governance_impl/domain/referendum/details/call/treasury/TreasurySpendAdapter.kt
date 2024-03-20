package io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.treasury

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNonce
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumCallAdapter
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumCallParseContext
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

class TreasurySpendAdapter : ReferendumCallAdapter {

    override suspend fun fromCall(
        call: GenericCall.Instance,
        context: ReferendumCallParseContext
    ): ReferendumCall? {
        // TODO: spend call is using MultiLocation now instead of MultiAddress so binding throws an exception
        if (!call.instanceOf(Modules.TREASURY, "spend_local", "spend")) return null

        val amount = bindNonce(call.arguments["amount"])
        val beneficiary = bindAccountIdentifier(call.arguments["beneficiary"])

        return ReferendumCall.TreasuryRequest(amount, beneficiary)
    }
}
