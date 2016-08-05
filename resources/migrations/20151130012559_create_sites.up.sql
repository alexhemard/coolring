CREATE TABLE sites (
  id            uuid PRIMARY KEY not null DEFAULT uuid_generate_v4(),
  ring_id       uuid REFERENCES rings (id),
  url           character varying(2048) NOT NULL,
  next_site     uuid NOT NULL REFERENCES sites (id),
  created_at    timestamp without time zone DEFAULT now(),
  updated_at    timestamp without time zone DEFAULT now()
);
