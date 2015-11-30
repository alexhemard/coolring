CREATE TABLE rings (
  id          uuid PRIMARY KEY not null DEFAULT uuid_generate_v4(),
  name        character varying(512) NOT NULL,
  description character varying(512),
  created_at  timestamp without time zone DEFAULT now(),
  updated_at  timestamp without time zone DEFAULT now(),
  owner_id    uuid REFERENCES users (id)
);
