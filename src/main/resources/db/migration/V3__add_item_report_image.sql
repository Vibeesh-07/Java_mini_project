-- Flyway Migration V3: Add optional image_data column to item_reports
ALTER TABLE item_reports
ADD COLUMN image_data MEDIUMTEXT NULL;
