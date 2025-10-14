package io.novafoundation.nova.feature_xcm_impl.config.api.response

import io.novafoundation.nova.feature_xcm_api.config.remote.JunctionsRemote
import java.math.BigInteger

class GeneralXcmConfigRemote(
    val chains: ChainXcmConfigRemote,
    val assets: AssetsXcmConfigRemote
)

class ChainXcmConfigRemote(
    val parachainIds: Map<String, BigInteger>
)

class AssetsXcmConfigRemote(
    val assetsLocation: Map<String, ChainAssetReserveConfigRemote>?,

    // By default, asset reserve id is equal to its symbol
    // This mapping allows to override that for cases like multiple reserves (Statemine & Polkadot for DOT)
    val reserveIdOverrides: Map<String, Map<Int, String>>?,
)

class ChainAssetReserveConfigRemote(
    val chainId: String,
    val assetId: Int,
    val multiLocation: JunctionsRemote,
)
