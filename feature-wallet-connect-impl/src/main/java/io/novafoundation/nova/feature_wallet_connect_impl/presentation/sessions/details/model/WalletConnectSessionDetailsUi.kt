package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.model

import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainListOverview
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

class WalletConnectSessionDetailsUi(
    val dappTitle: String,
    val dappUrl: String?,
    val dappIcon: String?,
    val networksOverview: ChainListOverview,
    val networks: List<ChainUi>,
    val wallet: WalletModel,
    val status: SessionStatus
) {

    class SessionStatus(
        val label: String,
        val labelStyle: TableCellView.FieldStyle,
        @DrawableRes val icon: Int
    )
}
