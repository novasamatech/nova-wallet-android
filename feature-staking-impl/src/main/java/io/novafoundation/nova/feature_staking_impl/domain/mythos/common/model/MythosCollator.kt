package io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.WithAccountId
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.extensions.toHexString

class MythosCollator(
    override val accountId: AccountIdKey,
    val identity: OnChainIdentity?,
    val totalStake: Balance,
    val delegators: Int,
    val apr: Fraction?,
) : Identifiable, WithAccountId {

    override val identifier: String = accountId.value.toHexString()
}

val MythosCollator.isActive: Boolean
    get() = apr != null
