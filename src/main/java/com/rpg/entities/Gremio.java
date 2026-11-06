package com.rpg.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agregacion: el gremio guarda referencias a personajes que existen por su cuenta.
 * Implementa Serializable para persistir la comunidad de heroes.
 * La serializacion de Java maneja adecuadamente las referencias ciclicas (Gremio <-> Personaje).
 */
public class Gremio implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String nombre;
    private final List<Personaje> miembros = new ArrayList<>();

    public Gremio(String nombre) {
        this.nombre = nombre;
    }

    public void agregarMiembro(Personaje personaje) {
        if (personaje == null || miembros.contains(personaje)) {
            return;
        }
        miembros.add(personaje);
        if (personaje.getGremio() != this) {
            personaje.unirseAGremio(this);
        }
    }

    public void removerMiembro(Personaje personaje) {
        if (!miembros.remove(personaje)) {
            return;
        }
        if (personaje.getGremio() == this) {
            personaje.abandonarGremio();
        }
    }

    public String getNombre() {
        return nombre;
    }

    public List<Personaje> getMiembros() {
        return Collections.unmodifiableList(miembros);
    }

    public void restaurar(Gremio otro) {
        if (otro == null) {
            return;
        }
        this.miembros.clear();
        List<Personaje> lista = new ArrayList<>(otro.getMiembros());
        for (Personaje p : lista) {
            agregarMiembro(p);
        }
    }
}
