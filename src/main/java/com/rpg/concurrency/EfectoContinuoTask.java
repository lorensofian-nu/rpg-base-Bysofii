package com.rpg.concurrency;

import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Personaje;

/**
 * Tarea en segundo plano para procesar un efecto continuo DoT (Damage over Time) o
 * HoT (Heal over Time) en su propio hilo independiente.
 *
 * Utiliza interrupcion limpia (isInterrupted / InterruptedException) y un bloque finally
 * para normalizar el estado del personaje y liberar recursos al terminar.
 */
public class EfectoContinuoTask implements Runnable {

    private final Personaje personaje;
    private final EstadoEfecto efecto;
    private final String nombreEfecto;
    private final long intervaloMs;

    public EfectoContinuoTask(Personaje personaje, EstadoEfecto efecto, long intervaloMs) {
        if (personaje == null || efecto == null) {
            throw new IllegalArgumentException("Personaje y efecto no pueden ser nulos");
        }
        if (intervaloMs <= 0) {
            throw new IllegalArgumentException("El intervalo debe ser positivo");
        }
        this.personaje = personaje;
        this.efecto = efecto;
        this.nombreEfecto = efecto.getNombre();
        this.intervaloMs = intervaloMs;
    }

    public EfectoContinuoTask(Personaje personaje, String nombreEfecto, long intervaloMs) {
        this(personaje, new EstadoEfecto(nombreEfecto, com.rpg.entities.enums.TipoEfecto.DANIO_CONTINUO, 4, 3), intervaloMs);
    }

    /**
     * Constructor segun la definicion de la guia (nombre e intervalo).
     */
    public EfectoContinuoTask(String nombreEfecto, long intervaloMs) {
        if (nombreEfecto == null || nombreEfecto.isBlank()) {
            throw new IllegalArgumentException("El nombre del efecto no puede estar vacio");
        }
        if (intervaloMs <= 0) {
            throw new IllegalArgumentException("El intervalo debe ser positivo");
        }
        this.personaje = null;
        this.efecto = new EstadoEfecto(nombreEfecto, com.rpg.entities.enums.TipoEfecto.DANIO_CONTINUO, 4, 3);
        this.nombreEfecto = nombreEfecto;
        this.intervaloMs = intervaloMs;
    }

    @Override
    public void run() {
        try {
            // Se ejecuta mientras el hilo no haya sido interrumpido y, si tiene personaje, este siga vivo y el efecto tenga ticks restantes
            while (!Thread.currentThread().isInterrupted() && (personaje == null || (personaje.estaVivo() && !efecto.haTerminado()))) {
                try {
                    Thread.sleep(intervaloMs);
                } catch (InterruptedException e) {
                    // Al ser interrumpido (por un antidoto, purificacion o fin del combate):
                    // 1. Limpiar el estado del personaje
                    if (personaje != null) {
                        personaje.normalizarEstado();
                    }
                    // 2. Restaurar la bandera de interrupcion
                    Thread.currentThread().interrupt();
                    // 3. Salir del bucle inmediatamente
                    break;
                }

                // Verificar interrupcion despues del sleep o antes de aplicar el efecto
                if (Thread.currentThread().isInterrupted() || (personaje != null && !personaje.estaVivo())) {
                    break;
                }

                // Aplicar el danio o curacion de forma sincronizada
                if (personaje != null && efecto != null) {
                    if (efecto.esDanio()) {
                        personaje.recibirDanio(efecto.getValorPorTick());
                    } else {
                        personaje.curar(efecto.getValorPorTick());
                    }
                    efecto.consumirTurno();
                }
            }
        } finally {
            // El bloque finally asegura la liberacion de recursos y la consistencia del estado del personaje
            if (personaje != null) {
                personaje.normalizarEstado();
            }
        }
    }

    public String getNombreEfecto() {
        return nombreEfecto;
    }

    public EstadoEfecto getEfecto() {
        return efecto;
    }

    public Personaje getPersonaje() {
        return personaje;
    }

    public long getIntervaloMs() {
        return intervaloMs;
    }
}
