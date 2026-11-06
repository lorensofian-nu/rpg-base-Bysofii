package com.rpg.persistencia;

import com.rpg.core.Bestiario;
import com.rpg.entities.Gremio;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa el estado persistente de una partida guardada.
 * Reune el perfil del jugador, el gremio (con sus personajes asociados),
 * el bestiario con el conteo de derrotas y el heroe actual.
 *
 * USO DE serialVersionUID:
 * Garantiza que la clase en memoria corresponda a la version guardada en el archivo binario (.dat).
 *
 * USO DE transient:
 * El campo 'sesionTemporal' se declara transient para demostrar que no se serializa a disco
 * y recupera su valor por defecto (null) tras la deserializacion con ObjectInputStream.
 */
public class EstadoPartida implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PerfilJugador perfil;
    private final Gremio gremio;
    private final Bestiario bestiario;
    private final Personaje heroeActual;
    private final String fechaGuardado;

    // Campo transitorio para demostrar la exclusion durante la serializacion
    private transient String sesionTemporal;

    public EstadoPartida(PerfilJugador perfil, Gremio gremio, Bestiario bestiario, Personaje heroeActual) {
        if (perfil == null || gremio == null || bestiario == null) {
            throw new IllegalArgumentException("Los componentes del estado no pueden ser nulos");
        }
        this.perfil = perfil;
        this.gremio = gremio;
        this.bestiario = bestiario;
        this.heroeActual = heroeActual;
        this.fechaGuardado = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.sesionTemporal = "SESION_" + System.currentTimeMillis();
    }

    public PerfilJugador getPerfil() {
        return perfil;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Bestiario getBestiario() {
        return bestiario;
    }

    public Personaje getHeroeActual() {
        return heroeActual;
    }

    public String getFechaGuardado() {
        return fechaGuardado;
    }

    public String getSesionTemporal() {
        return sesionTemporal;
    }

    public void setSesionTemporal(String sesionTemporal) {
        this.sesionTemporal = sesionTemporal;
    }

    @Override
    public String toString() {
        return String.format("Partida guardada el %s [Victorias: %d, Miembros gremio: %d, Heroe: %s]",
                fechaGuardado, perfil.getVictorias(), gremio.getMiembros().size(),
                heroeActual != null ? heroeActual.getNombre() : "Ninguno");
    }
}
