package io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted

import android.graphics.drawable.Drawable
import io.novafoundation.nova.feature_account_api.domain.model.DerivativeMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

typealias IconTransform = (Drawable) -> Drawable

interface DerivativeFormatter {

    suspend fun formatDerivativeParent(
        parent: MetaAccount,
        iconTransform: IconTransform = { it },
    ): CharSequence

    suspend fun formatDeriveAccountSubtitle(
        derivativeMetaAccount: DerivativeMetaAccount,
        parent: MetaAccount,
        iconTransform: IconTransform = { it },
    ): CharSequence


   suspend fun makeParentDrawable(parent: MetaAccount): Drawable
}
