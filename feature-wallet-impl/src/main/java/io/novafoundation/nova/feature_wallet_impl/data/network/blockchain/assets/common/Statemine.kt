package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.common

import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine.AssetAccount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine.bindAssetAccount
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.module

fun RuntimeMetadata.statemineModule(statemineType: Chain.Asset.Type.Statemine) = module(statemineType.palletNameOrDefault())

fun bindAssetAccountOrEmpty(decoded: Any?): AssetAccount {
    return decoded?.let(::bindAssetAccount) ?: AssetAccount.empty()
}
