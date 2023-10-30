package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.ethereumAddressToAccountId
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.runtime.multiNetwork.asset.remote.model.EVMAssetRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun chainAssetIdOfErc20Token(contractAddress: String): Int {
    return contractAddress.ethereumAddressToAccountId()
        .contentHashCode()
}

fun mapEVMAssetRemoteToLocalAssets(evmAssetRemote: EVMAssetRemote, gson: Gson): List<ChainAssetLocal> {
    return evmAssetRemote.instances.map {
        val assetId = chainAssetIdOfErc20Token(it.contractAddress)

        val domainType = Chain.Asset.Type.EvmErc20(it.contractAddress)
        val (type, typeExtras) = mapChainAssetTypeToRaw(domainType)

        ChainAssetLocal(
            id = assetId,
            chainId = it.chainId,
            symbol = evmAssetRemote.symbol,
            name = evmAssetRemote.name,
            precision = evmAssetRemote.precision,
            priceId = evmAssetRemote.priceId,
            icon = evmAssetRemote.icon,
            staking = mapStakingTypeToLocal(Chain.Asset.StakingType.UNSUPPORTED),
            source = AssetSourceLocal.ERC20,
            type = type,
            buyProviders = gson.toJson(it.buyProviders),
            typeExtras = gson.toJson(typeExtras),
            enabled = true
        )
    }
}
