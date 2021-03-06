-- name: user-by-email
SELECT *
FROM users
WHERE email = :email

-- name: user-by-id
SELECT *
FROM users
WHERE id = :id

-- name: create-user<!
INSERT INTO users
(email, hashword) VALUES (:email, :hashword)

-- name: ring-by-id
SELECT *
FROM rings
WHERE id = :id

-- name: rings-by-owner
SELECT *
FROM rings
WHERE owner_id = :owner_id

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

-- name: site-by-url
SELECT *
FROM sites
WHERE
ring_id = :ring_id
AND url = :url

-- name: create-site<!
INSERT INTO sites
(ring_id, owner_id, name, url, approved) VALUES (:ring_id, :owner_id, :name, :url, :approved)

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
