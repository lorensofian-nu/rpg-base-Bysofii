package com.rpg.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Demostracion didactica de Inyeccion SQL (Unidad 5.2):
 * Compara el uso inseguro de Statement mediante concatenacion directa de Strings
 * frente al uso seguro y parametrizado de PreparedStatement.
 */
public class LoginInseguroDemo {

    /**
     * VULNERABLE: Concatenar entradas de usuario directamente en la sentencia SQL.
     * Ejemplo de ataque en entrada: ' OR '1'='1
     * La consulta se transformaria en: SELECT * FROM heroes WHERE nombre = '' OR '1'='1'
     * burlando la autenticacion o devolviendo registros no autorizados.
     */
    public static boolean autenticarInseguro(String nombre) {
        String sql = "SELECT * FROM heroes WHERE nombre = '" + nombre + "'";
        try (Connection conn = ConexionDB.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        } catch (SQLException e) {
            System.out.println("Error en consulta insegura: " + e.getMessage());
            return false;
        }
    }

    /**
     * SEGURO: Uso de PreparedStatement con parametro posicional '?'.
     * El driver de base de datos precompila la consulta y trata la entrada
     * estrictamente como un valor literal de dato, neutralizando caracteres de control SQL.
     */
    public static boolean autenticarSeguro(String nombre) {
        String sql = "SELECT * FROM heroes WHERE nombre = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Error en consulta segura: " + e.getMessage());
            return false;
        }
    }
}
