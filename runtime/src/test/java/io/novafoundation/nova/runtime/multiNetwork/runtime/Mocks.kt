package io.novafoundation.nova.runtime.multiNetwork.runtime

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.test_shared.whenever
import org.mockito.Mockito

object Mocks {
    fun chain(id: String) : Chain {
        val chain = Mockito.mock(Chain::class.java)

        whenever(chain.id).thenReturn(id)

        return chain
    }
}
