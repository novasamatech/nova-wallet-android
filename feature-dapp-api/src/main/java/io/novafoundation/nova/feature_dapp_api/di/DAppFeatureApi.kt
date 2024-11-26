package io.novafoundation.nova.feature_dapp_api.di

import io.novafoundation.nova.feature_dapp_api.DAppRouter
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository

interface DAppFeatureApi {

    val dappMetadataRepository: DAppMetadataRepository

    val dappRouter: DAppRouter
}
