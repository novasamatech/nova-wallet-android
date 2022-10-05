package io.novafoundation.nova.feature_crowdloan_api.data.network.updater

import io.novafoundation.nova.core.updater.UpdateSystem

interface ContributionsUpdateSystemFactory {
    fun create(): UpdateSystem
}
