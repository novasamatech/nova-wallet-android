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
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_impl.R
import javax.inject.Inject

@FeatureScope
class MultisigFormatter @Inject constructor(
    private val walletUiUseCase: WalletUiUseCase,
    private val resourceManager: ResourceManager,
) {

    suspend fun formatSignatorySubtitle(signatory: MetaAccount): CharSequence {
        val icon = makeAccountDrawable(signatory)
        return formatSignatorySubtitle(signatory, icon)
    }

    fun formatSignatorySubtitle(signatory: MetaAccount, icon: Drawable): CharSequence {
        val formattedProxyMetaAccount = formatAccount(signatory.name, icon)

        return SpannableStringBuilder(resourceManager.getString(R.string.multisig_signatory))
            .appendSpace()
            .append(formattedProxyMetaAccount)
    }

    suspend fun makeAccountDrawable(metaAccount: MetaAccount): Drawable {
        // TODO multisig: this does db request for each icon. We should probably batch it. Same with proxieds
        return walletUiUseCase.walletIcon(metaAccount, SUBTITLE_ICON_SIZE_DP)
    }

    // TODO multisig: refactor duplication with ProxyFormatter
    private fun formatAccount(proxyAccountName: String, proxyAccountIcon: Drawable): CharSequence {
        return SpannableStringBuilder()
            .appendEnd(drawableSpan(proxyAccountIcon))
            .appendSpace()
            .append(proxyAccountName, colorSpan(resourceManager.getColor(R.color.text_primary)))
    }
}
