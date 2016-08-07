-- name: user-by-email
SELECT *
FROM users
WHERE email = :email

-- name: ring-by-id
SELECT *
FROM rings
WHERE id = :id

-- name: rings
SELECT *
FROM rings

-- name: approved-sites-for-ring
SELECT *
FROM sites
WHERE ring_id = :id
AND approved = true

-- name: create-ring<!
INSERT INTO rings (name, description, owner_id)
VALUES (:name, :description, :owner_id)

-- name: site-by-id
SELECT *
FROM sites
WHERE
id = :id

-- name: create-site<!
INSERT INTO sites
(ring_id, name, url) VALUES (:ring_id, :name, :url)

-- name: approve-site!
UPDATE sites
SET approved = true
FROM rings
WHERE id = :id
AND r.owner_id = :owner_id

-- name: deactivate-site!
UPDATE sites
SET approved = false, active = false
FROM rings
WHERE id = :id
AND r.owner_id = :owner_id
