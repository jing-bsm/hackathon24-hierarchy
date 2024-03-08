CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TABLE if not exists definition (
  id SERIAL PRIMARY KEY, -- auto generated integer column
  hierarchy_id BIGINT NOT NULL,
  version INTEGER NOT NULL,
  effective_from TIMESTAMP NOT NULL,
  client_id BIGINT NOT NULL,
  schema TEXT NOT NULL,
  create_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- default value for creation timestamp
);

CREATE INDEX idx_hier_eff ON definition USING btree (hierarchy_id, effective_from, version DESC);

CREATE TABLE if not exists node (
  iid SERIAL PRIMARY KEY,
  definition_id INTEGER NOT NULL REFERENCES definition (id), -- foreign key
  id VARCHAR(20) NOT NULL,
--  level VARCHAR(20) NOT NULL,
  depth INTEGER NOT NULL,
--  name VARCHAR(100) NOT NULL,
  parent_id VARCHAR(20) NOT NULL,
  leaf BOOLEAN NOT NULL,
  tree ltree,
  ext_data JSONB, -- binary JSON column
  create_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- default value for creation timestamp
  CONSTRAINT unique_id UNIQUE (definition_id, id)
);

CREATE INDEX idx_def_cid ON node (definition_id, id, tree);
CREATE INDEX idx_def_ext ON node using gin (ext_data, (ext_data->'level'));
-- CREATE INDEX idx_def_tree ON node USING gist (definition_id, tree);
-- CREATE INDEX idx_def_ext ON node USING gin (definition_id, ext_data);

-- Ancestors
SELECT * FROM node
WHERE tree @> (select tree from node where c_id = '2');

-- Descendants
SELECT * FROM node
WHERE tree <@ (select tree from node where c_id = 'R_2');

---

CREATE OR REPLACE FUNCTION get_definition_id(hierarchyId INTEGER, effectiveFrom TIMESTAMP)
RETURNS INTEGER AS $$
DECLARE
    result_id INTEGER;
BEGIN
    SELECT id
    INTO result_id
    FROM definition
    WHERE hierarchy_id = hierarchyId
        AND effective_from <= effectiveFrom
    ORDER BY version
    LIMIT 1;

    RETURN result_id;
END;
$$ LANGUAGE plpgsql;
---
WITH def_q (id) AS (
    SELECT id
    FROM definition
    WHERE hierarchy_id = :hierarchyId
    AND effective_from <= :effectiveFrom
    ORDER BY version
    LIMIT 1
)
SELECT * FROM node
WHERE definition_id = (
    select definition_id from def_q
)
AND tree @> (
    select tree from node
    where definition_id=(
        select definition_id from def_q
    ) AND c_id=:cId
);

SELECT *
FROM your_table
WHERE your_jsonb_column->>'kkk' IN ('100', '200', '300');
----

CREATE TABLE if not exists user_auth (
  id BIGINT UNIQUE NOT NULL,
  definition_id INTEGER NOT NULL REFERENCES definition (id), -- foreign key
  scope SMALLINT NOT NULL, -- todo. all, none, specific
  cascade JSONB, -- array of the top node ids
  explicit JSONB, -- array of nodes ids that not in or under cascade
  entire JSONB, -- array of all nodes user has permission
  update_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- default value for creation timestamp
);

CREATE INDEX idx_user_scope ON user_auth USING btree (id, scope);
CREATE INDEX idx_entire ON user_auth USING gin (entire jsonb_ops, explicit jsonb_ops) WHERE entire IS NOT NULL;

