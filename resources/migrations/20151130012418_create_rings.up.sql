CREATE SEQUENCE rings_id_seq;

CREATE TABLE rings (
  id          int8 PRIMARY KEY DEFAULT nextval('rings_id_seq'::regclass),
  name        character varying(512) NOT NULL,
  description character varying(2048),
  created_at  timestamp without time zone DEFAULT now(),
  updated_at  timestamp without time zone DEFAULT now(),
  owner_id    int8 REFERENCES users (id) NOT NULL
);
