package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.model

import android.os.Parcelable
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapIdentityParcelModelToIdentity
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapIdentityToIdentityParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.IdentityParcelModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.android.parcel.Parcelize

@Parcelize
class MythosCollatorParcel(
    val accountId: AccountId,
    val identity: IdentityParcelModel?,
    val totalStake: Balance,
    val delegators: Int,
    val apr: Double?
) : Parcelable

fun MythosCollator.toParcelable(): MythosCollatorParcel {
    return MythosCollatorParcel(
        accountId = this.accountId.value,
        identity = this.identity?.let { mapIdentityToIdentityParcelModel(it) },
        totalStake = this.totalStake,
        delegators = this.delegators,
        apr = this.apr?.inFraction
    )
}

fun MythosCollatorParcel.toDomain(): MythosCollator {
    return MythosCollator(
        accountId = accountId.intoKey(),
        identity = identity?.let { mapIdentityParcelModelToIdentity(it) },
        totalStake = totalStake,
        delegators = delegators,
        apr = apr?.fractions
    )
}
