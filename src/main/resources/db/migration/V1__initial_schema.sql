-- Initial schema for AIUP PetClinic
-- Matches docs/entity_model.md.

CREATE SEQUENCE owners_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE pets_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE types_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE visits_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE vets_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE specialties_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE types (
    id   INTEGER      PRIMARY KEY DEFAULT nextval('types_seq'),
    name VARCHAR(80)  NOT NULL UNIQUE
);

CREATE TABLE owners (
    id         INTEGER      PRIMARY KEY DEFAULT nextval('owners_seq'),
    first_name VARCHAR(30)  NOT NULL,
    last_name  VARCHAR(30)  NOT NULL,
    address    VARCHAR(255) NOT NULL,
    city       VARCHAR(80)  NOT NULL,
    telephone  VARCHAR(20)  NOT NULL
);

CREATE INDEX owners_last_name_idx ON owners (last_name);

CREATE TABLE pets (
    id         INTEGER     PRIMARY KEY DEFAULT nextval('pets_seq'),
    name       VARCHAR(30) NOT NULL,
    birth_date DATE,
    type_id    INTEGER     NOT NULL REFERENCES types (id),
    owner_id   INTEGER     NOT NULL REFERENCES owners (id),
    CONSTRAINT pets_owner_name_unique UNIQUE (owner_id, name)
);

CREATE INDEX pets_owner_id_idx ON pets (owner_id);

CREATE TABLE visits (
    id          INTEGER      PRIMARY KEY DEFAULT nextval('visits_seq'),
    visit_date  DATE         NOT NULL,
    description VARCHAR(255) NOT NULL,
    pet_id      INTEGER      NOT NULL REFERENCES pets (id)
);

CREATE INDEX visits_pet_id_idx ON visits (pet_id);

CREATE TABLE vets (
    id         INTEGER     PRIMARY KEY DEFAULT nextval('vets_seq'),
    first_name VARCHAR(30) NOT NULL,
    last_name  VARCHAR(30) NOT NULL
);

CREATE TABLE specialties (
    id   INTEGER     PRIMARY KEY DEFAULT nextval('specialties_seq'),
    name VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE vet_specialties (
    vet_id       INTEGER NOT NULL REFERENCES vets (id),
    specialty_id INTEGER NOT NULL REFERENCES specialties (id),
    PRIMARY KEY (vet_id, specialty_id)
);
