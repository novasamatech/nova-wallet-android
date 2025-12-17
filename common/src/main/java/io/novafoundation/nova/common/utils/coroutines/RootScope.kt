package io.novafoundation.nova.common.utils.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

@RequiresOptIn(
    message = """
        Using RootScope might have unintended side effects.
        In case when we use RootScope in RootViewModel and its is re-created - RootScope jobs wont be cancelled that may give us duplicated jobs
    """,
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
annotation class DangerousScope

@DangerousScope
class RootScope : CoroutineScope by MainScope()
