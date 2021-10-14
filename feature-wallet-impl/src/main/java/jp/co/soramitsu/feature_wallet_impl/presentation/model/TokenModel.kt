package jp.co.soramitsu.feature_wallet_impl.presentation.model

import androidx.annotation.ColorRes
import jp.co.soramitsu.core.model.Node
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

class TokenModel(
    val configuration: Chain.Asset,
    val dollarRate: BigDecimal,
    val recentRateChange: BigDecimal,
    @ColorRes val rateChangeColorRes: Int
)

// TODO - wallet compatibility
val Chain.Asset.networkType
    get() = Node.NetworkType.findByGenesis(chainId)
