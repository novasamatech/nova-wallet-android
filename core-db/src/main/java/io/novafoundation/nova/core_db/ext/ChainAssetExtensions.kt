package io.novafoundation.nova.core_db.ext

import io.novafoundation.nova.core_db.dao.FullAssetIdLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal

fun ChainAssetLocal.isSameAsset(other: ChainAssetLocal): Boolean {
    return this.chainId == other.chainId && this.id == other.id
}

fun ChainAssetLocal.fullId(): FullAssetIdLocal {
    return FullAssetIdLocal(this.chainId, this.id)
}
