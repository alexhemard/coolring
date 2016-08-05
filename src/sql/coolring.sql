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

-- name: create-ring
INSERT INTO rings (name)
VALUES (:name, :description, :owner_id)

-- name: site-by-url
SELECT *
FROM sites
WHERE
ring_id = :ring_id
AND
url = :url

-- name: create-site
WITH last_site AS (
SELECT id
FROM sites

INSERT INTO sites
(name) VALUES (:ring_id, :url, :owner_id)



