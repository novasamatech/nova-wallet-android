package io.novafoundation.nova.runtime.util

import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.dynamic.DynamicTypeResolver
import io.novasama.substrate_sdk_android.runtime.definitions.dynamic.extentsions.GenericsExtension
import io.novasama.substrate_sdk_android.runtime.definitions.registry.TypeRegistry
import io.novasama.substrate_sdk_android.runtime.definitions.registry.v14Preset
import io.novasama.substrate_sdk_android.runtime.definitions.v14.TypesParserV14
import io.novasama.substrate_sdk_android.runtime.metadata.GetMetadataRequest
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadataReader
import io.novasama.substrate_sdk_android.runtime.metadata.builder.VersionedRuntimeBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.v14.RuntimeMetadataSchemaV14
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.executeAsync
import io.novasama.substrate_sdk_android.wsrpc.mappers.nonNull
import io.novasama.substrate_sdk_android.wsrpc.mappers.pojo

suspend fun SocketService.fetchRuntimeSnapshot(): RuntimeSnapshot {
    val metadataHex = stateGetMetadata()
    val metadataReader = RuntimeMetadataReader.read(metadataHex)

    val types = TypesParserV14.parse(
        lookup = metadataReader.metadata[RuntimeMetadataSchemaV14.lookup],
        typePreset = v14Preset(),
    )

    val typeRegistry = TypeRegistry(types, DynamicTypeResolver(DynamicTypeResolver.DEFAULT_COMPOUND_EXTENSIONS + GenericsExtension))
    val runtimeMetadata = VersionedRuntimeBuilder.buildMetadata(metadataReader, typeRegistry)

    return RuntimeSnapshot(typeRegistry, runtimeMetadata)
}

suspend fun SocketService.stateGetMetadata(): String {
    return executeAsync(GetMetadataRequest, mapper = pojo<String>().nonNull())
}
