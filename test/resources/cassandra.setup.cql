-- =============================================================================
-- Schema for unit tests
-- =============================================================================
CREATE KEYSPACE IF NOT EXISTS lcmap_changes_unit
WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1};

CREATE TABLE IF NOT EXISTS lcmap_changes_unit.algorithms(
    algorithm text PRIMARY KEY,
    inputs_url_template text,
    enabled boolean
);

CREATE TABLE IF NOT EXISTS lcmap_changes_unit.results(
    chip_x int,
    chip_y int,
    algorithm text,
    x int,
    y int,
    result text,
    result_md5 text,
    result_ok boolean,
    result_produced timestamp,
    chip_update_requested timestamp STATIC,
    inputs_url text STATIC,
    inputs_md5 text STATIC,
    PRIMARY KEY ((chip_x, chip_y, algorithm), x, y)
)
WITH COMPRESSION = { 'sstable_compression' : 'LZ4Compressor' }
AND  COMPACTION = { 'class' :  'LeveledCompactionStrategy' };
