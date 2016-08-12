CREATE SEQUENCE sites_id_seq;

CREATE TABLE sites (
  id            int8 PRIMARY KEY DEFAULT nextval('sites_id_seq'::regclass),
  ring_id       int8 NOT NULL REFERENCES rings (id),
  owner_id      int8 NOT NULL REFERENCES users (id),  
  name          character varying(2048) NOT NULL,  
  url           character varying(2048) NOT NULL,
  approved      boolean DEFAULT false,
  active        boolean DEFAULT true,    
  next_site     int8 UNIQUE DEFERRABLE INITIALLY DEFERRED
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
    IF (((TG_OP='INSERT')
          AND NEW.approved is true) OR
        ((TG_OP='UPDATE')
          AND (NEW.approved is true
               AND OLD.approved is false))) THEN
      RAISE NOTICE 'activating site(%)', NEW.id;            
      SELECT *
      FROM sites
      WHERE approved = true
      AND next_site is not null
      ORDER BY updated_at desc
      LIMIT 1 INTO last_site;
      IF NOT FOUND THEN
        NEW.next_site = NEW.id;
      ELSIF NEW.id = last_site.id THEN
        NEW.next_site = last_site.id;
      ELSE
        UPDATE sites set next_site = NEW.id where id = last_site.id;
        NEW.next_site = last_site.next_site;
      END IF;
      RAISE NOTICE 'next site is now(%)', NEW.next_site;                  
      NEW.active = true;
      RETURN NEW; 
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
      IF ((NEW.approved     = false
           OR NEW.active    = false)
           AND NEW.next_site is not null) THEN
        RAISE NOTICE 'deactivating site(%)', NEW.id;  
        SELECT * FROM sites WHERE id = NEW.next_site INTO next_s;
        SELECT * FROM sites WHERE next_site = NEW.id INTO previous_s;
        
        IF previous_s.id != NEW.id THEN
          UPDATE sites set next_site = next_s.id where id = previous_s.id;
        ELSE
          UPDATE sites set next_site = previous_s.id where id = previous_s.id;
        END IF;

        NEW.approved  = false;      
        NEW.active    = false;          
        NEW.next_site = null;        
      END IF;
      
      RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
      SELECT * FROM sites WHERE id = OLD.next_site INTO next_s;
      SELECT * FROM sites WHERE next_site = OLD.id INTO previous_s;

      IF previous_s.id != OLD.id THEN   
        UPDATE sites set next_site = next_s.id where id = previous_s.id;
      END IF;
      
      RETURN OLD;
    END IF;
  END IF;  
END;
$$ language plpgsql;

CREATE OR REPLACE FUNCTION update_timestamp()	
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;	
END;
$$ language 'plpgsql';

CREATE TRIGGER _00_deactivate_site_trg
    BEFORE UPDATE OR DELETE
    ON sites
    FOR EACH ROW
    EXECUTE PROCEDURE deactivate_site();
    
CREATE TRIGGER _01_approval_trg
    BEFORE INSERT OR UPDATE
    ON sites
    FOR EACH ROW
    EXECUTE PROCEDURE approve_site();

CREATE TRIGGER _99_update_timestamp_trg
    BEFORE UPDATE
    ON sites
    FOR EACH ROW
    EXECUTE PROCEDURE update_timestamp();
