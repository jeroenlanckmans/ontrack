-- Simplifying the entity data value (#557)

UPDATE ENTITY_DATA
SET JSON_VALUE = VALUE::JSONB
WHERE VALUE IS NOT NULL
      AND JSON_VALUE IS NULL;

ALTER TABLE ENTITY_DATA
  DROP COLUMN VALUE;