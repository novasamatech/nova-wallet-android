package io.novafoundation.nova.feature_swap_api.domain.model

import io.novasama.substrate_sdk_android.extensions.fromHex

object HydrationSystemAccounts {

    // Hydration Router pallet's substrate-derived account ("modlrouterex").
    // The router temporarily holds assets during swap execution; transfers
    // user <-> router surface as separate entries in the indexed history
    // and are filtered out so the user only sees the swap itself.
    const val ROUTER_ACCOUNT_HEX = "6d6f646c726f7574657265780000000000000000000000000000000000000000"

    val routerAccountId: ByteArray by lazy {
        ROUTER_ACCOUNT_HEX.fromHex()
    }
}
