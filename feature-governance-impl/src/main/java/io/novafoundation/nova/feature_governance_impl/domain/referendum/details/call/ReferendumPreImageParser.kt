package io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call

import android.util.Log
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ReferendumPreImageParser {

    suspend fun parse(preImage: PreImage, chain: Chain): ReferendumCall?

    suspend fun parsePreimageCall(call: GenericCall.Instance, chain: Chain): ReferendumCall?
}

interface ReferendumCallAdapter {

    suspend fun fromCall(call: GenericCall.Instance, context: ReferendumCallParseContext): ReferendumCall?
}

interface ReferendumCallParseContext {

    val chain: Chain

    /**
     * Can be used to resolve nested calls in compound calls like batch or proxy calls
     * Do not pass the same call you're processing otherwise you'll get a stack overflow
     */
    suspend fun parse(call: GenericCall.Instance): ReferendumCall?
}

class RealReferendumPreImageParser(
    private val knownAdapters: Collection<ReferendumCallAdapter>,
) : ReferendumPreImageParser {

    override suspend fun parse(preImage: PreImage, chain: Chain): ReferendumCall? {
        return parsePreimageCall(preImage.call, chain)
    }

    override suspend fun parsePreimageCall(call: GenericCall.Instance, chain: Chain): ReferendumCall? {
        val context = RealReferendumCallParseContext(chain, knownAdapters)

        return withContext(Dispatchers.IO) {
            context.parse(call)
        }
    }

    private inner class RealReferendumCallParseContext(
        override val chain: Chain,
        private val knownAdapters: Collection<ReferendumCallAdapter>,
    ) : ReferendumCallParseContext {

        override suspend fun parse(call: GenericCall.Instance): ReferendumCall? {
            return knownAdapters.tryFindNonNull { adapter ->
                runCatching { adapter.fromCall(call, context = this) }
                    .onFailure { Log.e("ReferendumPreImageParser", "Adapter ${adapter::class.simpleName} failed to parse call", it) }
                    .getOrNull()
            }
        }
    }
}
