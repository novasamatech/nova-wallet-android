package io.novafoundation.nova.feature_crowdloan_api.di

import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor

interface CrowdloanFeatureApi {

    fun repository(): CrowdloanRepository

    fun contributionsInteractor(): ContributionsInteractor

    fun contributionsRepository(): ContributionsRepository
}
