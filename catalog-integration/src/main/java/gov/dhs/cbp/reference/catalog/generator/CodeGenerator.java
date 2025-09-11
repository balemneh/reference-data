package gov.dhs.cbp.reference.catalog.generator;

import gov.dhs.cbp.reference.catalog.client.OpenMetadataClient.DatasetMetadata;

public interface CodeGenerator {
    String generate(DatasetMetadata dataset);
    String getEngineType();
}