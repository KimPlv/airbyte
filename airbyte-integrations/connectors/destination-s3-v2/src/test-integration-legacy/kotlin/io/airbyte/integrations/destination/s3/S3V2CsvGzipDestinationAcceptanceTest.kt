/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */
package io.airbyte.integrations.destination.s3

import com.fasterxml.jackson.databind.JsonNode
import io.airbyte.cdk.integrations.destination.s3.S3BaseCsvGzipDestinationAcceptanceTest
import io.airbyte.cdk.integrations.standardtest.destination.ProtocolVersion
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class S3V2CsvGzipDestinationAcceptanceTest : S3BaseCsvGzipDestinationAcceptanceTest() {
    override val imageName: String = "airbyte/destination-s3-v2:dev"
    override fun getProtocolVersion(): ProtocolVersion {
        return ProtocolVersion.V1
    }

    override val baseConfigJson: JsonNode
        get() = S3V2DestinationTestUtils.baseConfigJsonFilePath
}
