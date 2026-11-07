package com.rpg.model;

/**
 * Entidad de dominio POJO para el registro persistente de heroes en base de datos.
 * Aplica encapsulamiento estricto con validaciones en constructor y setters.
 */
public class Heroe {

    private int id;
    private String nombre;
    private String tipo;
    private int vidaMaxima;
    private int nivel;

    public Heroe(String nombre, String tipo, int vidaMaxima, int nivel) {
        this(0, nombre, tipo, vidaMaxima, nivel);
    }

    public Heroe(int id, String nombre, String tipo, int vidaMaxima, int nivel) {
        validarNombre(nombre);
        validarTipo(tipo);
        validarVidaMaxima(vidaMaxima);
        validarNivel(nivel);
        this.id = id;
        this.nombre = nombre.trim();
        this.tipo = tipo.trim().toLowerCase();
        this.vidaMaxima = vidaMaxima;
        this.nivel = nivel;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del heroe no puede estar vacio.");
        }
    }

    private static void validarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("El tipo de heroe no puede estar vacio.");
        }
        String t = tipo.trim().toLowerCase();
        if (!t.equals("guerrero") && !t.equals("mago")) {
            throw new IllegalArgumentException("Tipo invalido: " + tipo + ". Debe ser 'guerrero' o 'mago'.");
        }
    }

    private static void validarVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser mayor a cero.");
        }
    }

    private static void validarNivel(int nivel) {
        if (nivel < 1) {
            throw new IllegalArgumentException("El nivel debe ser mayor o igual a 1.");
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        validarNombre(nombre);
        this.nombre = nombre.trim();
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        validarTipo(tipo);
        this.tipo = tipo.trim().toLowerCase();
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    public void setVidaMaxima(int vidaMaxima) {
        validarVidaMaxima(vidaMaxima);
        this.vidaMaxima = vidaMaxima;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        validarNivel(nivel);
        this.nivel = nivel;
    }

    @Override
    public String toString() {
        return String.format("[ID: %d] %-14s | Tipo: %-8s | HP: %3d | Nivel: %2d",
                id, nombre, tipo.toUpperCase(), vidaMaxima, nivel);
    }
}
