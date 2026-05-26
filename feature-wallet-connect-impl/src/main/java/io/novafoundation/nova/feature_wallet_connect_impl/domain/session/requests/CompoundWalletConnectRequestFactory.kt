package io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests

import com.reown.walletkit.client.Wallet
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull

class CompoundWalletConnectRequestFactory(
    private val nestedFactories: List<WalletConnectRequest.Factory>
) : WalletConnectRequest.Factory {

    override fun create(sessionRequest: Wallet.Model.SessionRequest): WalletConnectRequest? {
        return nestedFactories.tryFindNonNull { it.create(sessionRequest) }
    }
}

fun CompoundWalletConnectRequestFactory(vararg factories: WalletConnectRequest.Factory): CompoundWalletConnectRequestFactory {
    return CompoundWalletConnectRequestFactory(factories.toList())
}
