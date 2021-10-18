package jp.co.soramitsu.common.mixin

import kotlinx.coroutines.CoroutineScope

interface MixinFactory<M> {

    fun create(scope: CoroutineScope): M
}
