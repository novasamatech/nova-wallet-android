package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.delegated

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
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.ProxyFormatter
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType

class RealProxyFormatter(
    private val walletUiUseCase: WalletUiUseCase,
    private val resourceManager: ResourceManager,
) : ProxyFormatter {

    override suspend fun formatProxiedMetaAccountSubtitle(
        proxy: MetaAccount,
        proxyAccount: ProxyAccount
    ): CharSequence {
        val proxyType = mapProxyTypeToString(proxyAccount.proxyType)
        val formattedProxyMetaAccount = formatProxyMetaAccount(proxy)

        return SpannableStringBuilder(proxyType)
            .append(":")
            .appendSpace()
            .append(formattedProxyMetaAccount)
    }

    override suspend fun formatProxyMetaAccount(proxy: MetaAccount): CharSequence {
        val icon = makeProxyDrawable(proxy)

        return SpannableStringBuilder()
            .appendEnd(drawableSpan(icon))
            .appendSpace()
            .append(proxy.name, colorSpan(resourceManager.getColor(R.color.text_primary)))
    }

    override fun mapProxyTypeToString(type: ProxyType): String {
        val proxyType = when (type) {
            ProxyType.Any -> resourceManager.getString(R.string.account_proxy_type_any)
            ProxyType.NonTransfer -> resourceManager.getString(R.string.account_proxy_type_non_transfer)
            ProxyType.Governance -> resourceManager.getString(R.string.account_proxy_type_governance)
            ProxyType.Staking -> resourceManager.getString(R.string.account_proxy_type_staking)
            ProxyType.IdentityJudgement -> resourceManager.getString(R.string.account_proxy_type_identity_judgement)
            ProxyType.CancelProxy -> resourceManager.getString(R.string.account_proxy_type_cancel_proxy)
            ProxyType.Auction -> resourceManager.getString(R.string.account_proxy_type_auction)
            ProxyType.NominationPools -> resourceManager.getString(R.string.account_proxy_type_nomination_pools)
            is ProxyType.Other -> type.name.splitCamelCase().joinToString(separator = " ") { it.capitalize() }
        }

        return resourceManager.getString(R.string.proxy_wallet_type, proxyType)
    }

    override suspend fun makeProxyDrawable(proxy: MetaAccount): Drawable {
        return walletUiUseCase.walletIcon(proxy, SUBTITLE_ICON_SIZE_DP)
    }
}
