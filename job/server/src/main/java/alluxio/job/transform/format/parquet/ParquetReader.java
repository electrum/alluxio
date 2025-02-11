/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.job.transform.format.parquet;

import alluxio.AlluxioURI;
import alluxio.job.transform.format.TableReader;
import alluxio.job.transform.format.ReadWriterUtils;
import alluxio.job.transform.format.TableRow;
import alluxio.job.transform.format.TableSchema;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.ParquetReadOptions;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroSchemaConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.InputFile;

import java.io.IOException;

/**
 * A reader for reading {@link ParquetRow}.
 */
public final class ParquetReader implements TableReader {
  private final org.apache.parquet.hadoop.ParquetReader<Record> mReader;
  private final ParquetSchema mSchema;

  /**
   * @param reader the Parquet reader
   * @param schema the schema
   */
  private ParquetReader(org.apache.parquet.hadoop.ParquetReader<Record> reader, Schema schema) {
    mReader = reader;
    mSchema = new ParquetSchema(schema);
  }

  /**
   * Creates a parquet reader.
   *
   * @param uri the URI to the input
   * @return the reader
   * @throws IOException when failed to create the reader
   */
  public static ParquetReader create(AlluxioURI uri) throws IOException {
    Path inputPath = new Path(uri.getScheme(), uri.getAuthority().toString(), uri.getPath());
    Configuration conf = ReadWriterUtils.readNoCacheConf();
    InputFile inputFile = HadoopInputFile.fromPath(inputPath, conf);
    org.apache.parquet.hadoop.ParquetReader<Record> reader =
        AvroParquetReader.<Record>builder(inputFile)
            .disableCompatibility()
            .withDataModel(GenericData.get())
            .withConf(conf)
            .build();

    Schema schema;
    try (ParquetFileReader r = new ParquetFileReader(inputFile,
        ParquetReadOptions.builder().build())) {
      ParquetMetadata footer = r.getFooter();
      schema = new AvroSchemaConverter().convert(footer.getFileMetaData().getSchema());
    }

    return new ParquetReader(reader, schema);
  }

  @Override
  public TableSchema getSchema() throws IOException {
    return mSchema;
  }

  @Override
  public TableRow read() throws IOException {
    Record record = mReader.read();
    return record == null ? null : new ParquetRow(record);
  }

  @Override
  public void close() throws IOException {
    mReader.close();
  }
}
