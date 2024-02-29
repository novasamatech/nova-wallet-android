package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novasama.substrate_sdk_android.extensions.asEthereumAddress
import io.novasama.substrate_sdk_android.extensions.isValid
import io.novasama.substrate_sdk_android.extensions.toAccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class EVMSpecProvider(
    private val addressIconGenerator: AddressIconGenerator
) : AddressInputSpecProvider {

    override val spec: Flow<AddressInputSpec> = flowOf(Spec())

    private inner class Spec : AddressInputSpec {

        override fun isValidAddress(input: String): Boolean {
            return input.asEthereumAddress().isValid()
        }

        override suspend fun generateIcon(input: String): Result<Drawable> {
            return runCatching {
                addressIconGenerator.createAddressIcon(
                    accountId = input.asEthereumAddress().toAccountId().value,
                    sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
                    backgroundColorRes = AddressIconGenerator.BACKGROUND_TRANSPARENT
                )
            }
        }
    }
}
