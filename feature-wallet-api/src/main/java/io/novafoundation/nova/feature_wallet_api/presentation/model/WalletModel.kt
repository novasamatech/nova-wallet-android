package io.novafoundation.nova.feature_wallet_api.presentation.model

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

class WalletModel(
    val name: String
)

fun mapMetaAccountToWalletModel(
    metaAccount: MetaAccount
) = WalletModel(
    name = metaAccount.name
)
