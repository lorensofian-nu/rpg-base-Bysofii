-- ========================================================
-- Simulador de Batalla RPG - Esquema de Base de Datos
-- Unidad 5.2: Registro de Heroes persistente con JDBC y MVC
-- Base de datos: rpg_grupal_db
-- ========================================================

CREATE DATABASE IF NOT EXISTS rpg_grupal_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE rpg_grupal_db;

-- Tabla de heroes
CREATE TABLE IF NOT EXISTS heroes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    tipo ENUM('guerrero', 'mago') NOT NULL,
    vida_maxima INT NOT NULL CHECK (vida_maxima > 0),
    nivel INT NOT NULL DEFAULT 1 CHECK (nivel >= 1),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Datos iniciales de ejemplo
INSERT INTO heroes (nombre, tipo, vida_maxima, nivel) VALUES
('Arthur', 'guerrero', 120, 1),
('Merlin', 'mago', 80, 1)
ON DUPLICATE KEY UPDATE nombre = nombre;
