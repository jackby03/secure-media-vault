-- Optimizaciones de índices básicas para consultas frecuentes
-- Fase 4.2 - Optimizaciones de BD (versión simplificada)

-- Solo índices esenciales para evitar timeouts
-- Índice principal para consultas por usuario y estado
CREATE INDEX IF NOT EXISTS idx_file_metadata_owner_status 
    ON file_metadata(owner_id, status);

-- Índice para ordenamiento por fecha
CREATE INDEX IF NOT EXISTS idx_file_metadata_owner_created 
    ON file_metadata(owner_id, created_at DESC);

-- Índice para búsquedas por tipo de contenido
CREATE INDEX IF NOT EXISTS idx_file_metadata_content_type 
    ON file_metadata(content_type, status);

-- Índice parcial solo para archivos READY (los más consultados)
CREATE INDEX IF NOT EXISTS idx_file_metadata_ready_files 
    ON file_metadata(owner_id, created_at DESC) 
    WHERE status = 'READY';
