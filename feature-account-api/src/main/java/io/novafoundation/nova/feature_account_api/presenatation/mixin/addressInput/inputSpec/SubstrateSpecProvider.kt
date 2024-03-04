package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.utils.isValidSS58Address
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SubstrateSpecProvider(
    private val addressIconGenerator: AddressIconGenerator,
) : AddressInputSpecProvider {

    override val spec: Flow<AddressInputSpec> = flowOf(Spec())

    private inner class Spec : AddressInputSpec {

        override fun isValidAddress(input: String): Boolean {
            return input.isValidSS58Address()
        }

        override suspend fun generateIcon(input: String): Result<Drawable> {
            return runCatching {
                addressIconGenerator.createAddressIcon(
                    accountId = input.toAccountId(),
                    sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
                    backgroundColorRes = AddressIconGenerator.BACKGROUND_TRANSPARENT
                )
            }
        }
    }
}
