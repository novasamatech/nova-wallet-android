package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import android.graphics.drawable.Drawable

fun Result<Drawable>.toIdenticonState(): AddressInputState.IdenticonState {
    return fold(
        onSuccess = { AddressInputState.IdenticonState.Address(it) },
        onFailure = { AddressInputState.IdenticonState.Placeholder }
    )
}
