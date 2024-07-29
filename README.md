# Data Retriever

## Usage:
 - Where to store the data source connection info?
 - How to create and test connection?
 - How to detect the data source structure?
 - Execute data (with batch or not)

## Components:

### Base
 - DwConnection (_Data Warehouse connection_)
 - DwDataSource (_Data Warehouse data source_)

### Interfaces
 - DatabaseDataSource (_Mariadb, MySql, Oracle, Postgres, MsSql_, ...)
 - ApiDataSource (_Rest, Soap, SpecApp, GraphQL, Sockets_, ...)
 - FileStoreDataSource (_Csv, Excel, Json, XML, Raw_, ...)
---
 - StructuredDataSource
 - SemiStructuredDataSource
 - UnStructuredDataSource