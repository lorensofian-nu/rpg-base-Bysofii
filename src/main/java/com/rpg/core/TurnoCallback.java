package com.rpg.core;

/**
 * Notificacion asincrona cuando el tiempo disponible para el turno del jugador se agota.
 */
@FunctionalInterface
public interface TurnoCallback {

    /** Metodo invocado por el hilo temporizador al agotarse el tiempo del turno. */
    void onTiempoAgotado();
}
