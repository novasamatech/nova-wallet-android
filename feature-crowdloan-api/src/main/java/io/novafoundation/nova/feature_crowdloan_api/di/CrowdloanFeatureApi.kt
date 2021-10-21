package io.novafoundation.nova.feature_crowdloan_api.di

import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository

interface CrowdloanFeatureApi {

    fun repository(): CrowdloanRepository
}
