package io.novafoundation.nova.caip.caip2.matchers

import io.novafoundation.nova.caip.caip2.identifier.Caip2Identifier

interface Caip2Matcher {

    fun match(caip2Identifier: Caip2Identifier): Boolean
}
