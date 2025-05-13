package io.novafoundation.nova.feature_pay_impl.domain

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

fun MetaAccount.isValidForShopApi(): Boolean {
    return type == LightMetaAccount.Type.SECRETS && substrateCryptoType == CryptoType.SR25519
}
