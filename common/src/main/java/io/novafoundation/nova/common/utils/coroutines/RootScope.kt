package io.novafoundation.nova.common.utils.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

@Deprecated("Using RootScope might have unintended side effects in case RootViewModel, where most of the clients of RootScope are used, is re-created - RootScope jobs wont be cancelled leading to potentially duplicated jobs")
class RootScope : CoroutineScope by MainScope()
