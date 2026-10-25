-- Colunas antigas ficam no H2 quando ddl-auto=update é usado.
-- Os valores padrão permitem novos cadastros sem manter campos sem uso nas entidades.
ALTER TABLE IF EXISTS material ALTER COLUMN IF EXISTS fixado SET DEFAULT FALSE;
ALTER TABLE IF EXISTS quiz_alternativa ALTER COLUMN IF EXISTS total_respostas SET DEFAULT 0;
