package io.novafoundation.nova.feature_staking_impl.data.bittensor

class BittensorRootValidatorRemote(
    val items: List<Validator>?,
    val meta: Meta?
) {
    class Validator(val hotkey: String?, val name: String?, val stakeYield: Yield?, val rootStakeTao: String?)
    class Yield(val d30: Window?)
    class Window(val apy: String?)
    class Meta(val components: Components?)
    class Components(val rootValidators: BittensorRecommendationsRemote.Component?, val stakeYieldWindows: BittensorRecommendationsRemote.Component?)
}
