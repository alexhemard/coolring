CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
  id         uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
  name       character varying(255),
  email      character varying(255) NOT NULL,
  hashword   character varying(1024),
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now()
);

CREATE UNIQUE INDEX index_users_on_email on users USING btree (email);
