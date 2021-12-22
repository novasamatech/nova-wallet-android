package io.novafoundation.nova.feature_account_api.presenatation.account.icon

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.invoke
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

suspend fun AddressIconGenerator.createAddressModel(chain: Chain, address: String, sizeInDp: Int, accountName: String? = null): AddressModel {
    val icon = createAddressIcon(chain, address, sizeInDp)

    return AddressModel(address, icon, accountName)
}

suspend fun AddressIconGenerator.createAddressModel(
    chain: Chain,
    address: String,
    sizeInDp: Int,
    addressDisplayUseCase: AddressDisplayUseCase,
): AddressModel {
    val icon = createAddressIcon(chain, address, sizeInDp)

    return AddressModel(address, icon, addressDisplayUseCase(chain, address))
}

suspend fun AddressIconGenerator.createAddressIcon(chain: Chain, address: String, sizeInDp: Int): Drawable {
    return createAddressIcon(chain.accountIdOf(address), sizeInDp)
}
