package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindPerbillNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs

private const val BLOCKED_DEFAULT = false

fun bindValidatorPrefs(decoded: Any?): ValidatorPrefs {
    val asStruct = decoded.castToStruct()

    return ValidatorPrefs(
        commission = bindPerbillNumber(asStruct.getTyped("commission")),
        blocked = asStruct["blocked"] ?: BLOCKED_DEFAULT
    )
}
