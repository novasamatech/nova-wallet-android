package io.novafoundation.nova.common.mixin.restrictions

interface RestrictionCheckMixin {

    // TODO: potentially may add a payload
    suspend fun isRestricted(): Boolean

    // TODO: potentially may add a payload
    suspend fun checkRestrictionAndDo(action: () -> Unit)
}

suspend fun RestrictionCheckMixin.isNotRestricted() = !isRestricted()
