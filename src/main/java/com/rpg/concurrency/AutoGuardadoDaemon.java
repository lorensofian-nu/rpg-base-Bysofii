package com.rpg.concurrency;

import com.rpg.core.MotorJuego;

/**
 * Hilo daemon que ejecuta el autoguardado en memoria periodicamente en segundo plano.
 *
 * DIFERENCIA ENTRE HILO DE USUARIO (USER THREAD) Y HILO DAEMON:
 * - Un hilo de usuario (user thread) mantiene la JVM con vida mientras este ejecutandose.
 *   La aplicacion Java no se cerrara hasta que todos los hilos de usuario hayan terminado
 *   (o se fuerce la salida con System.exit).
 * - Un hilo daemon es un hilo de soporte o servicio que corre en segundo plano. Cuando todos
 *   los hilos de usuario finalizan (por ejemplo, cuando el hilo principal 'main' termina),
 *   la JVM finaliza de inmediato, abandonando la ejecucion de los hilos daemon restantes
 *   sin esperar a que concluyan.
 *
 * REQUISITO DE CICLO DE VIDA:
 * - setDaemon(true) DEBE ser llamado antes de invocar start(). Si se llama despues de start(),
 *   la JVM lanza IllegalThreadStateException porque el estado de daemon no puede modificarse
 *   una vez que el hilo ha sido iniciado.
 */
public class AutoGuardadoDaemon extends Thread {

    private final MotorJuego motor;
    private final long intervaloMs;

    public AutoGuardadoDaemon(MotorJuego motor, long intervaloMs) {
        super("AutoGuardadoDaemon");
        if (intervaloMs <= 0) {
            throw new IllegalArgumentException("El intervalo debe ser positivo");
        }
        this.motor = motor;
        this.intervaloMs = intervaloMs;

        // IMPORTANTE: setDaemon(true) antes de start() para evitar IllegalThreadStateException
        setDaemon(true);
    }

    public AutoGuardadoDaemon(long intervaloMs) {
        this(null, intervaloMs);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(intervaloMs);
                if (motor != null) {
                    motor.guardarProgresoEnMemoriaSilencioso();
                }
            } catch (InterruptedException e) {
                // Si el daemon es interrumpido, restaurar la bandera y salir limpiamente
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public long getIntervaloMs() {
        return intervaloMs;
    }

    public MotorJuego getMotor() {
        return motor;
    }

    /**
     * PRUEBA DE INVESTIGACION OBLIGATORIA (Hilos daemon):
     * Demuestra:
     * 1) Que setDaemon(true) despues de start() lanza IllegalThreadStateException.
     * 2) Que la JVM cierra de inmediato aunque el hilo daemon siga vivo en un bucle infinito.
     */
    public static void main(String[] args) {
        System.out.println("=== PRUEBA DE INVESTIGACION: HILOS DAEMON ===");

        // 1. Demostracion de IllegalThreadStateException si se invoca setDaemon(true) despues de start()
        Thread hiloPrueba = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }, "HiloPruebaDaemon");

        hiloPrueba.start();
        try {
            hiloPrueba.setDaemon(true);
            System.err.println("ERROR: No se lanzo IllegalThreadStateException!");
        } catch (IllegalThreadStateException e) {
            System.out.println("[OK] Comprobado: invocar setDaemon(true) despues de start() lanza "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // 2. Demostracion de que la JVM cierra sin esperar al hilo daemon aunque este en bucle infinito
        Thread daemonInfinito = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "DaemonInfinito");
        daemonInfinito.setDaemon(true);
        daemonInfinito.start();

        System.out.println("[OK] Comprobado: hilo daemon activo en segundo plano (isDaemon="
                + daemonInfinito.isDaemon() + ", isAlive=" + daemonInfinito.isAlive() + ").");
        System.out.println("[OK] El hilo principal (main) termina ahora: la JVM finalizara de inmediato.");
    }
}
