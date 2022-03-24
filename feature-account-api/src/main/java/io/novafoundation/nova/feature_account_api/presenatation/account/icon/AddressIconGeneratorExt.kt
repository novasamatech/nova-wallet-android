package io.novafoundation.nova.feature_account_api.presenatation.account.icon

import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.invoke
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

suspend fun AddressIconGenerator.createAddressModel(
    chain: Chain,
    address: String,
    sizeInDp: Int,
    accountName: String? = null,
    background: Int = AddressIconGenerator.BACKGROUND_DEFAULT,
): AddressModel {
    val icon = createAddressIcon(chain, address, sizeInDp, background)

    return AddressModel(address, icon, accountName)
}

suspend fun AddressIconGenerator.createAddressModel(
    chain: Chain,
    address: String,
    sizeInDp: Int,
    addressDisplayUseCase: AddressDisplayUseCase? = null,
    @ColorRes background: Int = AddressIconGenerator.BACKGROUND_DEFAULT,
): AddressModel {
    val icon = createAddressIcon(chain, address, sizeInDp, background)

    return AddressModel(address, icon, addressDisplayUseCase?.invoke(chain, address))
}

suspend fun AddressIconGenerator.createAddressModel(
    chain: Chain,
    accountId: ByteArray,
    sizeInDp: Int,
    addressDisplayUseCase: AddressDisplayUseCase,
    @ColorRes background: Int = AddressIconGenerator.BACKGROUND_DEFAULT,
): AddressModel {
    val icon = createAddressIcon(accountId, sizeInDp, background)
    val address = chain.addressOf(accountId)

    return AddressModel(address, icon, addressDisplayUseCase(chain, address))
}

suspend fun AddressIconGenerator.createAddressIcon(
    chain: Chain,
    address: String,
    sizeInDp: Int,
    @ColorRes background: Int = AddressIconGenerator.BACKGROUND_DEFAULT,
): Drawable {
    return createAddressIcon(chain.accountIdOf(address), sizeInDp, background)
}

suspend fun AddressIconGenerator.createAccountAddressModel(
    chain: Chain,
    address: String,
    name: String? = null,
) = createAddressModel(
    chain = chain,
    address = address,
    sizeInDp = AddressIconGenerator.SIZE_SMALL,
    accountName = name,
    background = AddressIconGenerator.BACKGROUND_TRANSPARENT
)
