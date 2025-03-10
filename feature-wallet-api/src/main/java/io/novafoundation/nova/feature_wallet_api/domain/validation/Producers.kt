package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.feature_account_api.data.model.Fee
import java.math.BigDecimal
import java.math.BigInteger

typealias AmountProducer<P> = suspend (P) -> BigDecimal

typealias PlanksProducer<P> = suspend (P) -> BigInteger

typealias SimpleFeeProducer<P> = OptionalFeeProducer<Fee, P>

typealias OptionalFeeProducer<F, P> = suspend (P) -> F?

typealias FeeProducer<F, P> = suspend (P) -> F

typealias FeeListProducer<F, P> = suspend (P) -> List<F>
