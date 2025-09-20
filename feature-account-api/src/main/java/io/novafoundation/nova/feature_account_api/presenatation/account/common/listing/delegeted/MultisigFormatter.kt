package io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

interface MultisigFormatter {

    suspend fun formatSignatorySubtitle(
        signatory: MetaAccount,
        iconTransform: IconTransform = { it }
    ): CharSequence

    suspend fun formatSignatory(signatory: MetaAccount): CharSequence

    suspend fun makeSignatoryDrawable(accountId: AccountIdKey): Drawable
}
