package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.delegated

import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.append
import io.novafoundation.nova.common.utils.appendEnd
import io.novafoundation.nova.common.utils.appendSpace
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.drawableSpan
import io.novafoundation.nova.feature_account_api.domain.model.DerivativeMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.DerivativeFormatter
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.IconTransform
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_impl.R
import javax.inject.Inject

@FeatureScope
class RealDerivativeFormatter @Inject constructor(
    private val walletUiUseCase: WalletUiUseCase,
    private val resourceManager: ResourceManager
) : DerivativeFormatter {

    override suspend fun formatDeriveAccountSubtitle(
        derivativeMetaAccount: DerivativeMetaAccount,
        parent: MetaAccount,
        iconTransform: IconTransform
    ): CharSequence {
        val childLabel = resourceManager.getString(R.string.derivative_subtitle, derivativeMetaAccount.index)
        val formattedParent = formatDerivativeParent(parent, iconTransform)

        return SpannableStringBuilder(childLabel)
            .appendSpace()
            .append(formattedParent)
    }

    override suspend fun formatDerivativeParent(
        parent: MetaAccount,
        iconTransform: IconTransform
    ): CharSequence {
        val parentDrawable = iconTransform(makeParentDrawable(parent))

        return SpannableStringBuilder()
            .appendEnd(drawableSpan(parentDrawable))
            .appendSpace()
            .append(parent.name, colorSpan(resourceManager.getColor(R.color.text_primary)))

    }

    override suspend fun makeParentDrawable(parent: MetaAccount): Drawable {
        return walletUiUseCase.walletIcon(parent, SUBTITLE_ICON_SIZE_DP)
    }
}
