package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.core.model.StorageEntry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class WithRawValue<T>(val raw: StorageEntry, val chainId: ChainId, val value: T)

class WithRawScale<T>(val value: T, val rawScale: String)
