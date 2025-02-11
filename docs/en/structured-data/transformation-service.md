---
layout: global
title: Alluxio Transformation Service
nickname: Transformation Service
group: Structured Data
priority: 1
---

* Table of Contents
{:toc}

## Data Transformations

With job service and catalog service, Alluxio can transform a table to a new table at partition level.
The data of the original table is not modified, and the data of the new table is persisted in a new location managed by Alluxio.
Once the transformation is done, Presto users can transparently query against the new data.

There are two kinds of supported transformations:

1. coalesce the files to a specified number of files.
2. convert CSV files to Parquet files.

The transformed data is always in Parquet format.

Before running a transformation, you should first attach a database.
The following command attaches the "default" database in Hive to Alluxio.

```console
$ ${ALLUXIO_HOME}/bin/alluxio table attachdb hive thrift://localhost:9083 default
```

The following command coalesce files under each partition of table "test" to one file.

```console
$ ${ALLUXIO_HOME}/bin/alluxio table transform default test -d "write(hive).option(hive.num.files, 1)"
```

After running the above command, you'll see output like:

```console
Started transformation job with job ID 1572296710137, you can monitor the status of the job with './bin/alluxio table transformStatus 1572296710137'.
```

Now follow the instruction in the output to monitor status of the transformation:

```console
$ ${ALLUXIO_HOME}/bin/alluxio table transformStatus 1572296710137
```

It will show the status of the transformation job:

```console
database: default
table: test
transformation: write(hive).option(hive.num.files, 1)
job ID: 1572296710137
job status: COMPLETED
```

Since the transformation has completed, you can run your Presto queries on the transformed table transparently.

You can find out the location of the transformed data with the following Presto query:

```console
presto:default> select "$path" from test;
```

You should see output like:

```console
alluxio://localhost:19998/catalog/default/tables/test/_internal_/part=0/20191024-213102-905-R29wf/part-0.parquet
```
