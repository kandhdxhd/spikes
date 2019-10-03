DROP TRIGGER insert_parent_version_t ON parent;
DROP TRIGGER update_parent_version_t ON parent;
DROP TRIGGER insert_child_version_t ON child;
DROP TRIGGER update_child_version_t ON child;
DROP TRIGGER insert_child_parent_t ON child;
DROP TRIGGER update_child_parent_t ON child;
DROP TRIGGER delete_child_parent_t ON child;
DROP FUNCTION insert_parent_version_f();
DROP FUNCTION update_parent_version_f();
DROP FUNCTION insert_child_version_f();
DROP FUNCTION update_child_version_f();
DROP FUNCTION insert_child_parent_f();
DROP FUNCTION update_child_parent_f();
DROP FUNCTION delete_child_parent_f();
DROP TABLE child;
DROP TABLE parent;

CREATE TABLE parent
(
    id      SERIAL PRIMARY KEY,
    abc     VARCHAR NOT NULL UNIQUE,
    version INT
);

CREATE TABLE child
(
    id        SERIAL PRIMARY KEY,
    pqr       VARCHAR NOT NULL UNIQUE,
    parent_id INT REFERENCES parent (id),
    version   INT
);

CREATE OR REPLACE FUNCTION insert_parent_version_f()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$BODY$
BEGIN
    new.version := 1;
    RETURN new;
END;
$BODY$;

CREATE OR REPLACE FUNCTION update_parent_version_f()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$BODY$
BEGIN
    new.version := new.version + 1;
    RETURN new;
END;
$BODY$;

CREATE OR REPLACE FUNCTION insert_child_version_f()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$BODY$
BEGIN
    new.version := 1;
    IF (new.parent_id IS NOT NULL) THEN
        UPDATE parent
           SET version = version
         WHERE id = new.parent_id;
    END IF;
    RETURN new;
END;
$BODY$;

CREATE OR REPLACE FUNCTION update_child_version_f()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$BODY$
BEGIN
    new.version := new.version + 1;
    RETURN new;
END;
$BODY$;

CREATE OR REPLACE FUNCTION insert_child_parent_f()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$BODY$
BEGIN
    UPDATE parent
       SET version = version
     WHERE id = new.parent_id;
    RETURN new;
END;
$BODY$;

CREATE OR REPLACE FUNCTION update_child_parent_f()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$BODY$
BEGIN
    UPDATE parent
       SET version = version
     WHERE id IN (old.parent_id, new.parent_id);
    RETURN new;
END;
$BODY$;

CREATE OR REPLACE FUNCTION delete_child_parent_f()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$BODY$
BEGIN
    UPDATE parent
       SET version = version
     WHERE id = old.parent_id;
    RETURN old;
END;
$BODY$;

CREATE TRIGGER insert_parent_version_t
    BEFORE INSERT
    ON parent
    FOR EACH ROW
EXECUTE PROCEDURE insert_parent_version_f();

CREATE TRIGGER update_parent_version_t
    BEFORE UPDATE
    ON parent
    FOR EACH ROW
EXECUTE PROCEDURE update_parent_version_f();

CREATE TRIGGER insert_child_version_t
    BEFORE INSERT
    ON child
    FOR EACH ROW
EXECUTE PROCEDURE insert_child_version_f();

CREATE TRIGGER update_child_version_t
    BEFORE UPDATE
    ON child
    FOR EACH ROW
EXECUTE PROCEDURE update_child_version_f();

CREATE TRIGGER insert_child_parent_t
    AFTER INSERT
    ON child
    FOR EACH ROW
EXECUTE PROCEDURE insert_child_parent_f();

CREATE TRIGGER update_child_parent_t
    AFTER UPDATE
    ON child
    FOR EACH ROW
EXECUTE PROCEDURE update_child_parent_f();

CREATE TRIGGER delete_child_parent_t
    AFTER DELETE
    ON child
    FOR EACH ROW
EXECUTE PROCEDURE delete_child_parent_f();



INSERT INTO parent
    (abc)
VALUES
    ('a');

UPDATE parent
   SET abc = 'b'
 WHERE abc = 'a';

INSERT INTO child(pqr)
VALUES
    ('p');

UPDATE child
   SET parent_id = (SELECT id FROM parent WHERE abc = 'b')
 WHERE pqr = 'p';

DELETE
  FROM child
 WHERE pqr = 'p';



SELECT *
  FROM parent;

SELECT *
  FROM child;
