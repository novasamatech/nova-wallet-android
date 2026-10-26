package io.novafoundation.nova.feature_staking_impl.data.bittensor

class BittensorRootOverviewRemote(val stakeIndex: StakeIndex?, val meta: Meta?) {

    class StakeIndex(val stakeYield: BittensorRootValidatorRemote.Yield?)

    class Meta(val components: Components?)

    class Components(val indexes: BittensorRecommendationsRemote.Component?)
}
