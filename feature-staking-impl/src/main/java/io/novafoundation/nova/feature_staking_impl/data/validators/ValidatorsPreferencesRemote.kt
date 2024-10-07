package io.novafoundation.nova.feature_staking_impl.data.validators

class ValidatorsPreferencesRemote(
    val preferred: Map<String, Set<String>>,
    val excluded: Map<String, Set<String>>
)
