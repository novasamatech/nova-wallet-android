package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.append
import io.novafoundation.nova.common.utils.appendEnd
import io.novafoundation.nova.common.utils.appendSpace
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.drawableSpan
import io.novafoundation.nova.common.utils.splitCamelCase
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_impl.R

class ProxyFormatter(
    private val walletUiUseCase: WalletUiUseCase,
    private val resourceManager: ResourceManager,
) {

    suspend fun mapProxyMetaAccountSubtitle(
        proxyAccountName: String,
        proxyAccountIcon: Drawable,
        proxyAccount: ProxyAccount
    ): CharSequence {
        val proxyType = mapProxyTypeToString(proxyAccount.proxyType)
        val formattedProxyMetaAccount = mapProxyMetaAccount(proxyAccountName, proxyAccountIcon)

        return SpannableStringBuilder(proxyType)
            .append(":")
            .appendSpace()
            .append(formattedProxyMetaAccount)
    }

    suspend fun mapProxyMetaAccount(proxyAccountName: String, proxyAccountIcon: Drawable): CharSequence {
        return SpannableStringBuilder()
            .appendEnd(drawableSpan(proxyAccountIcon))
            .appendSpace()
            .append(proxyAccountName, colorSpan(resourceManager.getColor(R.color.text_primary)))
    }

    fun mapProxyTypeToString(type: ProxyAccount.ProxyType): String {
        val proxyType = when (type) {
            ProxyAccount.ProxyType.Any -> resourceManager.getString(R.string.account_proxy_type_any)
            ProxyAccount.ProxyType.NonTransfer -> resourceManager.getString(R.string.account_proxy_type_non_transfer)
            ProxyAccount.ProxyType.Governance -> resourceManager.getString(R.string.account_proxy_type_governance)
            ProxyAccount.ProxyType.Staking -> resourceManager.getString(R.string.account_proxy_type_staking)
            ProxyAccount.ProxyType.IdentityJudgement -> resourceManager.getString(R.string.account_proxy_type_identity_judgement)
            ProxyAccount.ProxyType.CancelProxy -> resourceManager.getString(R.string.account_proxy_type_cancel_proxy)
            ProxyAccount.ProxyType.Auction -> resourceManager.getString(R.string.account_proxy_type_auction)
            ProxyAccount.ProxyType.NominationPools -> resourceManager.getString(R.string.account_proxy_type_nomination_pools)
            is ProxyAccount.ProxyType.Other -> type.name.splitCamelCase().joinToString(separator = " ") { it.capitalize() }
        }

        return resourceManager.getString(R.string.proxy_wallet_type, proxyType)
    }

    suspend fun makeAccountDrawable(metaAccount: MetaAccount): Drawable {
        return walletUiUseCase.walletIcon(metaAccount, 16)
    }
}
