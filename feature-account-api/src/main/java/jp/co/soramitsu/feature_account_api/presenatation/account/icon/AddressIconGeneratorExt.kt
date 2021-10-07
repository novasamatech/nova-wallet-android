package jp.co.soramitsu.feature_account_api.presenatation.account.icon

import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.address.AddressModel
import jp.co.soramitsu.runtime.ext.accountIdOf
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain

suspend fun AddressIconGenerator.createAddressModel(chain: Chain, address: String, sizeInDp: Int, accountName: String? = null): AddressModel {
    val icon = createAddressIcon(chain.accountIdOf(address), sizeInDp)

    return AddressModel(address, icon, accountName)
}
