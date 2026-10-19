-- V39: Ajuste das faixas NORMAIS de SPO2 e TEMPERATURE.
--
-- As faixas clínicas foram revisadas conforme a seção 2.6.2 do TCC. Só o limite
-- inferior da faixa normal muda nos dois tipos:
--
--   SPO2         normal_min 90   -> 91   (normal_max continua nulo)
--      91 a 94 é hipoxemia leve e não gera alerta. 85 a 90 é hipoxemia
--      moderada e passa a gerar alerta após a confirmação por duas leituras.
--      84 ou menos continua grave (severe_min da V36).
--
--   TEMPERATURE  normal_min 35.5 -> 36.2 (normal_max continua 38.0)
--      Faixa normal de 36,2 a 38,0 conforme a referência do TCC. 37,5 a 38,0 é
--      febre baixa e não aciona o médico, por isso o máximo não muda.
--
-- Nada mais é alterado: faixas graves (V36), faixas plausíveis (V35), HEART_RATE,
-- severity e as regras de alerta (confirmação por duas leituras, pergunta à
-- paciente no caso grave, janela de 4h e filtro de valor impossível) seguem como
-- estão. O AlertServiceImpl lê a faixa do banco a cada leitura, então o novo
-- limite vale a partir da primeira leitura avaliada depois desta migration.
--
-- O UPDATE casa por reading_type, que tem UNIQUE — mesmo critério da V35 e da V36.
-- SQL padrão, aceito pelo PostgreSQL e pelo H2.

UPDATE reading_thresholds SET normal_min = 91   WHERE reading_type = 'SPO2';
UPDATE reading_thresholds SET normal_min = 36.2 WHERE reading_type = 'TEMPERATURE';

-- O label entra no título do alerta (AlertServiceImpl.buildTitle). Sem esta
-- atualização, o título continuaria citando os limites antigos da V26.
UPDATE reading_thresholds SET label = 'Saturação de oxigênio normal a partir de 91 por cento' WHERE reading_type = 'SPO2';
UPDATE reading_thresholds SET label = 'Temperatura corporal normal entre 36,2 e 38,0 graus Celsius' WHERE reading_type = 'TEMPERATURE';
