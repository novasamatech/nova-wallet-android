package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import java.math.BigDecimal
import java.math.BigInteger

typealias AmountProducer<P> = suspend (P) -> BigDecimal

typealias PlanksProducer<P> = suspend (P) -> BigInteger

typealias TokenProducer<P> = suspend (P) -> Token
