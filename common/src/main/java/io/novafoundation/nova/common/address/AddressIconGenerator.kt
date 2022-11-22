package io.novafoundation.nova.common.address

import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.resources.ResourceManager
import jp.co.soramitsu.fearless_utils.exceptions.AddressFormatException
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.icon.IconGenerator
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// TODO ethereum address icon generation
interface AddressIconGenerator {

    companion object {

        const val SIZE_SMALL = 18
        const val SIZE_MEDIUM = 24
        const val SIZE_BIG = 32

        val BACKGROUND_LIGHT = R.color.address_icon_background
        val BACKGROUND_TRANSPARENT = android.R.color.transparent

        val BACKGROUND_DEFAULT = BACKGROUND_LIGHT
    }

    suspend fun createAddressIcon(
        accountId: AccountId,
        sizeInDp: Int,
        @ColorRes backgroundColorRes: Int = BACKGROUND_DEFAULT
    ): Drawable
}

@Throws(AddressFormatException::class)
suspend fun AddressIconGenerator.createAddressModel(
    accountAddress: String,
    sizeInDp: Int,
    accountName: String? = null,
    @ColorRes background: Int = AddressIconGenerator.BACKGROUND_DEFAULT
): AddressModel {
    val icon = createAddressIcon(accountAddress, sizeInDp, background)

    return AddressModel(accountAddress, icon, accountName)
}

@Throws(AddressFormatException::class)
suspend fun AddressIconGenerator.createAddressIcon(
    accountAddress: String,
    sizeInDp: Int,
    @ColorRes background: Int = AddressIconGenerator.BACKGROUND_DEFAULT
) = withContext(Dispatchers.Default) {
    val addressId = accountAddress.toAccountId()

    createAddressIcon(addressId, sizeInDp, background)
}

class CachingAddressIconGenerator(
    private val delegate: AddressIconGenerator
) : AddressIconGenerator {

    val cache = ConcurrentHashMap<String, Drawable>()

    override suspend fun createAddressIcon(
        accountId: AccountId,
        sizeInDp: Int,
        @ColorRes backgroundColorRes: Int
    ): Drawable = withContext(Dispatchers.Default) {
        val key = "${accountId.toHexString()}:$sizeInDp:$backgroundColorRes"

        cache.getOrPut(key) {
            delegate.createAddressIcon(accountId, sizeInDp, backgroundColorRes)
        }
    }
}

class StatelessAddressIconGenerator(
    private val iconGenerator: IconGenerator,
    private val resourceManager: ResourceManager
) : AddressIconGenerator {

    override suspend fun createAddressIcon(
        accountId: AccountId,
        sizeInDp: Int,
        @ColorRes backgroundColorRes: Int
    ) = withContext(Dispatchers.Default) {
        val sizeInPx = resourceManager.measureInPx(sizeInDp)
        val backgroundColor = resourceManager.getColor(backgroundColorRes)

        iconGenerator.getSvgImage(accountId, sizeInPx, backgroundColor = backgroundColor)
    }
}
