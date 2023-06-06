package io.novafoundation.nova.caip.di

import io.novafoundation.nova.caip.caip19.Caip19MatcherFactory
import io.novafoundation.nova.caip.caip19.Caip19Parser
import io.novafoundation.nova.caip.caip2.Caip2MatcherFactory
import io.novafoundation.nova.caip.caip2.Caip2Parser
import io.novafoundation.nova.caip.caip2.Caip2Resolver

interface CaipApi {

    val caip2Parser: Caip2Parser

    val caip2Resolver: Caip2Resolver

    val caip2MatcherFactory: Caip2MatcherFactory

    val caip19Parser: Caip19Parser

    val caip19MatcherFactory: Caip19MatcherFactory
}
