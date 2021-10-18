package jp.co.soramitsu.feature_account_api.presenatation.account.icon

import android.graphics.drawable.Drawable
import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.address.AddressModel
import jp.co.soramitsu.runtime.ext.accountIdOf
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain

suspend fun AddressIconGenerator.createAddressModel(chain: Chain, address: String, sizeInDp: Int, accountName: String? = null): AddressModel {
    val icon = createAddressIcon(chain, address, sizeInDp)

    return AddressModel(address, icon, accountName)
}

suspend fun AddressIconGenerator.createAddressIcon(chain: Chain, address: String, sizeInDp: Int): Drawable {
    return createAddressIcon(chain.accountIdOf(address), sizeInDp)
}
