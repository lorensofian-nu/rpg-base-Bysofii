package com.rpg.dao;

import com.rpg.model.Heroe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para la entidad Heroe.
 * Encapsula todas las operaciones JDBC contra la tabla 'heroes'.
 * Reglas:
 * - Uso exclusivo de PreparedStatement con parametros '?' (prohibida la concatenacion SQL).
 * - Cierre garantizado de Connection, PreparedStatement y ResultSet con try-with-resources.
 * - executeQuery() para consultas y executeUpdate() para inserciones, actualizaciones y eliminaciones.
 */
public class HeroeDAO {

    /**
     * Inserta un nuevo heroe en la base de datos usando executeUpdate().
     * Recupera el ID autogenerado y lo asigna al objeto Heroe.
     * @return true si se inserto exitosamente, false si fallo
     */
    public boolean insertar(Heroe heroe) {
        String sql = "INSERT INTO heroes (nombre, tipo, vida_maxima, nivel) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, heroe.getNombre());
            ps.setString(2, heroe.getTipo());
            ps.setInt(3, heroe.getVidaMaxima());
            ps.setInt(4, heroe.getNivel());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        heroe.setId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.out.println("Error al insertar heroe en base de datos: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lista todos los heroes registrados usando executeQuery() y while(rs.next()).
     * @return lista de heroes encontrados
     */
    public List<Heroe> listar() {
        List<Heroe> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes ORDER BY id ASC";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearHeroe(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error al listar heroes: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Busca heroes filtrando por su tipo ('guerrero' o 'mago') con executeQuery() y '?'.
     * @return lista de heroes del tipo especificado
     */
    public List<Heroe> buscarPorTipo(String tipo) {
        List<Heroe> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes WHERE tipo = ? ORDER BY id ASC";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tipo.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearHeroe(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar heroes por tipo: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Busca un heroe por su clave primaria (id).
     * @return Heroe encontrado o null si no existe
     */
    public Heroe buscarPorId(int id) {
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearHeroe(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar heroe por id: " + e.getMessage());
        }
        return null;
    }

    /**
     * Actualiza el nivel de un heroe mediante executeUpdate().
     * @return cantidad de filas afectadas (0 si el id no existe)
     */
    public int actualizarNivel(int id, int nivel) {
        String sql = "UPDATE heroes SET nivel = ? WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, nivel);
            ps.setInt(2, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al actualizar nivel de heroe: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Elimina un heroe por su ID mediante executeUpdate().
     * @return cantidad de filas afectadas (0 si el id no existe)
     */
    public int eliminar(int id) {
        String sql = "DELETE FROM heroes WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al eliminar heroe: " + e.getMessage());
            return 0;
        }
    }

    private Heroe mapearHeroe(ResultSet rs) throws SQLException {
        return new Heroe(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("tipo"),
                rs.getInt("vida_maxima"),
                rs.getInt("nivel")
        );
    }
}
