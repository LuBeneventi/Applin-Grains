-- Script de inicialización para crear las bases de datos de Catálogo, Órdenes, Auditoría y Reportes
SELECT 'CREATE DATABASE pedidos360_catalog'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pedidos360_catalog')\gexec

SELECT 'CREATE DATABASE pedidos360_orders'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pedidos360_orders')\gexec

SELECT 'CREATE DATABASE pedidos360_audit'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pedidos360_audit')\gexec

SELECT 'CREATE DATABASE pedidos360_report'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pedidos360_report')\gexec
