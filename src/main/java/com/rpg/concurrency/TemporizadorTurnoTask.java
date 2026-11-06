package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

/**
 * Tarea en segundo plano encargada de contar el tiempo limite del turno del jugador.
 * Implementa Runnable y realiza una cuenta regresiva segundo a segundo.
 * Administra tambien la creacion del Thread (start()) y su cancelacion mediante interrupt().
 */
public class TemporizadorTurnoTask implements Runnable {

    private final int duracionSegundos;
    private final TurnoCallback callback;
    private Thread hiloTemporizador;
    private volatile boolean tiempoAgotado;

    public TemporizadorTurnoTask(int duracionSegundos, TurnoCallback callback) {
        if (duracionSegundos <= 0) {
            throw new IllegalArgumentException("La duracion debe ser positiva");
        }
        this.duracionSegundos = duracionSegundos;
        this.callback = callback;
    }

    @Override
    public void run() {
        // Cuenta regresiva con bucle decreciente, un segundo por iteracion
        for (int restante = duracionSegundos; restante > 0; restante--) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Si el jugador responde a tiempo, el hilo es interrumpido con interrupt()
                // Se restaura el estado de interrupcion y se sale inmediatamente sin banderas booleanas
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Al llegar a 0 sin haber sido interrumpido, se notifica el tiempo agotado
        this.tiempoAgotado = true;
        if (callback != null) {
            callback.onTiempoAgotado();
        }
    }

    /**
     * Inicia el temporizador creando un nuevo Thread con esta tarea Runnable y llamando a start().
     * Nunca se llama a start() dos veces sobre el mismo hilo.
     */
    public void iniciar() {
        this.tiempoAgotado = false;
        this.hiloTemporizador = new Thread(this, "TemporizadorTurno");
        this.hiloTemporizador.setDaemon(true);
        this.hiloTemporizador.start();
    }

    /**
     * Cancela el temporizador mediante interrupcion del hilo (interrupt()).
     * No utiliza banderas booleanas manuales dentro de la tarea.
     */
    public void cancelar() {
        if (hiloTemporizador != null && hiloTemporizador.isAlive()) {
            hiloTemporizador.interrupt();
        }
    }

    public boolean haExpirado() {
        return tiempoAgotado;
    }

    public int getDuracionSegundos() {
        return duracionSegundos;
    }
}
