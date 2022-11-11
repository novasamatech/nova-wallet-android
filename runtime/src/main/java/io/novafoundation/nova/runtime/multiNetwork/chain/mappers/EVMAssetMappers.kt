package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import io.novafoundation.nova.common.utils.ethereumAddressToAccountId
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.runtime.multiNetwork.asset.remote.model.EVMAssetRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun mapEVMAssetRemoteToLocalAssets(evmAssetRemote: EVMAssetRemote): List<ChainAssetLocal> {
    return evmAssetRemote.instances.map {
        val assetId = it.contractAddress.ethereumAddressToAccountId()
            .contentHashCode()
        ChainAssetLocal(
            id = assetId,
            chainId = it.chainId,
            symbol = evmAssetRemote.symbol,
            name = evmAssetRemote.name,
            precision = evmAssetRemote.precision,
            priceId = evmAssetRemote.priceId,
            icon = evmAssetRemote.icon,
            staking = Chain.Asset.StakingType.UNSUPPORTED.toString(),
            source = AssetSourceLocal.ERC20,
            type = null,
            buyProviders = null,
            typeExtras = null
        )
    }
}
