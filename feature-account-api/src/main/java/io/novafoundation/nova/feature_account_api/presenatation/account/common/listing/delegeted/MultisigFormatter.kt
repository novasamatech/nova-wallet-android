package io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted

import android.graphics.drawable.Drawable
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

interface MultisigFormatter {

    suspend fun formatSignatorySubtitle(signatory: MetaAccount): CharSequence

    fun formatSignatorySubtitle(signatory: MetaAccount, icon: Drawable): CharSequence

    suspend fun formatSignatory(signatory: MetaAccount): CharSequence

    suspend fun makeAccountDrawable(metaAccount: MetaAccount): Drawable

    suspend fun makeAccountDrawable(accountId: ByteArray): Drawable
}
