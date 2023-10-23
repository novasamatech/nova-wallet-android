package io.novafoundation.nova.feature_nft_impl.data.network.blockchain

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

fun ExtrinsicBuilder.statemineNftTransfer(
    nftType: Nft.Type.Uniques,
    target: AccountId
) {
    call(
        moduleName = Modules.UNIQUES,
        callName = "transfer",
        arguments = mapOf(
            "collection" to nftType.collectionId,
            "item" to nftType.instanceId,
            "dest" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, target)
        )
    )
}

