package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface PaymentUpdaterFactory {

    fun create(chain: Chain): Updater<MetaAccount>
}
