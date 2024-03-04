package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml

import io.novafoundation.nova.common.data.network.runtime.binding.AccountBalance
import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindOrmlAccountData
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.tokens
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHexOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.storage

@UseCaseBinding
fun bindOrmlAccountData(scale: String, runtime: RuntimeSnapshot): AccountBalance {
    val type = runtime.metadata.tokens().storage("Accounts").returnType()

    val dynamicInstance = type.fromHexOrNull(runtime, scale)

    return bindOrmlAccountData(dynamicInstance)
}
