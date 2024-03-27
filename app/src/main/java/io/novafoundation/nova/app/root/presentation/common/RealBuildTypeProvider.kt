package io.novafoundation.nova.app.root.presentation.common

import io.novafoundation.nova.app.BuildConfig
import io.novafoundation.nova.common.interfaces.BuildType
import io.novafoundation.nova.common.interfaces.BuildTypeProvider

class RealBuildTypeProvider : BuildTypeProvider {

    override fun isDebug(): Boolean {
        return BuildConfig.DEBUG
    }

    override fun getBuildType(): BuildType? {
        return when (BuildConfig.BUILD_TYPE) {
            "debug" -> BuildType.DEBUG
            "develop" -> BuildType.DEVELOP
            "instrumentalTest" -> BuildType.INSTRUMENTAL_TEST
            "release" -> BuildType.RELEASE
            "releaseTest" -> BuildType.RELEASE_TEST
            "releaseMarket" -> BuildType.RELEASE_MARKET
            "releaseGithub" -> BuildType.RELEASE_GITHUB
            else -> null
        }
    }
}
