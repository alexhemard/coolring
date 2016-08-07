CREATE SEQUENCE sites_id_seq;

CREATE TABLE sites (
  id            int8 PRIMARY KEY DEFAULT nextval('sites_id_seq'::regclass),
  ring_id       int8 NOT NULL REFERENCES rings (id),
  owner_id      int8 NOT NULL REFERENCES users (id),  
  name          character varying(2048) NOT NULL,  
  url           character varying(2048) NOT NULL,
  approved      boolean DEFAULT false,
  active        boolean DEFAULT true,    
  next_site     int8 UNIQUE DEFERRABLE
                     REFERENCES sites (id) DEFERRABLE INITIALLY DEFERRED,
  created_at    timestamp without time zone DEFAULT now(),
  updated_at    timestamp without time zone DEFAULT now()
);

CREATE UNIQUE INDEX index_ring_on_url on sites USING btree (url);

CREATE OR REPLACE FUNCTION approve_site() RETURNS trigger AS $$
DECLARE
  last_site sites%ROWTYPE;
BEGIN
  IF TG_TABLE_NAME = 'sites' THEN
    IF NEW.approved     = true 
       AND OLD.approved = false THEN
      SELECT *
      FROM sites
      WHERE approved = true
      ORDER BY created_at desc
      LIMIT 1 INTO last_site;
      IF NOT FOUND THEN
        NEW.next_site = NEW.id;
      ELSE
        UPDATE sites set next_site = NEW.id where id = last_site.id;
        NEW.next_site = last_site.next_site;
      END IF;
      NEW.active = true;
    END IF;
  END IF;  
    
  RETURN NEW;
END;
$$ language plpgsql;

CREATE OR REPLACE FUNCTION deactivate_site() RETURNS trigger AS $$
DECLARE
  previous_s sites%ROWTYPE;
  next_s sites%ROWTYPE;  
BEGIN
  IF TG_TABLE_NAME = 'sites' THEN
    IF (TG_OP = 'UPDATE') THEN
      IF (NEW.approved     = false
          OR NEW.active    = false
          OR NEW.next_site = null) THEN
        SELECT * FROM sites WHERE id = NEW.next_site INTO next_s;
        SELECT * FROM sites WHERE next_site = NEW.id INTO previous_s;      
        UPDATE sites set next_site = next_s.id where id = previous_s.id;

        NEW.approved = false;      
        NEW.active   = false;          
        NEW.next_site = null;                
      END IF;
      
      RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
      SELECT * FROM sites WHERE id = OLD.next_site INTO next_s;
      SELECT * FROM sites WHERE next_site = OLD.id INTO previous_s;      
      UPDATE sites set next_site = next_s.id where id = previous_s.id;

      RETURN OLD;
    END IF;
  END IF;  
END;
$$ language plpgsql;

CREATE TRIGGER approval_trg
    BEFORE INSERT OR UPDATE
    ON sites
    FOR EACH ROW
    EXECUTE PROCEDURE approve_site();

CREATE TRIGGER deactivate_site_trg
    BEFORE UPDATE OR DELETE
    ON sites
    FOR EACH ROW
    EXECUTE PROCEDURE deactivate_site();
