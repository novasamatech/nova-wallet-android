package io.novafoundation.nova.feature_account_api.presenatation.account.icon

import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.OptionalAddressModel
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.invoke
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

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

suspend fun AddressIconGenerator.createOptionalAddressModel(
    chain: Chain,
    address: String,
    sizeInDp: Int = AddressIconGenerator.SIZE_SMALL,
    addressDisplayUseCase: AddressDisplayUseCase? = null,
    @ColorRes background: Int = AddressIconGenerator.BACKGROUND_DEFAULT,
): OptionalAddressModel {
    val icon = runCatching { createAddressIcon(chain, address, sizeInDp, background) }.getOrNull()

    return OptionalAddressModel(address, icon, addressDisplayUseCase?.invoke(chain, address))
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

suspend fun AddressIconGenerator.createOptionalAccountAddressIcon(
    chain: Chain,
    address: String,
) = kotlin.runCatching {
    createAddressIcon(chain.accountIdOf(address), AddressIconGenerator.SIZE_SMALL, AddressIconGenerator.BACKGROUND_TRANSPARENT)
}.getOrNull()

suspend fun AddressIconGenerator.createAccountAddressModel(
    chain: Chain,
    accountId: ByteArray,
    name: String? = null,
) = createAddressModel(
    chain = chain,
    address = chain.addressOf(accountId),
    sizeInDp = AddressIconGenerator.SIZE_SMALL,
    accountName = name,
    background = AddressIconGenerator.BACKGROUND_TRANSPARENT
)

suspend fun AddressIconGenerator.createAccountAddressModel(
    chain: Chain,
    account: MetaAccount,
    name: String? = account.name
) = createAddressModel(
    chain = chain,
    address = account.addressIn(chain) ?: throw IllegalArgumentException("No address found for ${account.name} in ${chain.name}"),
    sizeInDp = AddressIconGenerator.SIZE_SMALL,
    accountName = name,
    background = AddressIconGenerator.BACKGROUND_TRANSPARENT
)

suspend fun AddressIconGenerator.createAccountAddressModel(
    chain: Chain,
    address: String,
    addressDisplayUseCase: AddressDisplayUseCase,
) = createAccountAddressModel(chain, address, addressDisplayUseCase(chain, address))

suspend fun AddressIconGenerator.createAccountAddressModel(
    chain: Chain,
    accountId: AccountId,
    addressDisplayUseCase: AddressDisplayUseCase,
) = createAccountAddressModel(chain, accountId, addressDisplayUseCase.invoke(accountId, chain.id))

suspend fun AddressIconGenerator.createIdentityAddressModel(
    chain: Chain,
    accountId: ByteArray,
    identityProvider: IdentityProvider
) = createAccountAddressModel(
    chain = chain,
    accountId = accountId,
    name = identityProvider.identityFor(accountId, chain.id)?.name
)
