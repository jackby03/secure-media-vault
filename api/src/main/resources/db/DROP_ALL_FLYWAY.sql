-- Script para hacer DROP completo de todas las tablas relacionadas con Flyway
-- Ejecutar manualmente en tu base de datos PostgreSQL

-- 1. Drop tabla de usuarios si existe
DROP TABLE IF EXISTS users CASCADE;

-- 2. Drop tabla de historial de Flyway
DROP TABLE IF EXISTS flyway_schema_history CASCADE;

-- 3. Drop todos los índices relacionados
DROP INDEX IF EXISTS idx_users_email;
DROP INDEX IF EXISTS idx_users_role;

-- 4. Verificar que todo se eliminó
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';
