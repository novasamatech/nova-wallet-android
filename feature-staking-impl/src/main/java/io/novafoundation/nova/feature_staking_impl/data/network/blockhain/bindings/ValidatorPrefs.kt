package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindPerbillNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage

private const val BLOCKED_DEFAULT = false

fun bindValidatorPrefs(decoded: Any?): ValidatorPrefs {
    val asStruct = decoded.castToStruct()

    return ValidatorPrefs(
        commission = bindPerbillNumber(asStruct.getTyped("commission")),
        blocked = asStruct["blocked"] ?: BLOCKED_DEFAULT
    )
}

@UseCaseBinding
fun bindValidatorPrefs(scale: String, runtime: RuntimeSnapshot): ValidatorPrefs {
    val type = runtime.metadata.staking().storage("Validators").returnType()
    val decoded = type.fromHexOrNull(runtime, scale)

    return bindValidatorPrefs(decoded)
}
