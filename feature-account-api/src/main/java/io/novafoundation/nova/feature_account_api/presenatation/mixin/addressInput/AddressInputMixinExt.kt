package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun Result<Drawable>.toIdenticonState(): AddressInputState.IdenticonState {
    return fold(
        onSuccess = { AddressInputState.IdenticonState.Address(it) },
        onFailure = { AddressInputState.IdenticonState.Placeholder }
    )
}

fun AddressInputMixin.mixinWithInputFlow(): Flow<MixinWithInput> {
    return inputFlow.map { MixinWithInput(this, it) }
}

class MixinWithInput(
    val mixin: AddressInputMixin,
    val input: String
)

suspend fun MixinWithInput.isInputValid() = mixin.isAddressValid(input)

suspend fun MixinWithInput.isNotEmpty() = input.isNotEmpty()
