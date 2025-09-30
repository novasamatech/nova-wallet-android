package io.novafoundation.nova.feature_ahm_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter

interface ChainMigrationRouter : ReturnableRouter {

    fun openChainMigrationDetails(chainId: String)
}
