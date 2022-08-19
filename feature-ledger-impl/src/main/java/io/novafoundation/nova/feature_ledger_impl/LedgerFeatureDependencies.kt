package io.novafoundation.nova.feature_ledger_impl

import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface LedgerFeatureDependencies {

    val chainRegistry: ChainRegistry
}
