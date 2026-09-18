-- PET.birth_date is Not Null (docs/entity_model.md, GR-007). The column was
-- nullable while the entity model still called the date optional; the form has
-- always required it and every seeded pet has one.
ALTER TABLE pets ALTER COLUMN birth_date SET NOT NULL;
