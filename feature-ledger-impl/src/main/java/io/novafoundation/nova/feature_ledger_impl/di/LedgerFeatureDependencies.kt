package io.novafoundation.nova.feature_ledger_impl.di

import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface LedgerFeatureDependencies {

    val chainRegistry: ChainRegistry

    val appLinksProvider: AppLinksProvider
}
