package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec

import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.Flow

interface AddressInputSpecProvider {

    val spec: Flow<AddressInputSpec>
}

interface AddressInputSpec {

    fun isValidAddress(input: String): Boolean

    suspend fun generateIcon(input: String): Result<Drawable>
}
