package io.novafoundation.nova.common.interfaces

interface BuildTypeProvider {

    fun isDebug(): Boolean

    fun getBuildType(): BuildType?
}

enum class BuildType {
    DEBUG,
    DEVELOP,
    INSTRUMENTAL_TEST,
    RELEASE,
    RELEASE_TEST,
    RELEASE_MARKET,
    RELEASE_GITHUB,
}

fun BuildTypeProvider.isMarketRelease(): Boolean {
    return getBuildType() == BuildType.RELEASE_MARKET
}
