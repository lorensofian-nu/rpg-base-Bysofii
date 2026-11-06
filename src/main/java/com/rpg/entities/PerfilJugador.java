package com.rpg.entities;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Perfil y progreso del jugador (victorias, derrotas y logros).
 * Implementa Serializable para persistir estadisticas de la partida.
 */
public class PerfilJugador implements Serializable {

    private static final long serialVersionUID = 1L;

    private int victorias;
    private int derrotas;
    private final Set<String> logros = new TreeSet<>();

    public void registrarVictoria() {
        victorias++;
    }

    public void registrarDerrota() {
        derrotas++;
    }

    /** @return true si el logro era nuevo; false si ya estaba desbloqueado */
    public boolean desbloquearLogro(String logro) {
        return logros.add(logro);
    }

    public int getVictorias() {
        return victorias;
    }

    public int getDerrotas() {
        return derrotas;
    }

    /** Los logros salen ordenados alfabeticamente porque se guardan en un TreeSet. */
    public Set<String> getLogros() {
        return Collections.unmodifiableSet(logros);
    }

    public void restaurar(PerfilJugador otro) {
        if (otro == null) {
            return;
        }
        this.victorias = otro.victorias;
        this.derrotas = otro.derrotas;
        this.logros.clear();
        this.logros.addAll(otro.logros);
    }
}
