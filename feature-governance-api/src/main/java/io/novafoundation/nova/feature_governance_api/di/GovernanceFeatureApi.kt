package io.novafoundation.nova.feature_governance_api.di

import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry

interface GovernanceFeatureApi {

    val governanceSourceRegistry: GovernanceSourceRegistry
}
