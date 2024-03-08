CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TABLE if not exists definition (
  id BIGSERIAL PRIMARY KEY, -- auto generated integer column
  hierarchy_id BIGINT NOT NULL,
  version INTEGER NOT NULL,
  effective_from TIMESTAMP NOT NULL,
  client_id BIGINT NOT NULL,
  schema TEXT NOT NULL,
  create_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- default value for creation timestamp
);

CREATE INDEX idx_hier_eff ON definition USING btree (hierarchy_id, version DESC);

CREATE TABLE if not exists node (
  iid BIGSERIAL PRIMARY KEY,
  definition_id BIGINT NOT NULL REFERENCES definition (id), -- foreign key
  id VARCHAR(20) NOT NULL,
  level VARCHAR(20),
  depth INTEGER NOT NULL,
  name VARCHAR(100),
  parent_id VARCHAR(20) NOT NULL,
  leaf BOOLEAN NOT NULL,
  tree ltree,
  ext_data JSONB, -- binary JSON column
  create_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- default value for creation timestamp
  CONSTRAINT unique_id UNIQUE (definition_id, id)
);

CREATE INDEX idx_def_pid ON node USING btree (definition_id, leaf);
CREATE INDEX idx_jsonb ON node USING gin (ext_data jsonb_ops) WHERE ext_data is not null;
CREATE INDEX idx_tree ON node USING gist (tree);

CREATE TABLE if not exists user_auth (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  hierarchy_id BIGINT NOT NULL,
  definition_id BIGINT NOT NULL REFERENCES definition (id), -- foreign key
  scope SMALLINT NOT NULL, -- todo. all, none, specific
  cascade JSONB, -- array of the top node ids
  explicit JSONB, -- array of nodes ids that not in or under cascade
  entire JSONB, -- array of all nodes user has permission
  update_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- default value for creation timestamp
  CONSTRAINT unique_user UNIQUE (user_id, definition_id)
);

CREATE INDEX idx_user_scope ON user_auth USING btree (user_id, hierarchy_id, definition_id desc);
CREATE INDEX idx_entire ON user_auth USING gin (entire jsonb_ops, explicit jsonb_ops) WHERE entire IS NOT NULL;

