CREATE SEQUENCE users_id_seq;

CREATE TABLE users (
  id         int8 PRIMARY KEY DEFAULT nextval('users_id_seq'::regclass),
  name       character varying(255),
  email      character varying(255) NOT NULL,
  hashword   character varying(1024),
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now()
);

CREATE UNIQUE INDEX index_users_on_email on users USING btree (email);
