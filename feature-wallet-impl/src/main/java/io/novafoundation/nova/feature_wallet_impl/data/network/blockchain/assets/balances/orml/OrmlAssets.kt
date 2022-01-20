package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml

import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.tokens
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import java.math.BigInteger

class OrmlAccountData(
    val free: BigInteger,
    val reserved: BigInteger,
    val frozen: BigInteger,
) {

    companion object {

        fun empty(): OrmlAccountData {
            return OrmlAccountData(
                free = BigInteger.ZERO,
                reserved = BigInteger.ZERO,
                frozen = BigInteger.ZERO,
            )
        }
    }
}

@UseCaseBinding
fun bindOrmlAccountData(scale: String, runtime: RuntimeSnapshot): OrmlAccountData {
    val type = runtime.metadata.tokens().storage("Accounts").returnType()

    val dynamicInstance = type.fromHexOrNull(runtime, scale).cast<Struct.Instance>()

    return OrmlAccountData(
        free = bindNumber(dynamicInstance["free"]),
        reserved = bindNumber(dynamicInstance["reserved"]),
        frozen = bindNumber(dynamicInstance["frozen"]),
    )
}
