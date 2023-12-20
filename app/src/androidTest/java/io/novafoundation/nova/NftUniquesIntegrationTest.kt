package io.novafoundation.nova

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindString
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.uniques
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeComponent
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigInteger

data class UniquesClass(
    val id: BigInteger,
    val metadata: Metadata?,
    val details: Details
) {
    data class Metadata(
        val deposit: BigInteger,
        val data: String,
    )

    data class Details(
        val instances: BigInteger,
        val frozen: Boolean
    )
}

data class UniquesInstance(
    val collection: UniquesClass,
    val id: BigInteger,
    val metadata: Metadata?,
    val details: Details
) {

    data class Metadata(
        val data: String,
    )

    data class Details(
        val owner: String,
        val frozen: Boolean,
    )
}

class NftUniquesIntegrationTest {

    private val chainGenesis = "48239ef607d7928874027a43a67689209727dfb3d3dc5e5b03a39bdc2eda771a"

    private val runtimeApi = FeatureUtils.getFeature<RuntimeComponent>(
        ApplicationProvider.getApplicationContext<Context>(),
        RuntimeApi::class.java
    )

    private val chainRegistry = runtimeApi.chainRegistry()
    private val externalRequirementFlow = runtimeApi.externalRequirementFlow()

    private val storageRemoteSource = runtimeApi.remoteStorageSource()

    @Test
    fun testUniquesIntegration(): Unit = runBlocking {
        chainRegistry.currentChains.first() // wait till chains are ready
        externalRequirementFlow.emit(ChainConnection.ExternalRequirement.ALLOWED)

        val chain = chainRegistry.getChain(chainGenesis)

        val accountId = "JGKSibhyZgzY7jEe5a9gdybDEbqNNRSxYyJJmeeycbCbQ5v".toAccountId()

        val instances = storageRemoteSource.query(chainGenesis) {
            val classesWithInstances = runtime.metadata.uniques().storage("Account").keys(accountId)
                .map { (_: AccountId, collection: BigInteger, instance: BigInteger) ->
                    listOf(collection, instance)
                }

            val classesIds = classesWithInstances.map { (collection, _) -> collection }.distinct()

            val classMetadataStorage = runtime.metadata.uniques().storage("ClassMetadataOf")
            val classStorage = runtime.metadata.uniques().storage("Class")
            val instanceMetadataStorage = runtime.metadata.uniques().storage("InstanceMetadataOf")
            val instanceDetailsStorage = runtime.metadata.uniques().storage("Asset")

            val multiQueryResults = multiInternal {
                classMetadataStorage.querySingleArgKeys(classesIds)
                classStorage.querySingleArgKeys(classesIds)
                instanceMetadataStorage.queryKeys(classesWithInstances)
                instanceDetailsStorage.queryKeys(classesWithInstances)
            }

            val classDetails = multiQueryResults.getValue(classStorage)
                .mapKeys { (keyComponents, _) -> keyComponents.component1<BigInteger>() }
                .mapValues { (_, parsedValue) ->
                    val classDetailsStruct = parsedValue.cast<Struct.Instance>()

                    UniquesClass.Details(
                        instances = classDetailsStruct.getTyped("instances"),
                        frozen = classDetailsStruct.getTyped("isFrozen")
                    )
                }

            val classMetadatas = multiQueryResults.getValue(classMetadataStorage)
                .mapKeys { (keyComponents, _) -> keyComponents.component1<BigInteger>() }
                .mapValues { (_, parsedValue) ->
                    parsedValue?.cast<Struct.Instance>()?.let { classMetadataStruct ->
                        UniquesClass.Metadata(
                            deposit = classMetadataStruct.getTyped("deposit"),
                            data = bindString(classMetadataStruct["data"])
                        )
                    }
                }

            val instancesDetails = multiQueryResults.getValue(instanceDetailsStorage)
                .mapKeys { (keyComponents, _) -> keyComponents.component1<BigInteger>() to keyComponents.component2<BigInteger>() }
                .mapValues { (_, parsedValue) ->
                    val instanceDetailsStruct = parsedValue.cast<Struct.Instance>()

                    UniquesInstance.Details(
                        owner = chain.addressOf(bindAccountId(instanceDetailsStruct["owner"])),
                        frozen = bindBoolean(instanceDetailsStruct["isFrozen"])
                    )
                }

            val instancesMetadatas = multiQueryResults.getValue(instanceMetadataStorage)
                .mapKeys { (keyComponents, _) -> keyComponents.component1<BigInteger>() to keyComponents.component2<BigInteger>() }
                .mapValues { (_, parsedValue) ->
                    parsedValue?.cast<Struct.Instance>()?.let {
                        UniquesInstance.Metadata(
                            data = bindString(it["data"])
                        )
                    }
                }

            val classes = classesIds.associateWith { classId ->
                UniquesClass(
                    id = classId,
                    metadata = classMetadatas[classId],
                    details = classDetails.getValue(classId)
                )
            }

            classesWithInstances.map { (collectionId, instanceId) ->
                val instanceKey = collectionId to instanceId

                UniquesInstance(
                    collection = classes.getValue(collectionId),
                    id = instanceId,
                    metadata = instancesMetadatas[instanceKey],
                    details = instancesDetails.getValue(instanceKey)
                )
            }
        }

        Log.d(LOG_TAG, instances.toString())
    }
}
