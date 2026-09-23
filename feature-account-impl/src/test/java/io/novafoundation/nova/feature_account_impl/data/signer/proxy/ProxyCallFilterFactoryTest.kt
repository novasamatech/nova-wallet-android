package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.metadata.module.MetadataFunction
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class ProxyCallFilterFactoryTest {

    private val factory = ProxyCallFilterFactory()

    @Test
    fun `non transfer rejects utility batch containing transfer`() {
        val swap = call(Modules.ASSET_CONVERSION, "swap_exact_tokens_for_tokens")
        val transfer = call(Modules.BALANCES, "transfer_keep_alive")
        val batchAll = call(Modules.UTILITY, "batch_all", mapOf("calls" to listOf(swap, transfer)))

        assertFalse(factory.getCallFilterFor(ProxyType.NonTransfer).canExecute(batchAll))
    }

    @Test
    fun `staking proxy accepts utility batch containing only staking calls`() {
        val bond = call(Modules.STAKING, "bond")
        val nominate = call(Modules.STAKING, "nominate")
        val batchAll = call(Modules.UTILITY, "batch_all", mapOf("calls" to listOf(bond, nominate)))

        assertTrue(factory.getCallFilterFor(ProxyType.Staking).canExecute(batchAll))
    }

    @Test
    fun `any proxy accepts utility batch containing transfer`() {
        val transfer = call(Modules.BALANCES, "transfer_keep_alive")
        val batchAll = call(Modules.UTILITY, "batch_all", mapOf("calls" to listOf(transfer)))

        assertTrue(factory.getCallFilterFor(ProxyType.Any).canExecute(batchAll))
    }

    private fun call(moduleName: String, callName: String, arguments: Map<String, Any?> = emptyMap()): GenericCall.Instance {
        val module = Mockito.mock(Module::class.java)
        Mockito.`when`(module.name).thenReturn(moduleName)

        val function = Mockito.mock(MetadataFunction::class.java)
        Mockito.`when`(function.name).thenReturn(callName)

        return GenericCall.Instance(module, function, arguments)
    }
}
