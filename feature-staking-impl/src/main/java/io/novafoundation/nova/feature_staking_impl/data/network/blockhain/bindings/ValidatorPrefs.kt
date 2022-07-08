package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import java.math.BigDecimal
import java.math.BigInteger

typealias Perbill = BigDecimal

private const val PERBILL_MANTISSA_SIZE = 9

@HelperBinding
fun bindPerbill(value: BigInteger): Perbill {
    return value.toBigDecimal(scale = PERBILL_MANTISSA_SIZE)
}

fun bindPerbill(dynamic: Any?): Perbill {
    return bindPerbill(dynamic.cast())
}

private const val BLOCKED_DEFAULT = false

fun bindValidatorPrefs(decoded: Any?): ValidatorPrefs {
    val asStruct = decoded.castToStruct()

    return ValidatorPrefs(
        commission = bindPerbill(asStruct.getTyped("commission")),
        blocked = asStruct["blocked"] ?: BLOCKED_DEFAULT
    )
}

@UseCaseBinding
fun bindValidatorPrefs(scale: String, runtime: RuntimeSnapshot): ValidatorPrefs {
    val type = runtime.metadata.staking().storage("Validators").returnType()
    val decoded = type.fromHexOrNull(runtime, scale)

    return bindValidatorPrefs(decoded)
}
