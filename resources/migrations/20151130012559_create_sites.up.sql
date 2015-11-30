CREATE TABLE sites (
  id            uuid PRIMARY KEY not null DEFAULT uuid_generate_v4(),
  url           character varying(2048) NOT NULL,
  created_at    timestamp without time zone DEFAULT now(),
  updated_at    timestamp without time zone DEFAULT now(),
  ring_id       uuid REFERENCES rings (id),
  previous_site uuid NOT NULL REFERENCES sites (id),
  next_site     uuid NOT NULL REFERENCES sites (id)
);
