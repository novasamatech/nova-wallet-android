package io.novafoundation.nova.caip.caip2

import io.novafoundation.nova.caip.caip2.matchers.Caip2Matcher
import io.novafoundation.nova.caip.caip2.matchers.Caip2MatcherList
import io.novafoundation.nova.caip.caip2.matchers.Eip155Matcher
import io.novafoundation.nova.caip.caip2.matchers.SubstrateCaip2Matcher
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface Caip2MatcherFactory {

    suspend fun getCaip2Matcher(chain: Chain): Caip2Matcher
}

internal class RealCaip2MatcherFactory : Caip2MatcherFactory {

    override suspend fun getCaip2Matcher(chain: Chain): Caip2Matcher {
        val matchers = buildList {
            add(SubstrateCaip2Matcher(chain))

            if (chain.isEthereumBased) {
                add(Eip155Matcher(chain))
            }
        }
        return Caip2MatcherList(matchers)
    }
}
