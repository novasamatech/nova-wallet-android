package io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.extensions.tryFindNonNull
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ReferendumPreImageParser {

    suspend fun parse(preImage: PreImage, chainId: ChainId): ReferendumCall?
}

interface ReferendumCallAdapter {

    suspend fun fromCall(call: GenericCall.Instance, context: ReferendumCallParseContext): ReferendumCall?
}

interface ReferendumCallParseContext {

    val chainId: ChainId

    /**
     * Can be used to resolve nested calls in compound calls like batch or proxy calls
     * Do not pass the same call you're processing otherwise you'll get a stack overflow
     */
    suspend fun parse(call: GenericCall.Instance): ReferendumCall?
}

class RealReferendumPreImageParser(
    private val knownAdapters: Collection<ReferendumCallAdapter>,
) : ReferendumPreImageParser {

    override suspend fun parse(preImage: PreImage, chainId: ChainId): ReferendumCall? {
        val context = RealReferendumCallParseContext(chainId, knownAdapters)

        return withContext(Dispatchers.IO) {
            context.parse(preImage.call)
        }
    }

    private inner class RealReferendumCallParseContext(
        override val chainId: ChainId,
        private val knownAdapters: Collection<ReferendumCallAdapter>,
    ) : ReferendumCallParseContext {

        override suspend fun parse(call: GenericCall.Instance): ReferendumCall? {
            return knownAdapters.tryFindNonNull { adapter ->
                runCatching { adapter.fromCall(call, context = this) }.getOrNull()
            }
        }
    }
}
