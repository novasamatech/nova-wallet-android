package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import javax.inject.Inject

@FeatureScope
class DynamicCrossChainWeigher @Inject constructor() {

    suspend fun estimateFee(amount: Balance, config: DynamicCrossChainTransferConfiguration): CrossChainFeeModel {
        // TODO
        return CrossChainFeeModel()
    }
}
