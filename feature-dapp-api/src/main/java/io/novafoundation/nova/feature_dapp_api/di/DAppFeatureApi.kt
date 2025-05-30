package io.novafoundation.nova.feature_dapp_api.di

import io.novafoundation.nova.feature_dapp_api.data.repository.BrowserTabExternalRepository
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_api.di.deeplinks.DAppDeepLinks

interface DAppFeatureApi {

    val dappMetadataRepository: DAppMetadataRepository

    val browserTabsRepository: BrowserTabExternalRepository

    val dAppDeepLinks: DAppDeepLinks
}
