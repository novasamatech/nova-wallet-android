package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.myselfBehavior

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NoMyselfBehaviorProvider : MyselfBehaviorProvider {

    override val behavior: Flow<MyselfBehavior> = flowOf(Behavior())

    private class Behavior : MyselfBehavior {
        override suspend fun myselfAvailable(): Boolean = false

        override suspend fun myself(): String? = null
    }
}
