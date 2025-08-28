-- Funciones y vistas materializadas para optimizaciones de BD
-- Fase 4.2 - Optimizaciones de BD (Parte 2)

-- Crear función para obtener estadísticas de uso
CREATE OR REPLACE FUNCTION get_file_stats_by_user(user_id UUID)
RETURNS TABLE(
    total_files BIGINT,
    total_size BIGINT,
    ready_files BIGINT,
    processing_files BIGINT,
    failed_files BIGINT,
    avg_file_size NUMERIC,
    most_common_type TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_files,
        COALESCE(SUM(size), 0) as total_size,
        COUNT(*) FILTER (WHERE status = 'READY') as ready_files,
        COUNT(*) FILTER (WHERE status = 'PROCESSING') as processing_files,
        COUNT(*) FILTER (WHERE status = 'FAILED') as failed_files,
        COALESCE(AVG(size), 0) as avg_file_size,
        (SELECT content_type FROM file_metadata 
         WHERE owner_id = user_id AND status = 'READY'
         GROUP BY content_type 
         ORDER BY COUNT(*) DESC 
         LIMIT 1) as most_common_type
    FROM file_metadata 
    WHERE owner_id = user_id;
END;
$$ LANGUAGE plpgsql;

-- Crear función para búsqueda optimizada
CREATE OR REPLACE FUNCTION search_files_optimized(
    user_id UUID, 
    search_term TEXT DEFAULT NULL,
    content_type_filter TEXT DEFAULT NULL,
    status_filter TEXT DEFAULT 'READY',
    limit_count INT DEFAULT 20,
    offset_count INT DEFAULT 0
)
RETURNS TABLE(
    id UUID,
    name VARCHAR(255),
    original_name VARCHAR(255),
    size BIGINT,
    content_type VARCHAR(100),
    created_at TIMESTAMP,
    rank REAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        fm.id,
        fm.name,
        fm.original_name,
        fm.size,
        fm.content_type,
        fm.created_at,
        CASE 
            WHEN search_term IS NOT NULL THEN
                ts_rank(
                    setweight(to_tsvector('english', fm.name), 'A') ||
                    setweight(to_tsvector('english', COALESCE(fm.description, '')), 'B'),
                    plainto_tsquery('english', search_term)
                )
            ELSE 1.0
        END as rank
    FROM file_metadata fm
    WHERE fm.owner_id = user_id
        AND fm.status = status_filter
        AND (content_type_filter IS NULL OR fm.content_type LIKE content_type_filter || '%')
        AND (
            search_term IS NULL OR
            (
                setweight(to_tsvector('english', fm.name), 'A') ||
                setweight(to_tsvector('english', COALESCE(fm.description, '')), 'B')
            ) @@ plainto_tsquery('english', search_term)
        )
    ORDER BY 
        CASE WHEN search_term IS NOT NULL THEN rank END DESC,
        fm.created_at DESC
    LIMIT limit_count
    OFFSET offset_count;
END;
$$ LANGUAGE plpgsql;

-- Crear vista materializada para estadísticas frecuentes
CREATE MATERIALIZED VIEW IF NOT EXISTS file_usage_stats AS
SELECT 
    owner_id,
    COUNT(*) as total_files,
    SUM(size) as total_size,
    COUNT(*) FILTER (WHERE status = 'READY') as ready_files,
    COUNT(*) FILTER (WHERE status = 'PROCESSING') as processing_files,
    COUNT(*) FILTER (WHERE status = 'FAILED') as failed_files,
    AVG(size) as avg_file_size,
    array_agg(DISTINCT content_type) as content_types,
    MAX(created_at) as last_upload,
    MIN(created_at) as first_upload
FROM file_metadata
GROUP BY owner_id;

-- Índice en la vista materializada
CREATE INDEX IF NOT EXISTS idx_file_usage_stats_owner ON file_usage_stats(owner_id);

-- Crear función para refrescar estadísticas
CREATE OR REPLACE FUNCTION refresh_file_stats() 
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY file_usage_stats;
END;
$$ LANGUAGE plpgsql;
