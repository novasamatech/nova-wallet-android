package io.novafoundation.nova.feature_account_api.data.mappers

import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun mapChainToUi(chain: Chain): ChainUi = with(chain) {
    ChainUi(
        id = id,
        name = name,
        icon = icon,
    )
}
