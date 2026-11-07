package com.rpg.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor de conexion JDBC a MySQL para el Simulador RPG.
 * Lee las credenciales y la URL de conexion exclusivamente desde 'config.txt'.
 *
 * EXPLICACION DE LA URL JDBC (ejemplo: jdbc:mysql://localhost:3306/rpg_grupal_db):
 * - 'jdbc': Protocolo principal de Java para conexion a bases de datos relacionales.
 * - 'mysql': Subprotocolo que identifica el controlador (driver) especifico de MySQL.
 * - 'localhost': Host o direccion IP de la maquina donde se ejecuta el servidor de base de datos.
 * - '3306': Puerto TCP de red por el que MySQL escucha conexiones entrantes.
 * - 'rpg_grupal_db': Nombre del esquema o base de datos a la que nos conectamos.
 */
public class ConexionDB {

    private static final String RUTA_CONFIG = "config.txt";

    private ConexionDB() {
        // Constructor privado para clase utilitaria
    }

    /**
     * Lee el archivo config.txt con try-with-resources y retorna una nueva Connection a MySQL.
     * @return Connection abierta a la base de datos
     * @throws SQLException si ocurre un error de conexion JDBC
     */
    public static Connection obtenerConexion() throws SQLException {
        Map<String, String> config = leerConfiguracion();
        if (config == null) {
            throw new SQLException("No se pudo cargar la configuracion desde " + RUTA_CONFIG);
        }

        String url = config.get("url");
        String usuario = config.get("usuario");
        String clave = config.get("clave");

        if (url == null || usuario == null || clave == null) {
            System.out.println("Configuracion incompleta en " + RUTA_CONFIG + ". Se requieren url, usuario y clave.");
            throw new SQLException("Faltan propiedades obligatorias en " + RUTA_CONFIG);
        }

        return DriverManager.getConnection(url, usuario, clave);
    }

    /**
     * Lee config.txt linea por linea con BufferedReader ignorando lineas vacias
     * y separando por clave=valor.
     */
    private static Map<String, String> leerConfiguracion() {
        File archivo = new File(RUTA_CONFIG);
        if (!archivo.exists()) {
            System.out.println("Archivo de configuracion no encontrado: " + archivo.getAbsolutePath());
            System.out.println("Crea un archivo " + RUTA_CONFIG + " en la raiz basado en config.example.txt.");
            return null;
        }

        Map<String, String> propiedades = new HashMap<>();
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }
                String[] partes = linea.split("=", 2);
                if (partes.length == 2) {
                    propiedades.put(partes[0].trim(), partes[1].trim());
                }
            }
            return propiedades;
        } catch (FileNotFoundException e) {
            System.out.println("No se encontro el archivo de configuracion: " + e.getMessage());
            return null;
        } catch (IOException e) {
            System.out.println("Error de lectura en archivo de configuracion: " + e.getMessage());
            return null;
        }
    }

    /**
     * Prueba si la base de datos es accesible actualmente.
     */
    public static boolean probarConexion() {
        try (Connection conn = obtenerConexion()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.out.println("Aviso: No se pudo conectar a la base de datos MySQL (" + e.getMessage() + ").");
            return false;
        }
    }
}
