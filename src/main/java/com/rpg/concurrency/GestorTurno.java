package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

/**
 * Gestor del temporizador de turno (Seccion 1 del taller de concurrencia).
 * Se encarga de instanciar un Thread nuevo con el Runnable (TemporizadorTurnoTask),
 * llamar a .start() y cancelar la ejecucion mediante .interrupt() cuando el jugador responde a tiempo.
 */
public class GestorTurno {

    private final int duracionSegundos;
    private TurnoCallback callback;
    private Thread hiloTemporizador;
    private volatile boolean tiempoAgotado;

    public GestorTurno(int duracionSegundos) {
        this(duracionSegundos, null);
    }

    public GestorTurno(int duracionSegundos, TurnoCallback callback) {
        if (duracionSegundos <= 0) {
            throw new IllegalArgumentException("La duracion debe ser positiva");
        }
        this.duracionSegundos = duracionSegundos;
        this.callback = callback;
    }

    /**
     * Inicia el temporizador creando un nuevo Thread con TemporizadorTurnoTask y llamando a start().
     * Utiliza el callback provisto previamente en el constructor.
     */
    public void iniciar() {
        iniciar(this.callback);
    }

    /**
     * Inicia el temporizador creando un nuevo Thread con TemporizadorTurnoTask y llamando a start().
     * Nunca se reutiliza ni se llama a start() dos veces sobre el mismo hilo.
     */
    public void iniciar(TurnoCallback callback) {
        this.callback = callback;
        this.tiempoAgotado = false;
        TemporizadorTurnoTask task = new TemporizadorTurnoTask(duracionSegundos, () -> {
            this.tiempoAgotado = true;
            if (this.callback != null) {
                this.callback.onTiempoAgotado();
            }
        });

        this.hiloTemporizador = new Thread(task, "TemporizadorTurno");
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
