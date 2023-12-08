package io.novafoundation.nova.feature_staking_api.domain.model

import java.math.BigInteger

class IndividualExposure(val who: ByteArray, val value: BigInteger)

class Exposure(val total: BigInteger, val own: BigInteger, val others: List<IndividualExposure>)


class ExposureOverview(val total: BigInteger, val own: BigInteger, val pageCount: BigInteger, val nominatorCount: BigInteger)

class ExposurePage(val others: List<IndividualExposure>)
