package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

@JvmInline
value class PreImage(val call: GenericCall.Instance)
