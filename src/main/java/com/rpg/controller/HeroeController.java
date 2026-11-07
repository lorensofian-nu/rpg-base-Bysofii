package com.rpg.controller;

import com.rpg.dao.HeroeDAO;
import com.rpg.model.Heroe;

import java.util.Collections;
import java.util.List;

/**
 * Controlador de la arquitectura MVC para la gestion de heroes.
 * Valida reglas de negocio, coordina con HeroeDAO y retorna mensajes descriptivos a la Vista.
 * No contiene codigo de acceso a datos JDBC (JDBC prohibido en el controlador).
 */
public class HeroeController {

    private final HeroeDAO heroeDAO;

    public HeroeController() {
        this(new HeroeDAO());
    }

    public HeroeController(HeroeDAO heroeDAO) {
        this.heroeDAO = heroeDAO;
    }

    /**
     * Valida y registra un nuevo heroe en el sistema.
     */
    public String registrarHeroe(String nombre, String tipo, int vidaMaxima, int nivel) {
        if (nombre == null || nombre.isBlank()) {
            return "Error de validacion: el nombre no puede estar vacio.";
        }
        if (tipo == null || tipo.isBlank()) {
            return "Error de validacion: debes especificar un tipo de heroe.";
        }
        String tipoNorm = tipo.trim().toLowerCase();
        if (!tipoNorm.equals("guerrero") && !tipoNorm.equals("mago")) {
            return "Error de validacion: tipo invalido ('" + tipo + "'). Debe ser 'guerrero' o 'mago'.";
        }
        if (vidaMaxima <= 0) {
            return "Error de validacion: la vida maxima debe ser mayor a cero.";
        }
        if (nivel < 1) {
            return "Error de validacion: el nivel debe ser mayor o igual a 1.";
        }

        try {
            Heroe heroe = new Heroe(nombre, tipoNorm, vidaMaxima, nivel);
            boolean insertado = heroeDAO.insertar(heroe);
            if (insertado) {
                return String.format("Heroe '%s' registrado exitosamente con ID %d.", heroe.getNombre(), heroe.getId());
            } else {
                return "No se pudo registrar el heroe. Es posible que el nombre ya este registrado o la base de datos no este disponible.";
            }
        } catch (IllegalArgumentException e) {
            return "Error en datos del heroe: " + e.getMessage();
        }
    }

    /**
     * Obtiene el listado completo de heroes registrados.
     */
    public List<Heroe> listarHeroes() {
        return heroeDAO.listar();
    }

    /**
     * Filtra los heroes por su clase o tipo.
     */
    public List<Heroe> buscarHeroesPorTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return Collections.emptyList();
        }
        String tipoNorm = tipo.trim().toLowerCase();
        if (!tipoNorm.equals("guerrero") && !tipoNorm.equals("mago")) {
            return Collections.emptyList();
        }
        return heroeDAO.buscarPorTipo(tipoNorm);
    }

    /**
     * Busca un heroe por su clave primaria.
     */
    public Heroe buscarHeroePorId(int id) {
        if (id <= 0) {
            return null;
        }
        return heroeDAO.buscarPorId(id);
    }

    /**
     * Actualiza el nivel de un heroe existente verificando que el ID exista.
     */
    public String actualizarNivel(int id, int nuevoNivel) {
        if (id <= 0) {
            return "Error de validacion: el ID debe ser positivo.";
        }
        if (nuevoNivel < 1) {
            return "Error de validacion: el nivel debe ser mayor o igual a 1.";
        }

        int filasAfectadas = heroeDAO.actualizarNivel(id, nuevoNivel);
        if (filasAfectadas > 0) {
            return String.format("Nivel del heroe ID %d actualizado a %d exitosamente.", id, nuevoNivel);
        } else {
            return "No existe un heroe con ese id (" + id + ").";
        }
    }

    /**
     * Elimina un heroe por su ID reportando si no fue encontrado.
     */
    public String eliminarHeroe(int id) {
        if (id <= 0) {
            return "Error de validacion: el ID debe ser positivo.";
        }

        int filasAfectadas = heroeDAO.eliminar(id);
        if (filasAfectadas > 0) {
            return String.format("Heroe con ID %d eliminado exitosamente.", id);
        } else {
            return "No existe un heroe con ese id (" + id + ").";
        }
    }
}
