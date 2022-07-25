package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.isValidAddress
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SingleChainSpecProvider(
    private val addressIconGenerator: AddressIconGenerator,
    targetChain: Flow<Chain>,
) : AddressInputSpecProvider {

    override val spec: Flow<AddressInputSpec> = targetChain.map(::Spec)

    private inner class Spec(private val targetChain: Chain) : AddressInputSpec {

        override fun isValidAddress(input: String): Boolean {
            return targetChain.isValidAddress(input)
        }

        override suspend fun generateIcon(input: String): Result<Drawable> {
            return runCatching {
                require(targetChain.isValidAddress(input))

                addressIconGenerator.createAddressIcon(
                    accountId = targetChain.accountIdOf(address = input),
                    sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
                    backgroundColorRes = AddressIconGenerator.BACKGROUND_TRANSPARENT
                )
            }
        }
    }
}
