package io.novafoundation.nova.feature_wallet_api.domain.model.xcm

import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransfersConfiguration

class CrossChainTransfersConfiguration(
    val dynamic: DynamicCrossChainTransfersConfiguration,
    val legacy: LegacyCrossChainTransfersConfiguration
)
