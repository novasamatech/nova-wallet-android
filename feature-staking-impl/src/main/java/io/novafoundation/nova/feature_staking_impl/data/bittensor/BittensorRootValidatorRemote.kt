package io.novafoundation.nova.feature_staking_impl.data.bittensor

class BittensorRootValidatorRemote(
    val validator: Validator?,
    val meta: Meta?
) {
    class Validator(val hotkey: String?, val name: String?, val stakeYield: Yield?)
    class Yield(val h24: Window?)
    class Window(val apy: String?, val coverage: String?, val valueQuality: String?)
    class Meta(val components: Components?)
    class Components(val rootValidator: BittensorRecommendationsRemote.Component?)
}
