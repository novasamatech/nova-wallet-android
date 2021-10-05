package jp.co.soramitsu.feature_account_api.data.mappers

import jp.co.soramitsu.feature_account_api.presenatation.chain.ChainUi
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain

fun mapChainToUi(chain: Chain): ChainUi = with(chain) {
    ChainUi(
        id = id,
        name = name,
        icon = icon
    )
}
