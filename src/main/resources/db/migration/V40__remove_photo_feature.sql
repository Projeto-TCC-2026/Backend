-- V40: remove a funcionalidade de fotos, que foi abandonada.

-- a) Tabela de fotos das execuções (criada na V10). Nenhum código a usava.

DROP TABLE IF EXISTS procedure_photos;

-- b) Linhas filhas dos campos do tipo PHOTO, apagadas antes dos campos para não violar FK:
DELETE FROM field_thresholds
WHERE field_id IN (SELECT id FROM doctor_procedure_fields WHERE data_type = 'PHOTO');

DELETE FROM checkin_field_values
WHERE field_id IN (SELECT id FROM doctor_procedure_fields WHERE data_type = 'PHOTO');

-- c) Campos configurados pelos médicos com o tipo PHOTO.
DELETE FROM doctor_procedure_fields
WHERE data_type = 'PHOTO';

-- d) Preset "Foto" inserido pela V24. Nenhuma tabela tem FK para field_type_presets.
DELETE FROM field_type_presets
WHERE data_type = 'PHOTO';

-- e) CHECK constraints: nenhuma migration cria CHECK listando 'PHOTO'

-- f) Coluna da URL da foto nas respostas de check-in (criada na V27).
ALTER TABLE checkin_field_values DROP COLUMN photo_url;
