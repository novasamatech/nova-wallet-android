package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources

import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.TradeAmountLimitedEdge

interface HydraDxQuotableEdge : QuotableEdge, TradeAmountLimitedEdge
