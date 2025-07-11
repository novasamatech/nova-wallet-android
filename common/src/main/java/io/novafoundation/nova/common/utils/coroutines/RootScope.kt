package io.novafoundation.nova.common.utils.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

@Deprecated("Use scope in RootViewModel instead")
class RootScope : CoroutineScope by MainScope()
