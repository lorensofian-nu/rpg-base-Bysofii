package com.rpg.core;

import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;

import java.util.List;

/** Torneo de 4 gladiadores: dos semifinales y una final. */
public class TorneoManager {

    private final Arena arena;
    private final RegistroCombate registro;

    public TorneoManager(Arena arena, RegistroCombate registro) {
        this.arena = arena;
        this.registro = registro;
    }

    /** @return el campeon del torneo */
    public Personaje jugarTorneo() {
        List<Personaje> gladiadores = crearGladiadores().getMiembros();

        // TALLER CONCURRENCIA: Se ejecutan las dos semifinales en paralelo utilizando dos hilos independientes
        // y se sincroniza la continuacion a la final con Thread.join().
        final Personaje[] finalistas = new Personaje[2];

        // Creacion de hilos con su respectivo Runnable (uno por semifinal)
        Thread hiloSemiA = new Thread(() -> {
            finalistas[0] = simularSemifinal(gladiadores.get(0), gladiadores.get(1));
        }, "Hilo-Semifinal-A");

        Thread hiloSemiB = new Thread(() -> {
            finalistas[1] = simularSemifinal(gladiadores.get(2), gladiadores.get(3));
        }, "Hilo-Semifinal-B");

        // 1. Estado antes de start(): NEW (hilo instanciado pero no arrancado)
        registro.agregar("[Torneo] Estado Semifinal A antes de start(): %s (NEW)", hiloSemiA.getState());
        registro.agregar("[Torneo] Estado Semifinal B antes de start(): %s (NEW)", hiloSemiB.getState());

        // Iniciar ambos hilos concurrentemente con start() (nunca llamar a run() directamente)
        hiloSemiA.start();
        hiloSemiB.start();

        // 2. Estado durante la ejecucion: RUNNABLE / TIMED_WAITING (en ejecucion o esperando CPU/bloqueo)
        registro.agregar("[Torneo] Estado Semifinal A durante ejecucion: %s (RUNNABLE / TIMED_WAITING)", hiloSemiA.getState());
        registro.agregar("[Torneo] Estado Semifinal B durante ejecucion: %s (RUNNABLE / TIMED_WAITING)", hiloSemiB.getState());

        // El hilo principal espera a que ambas semifinales concluyan mediante join()
        try {
            hiloSemiA.join();
            hiloSemiB.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            registro.agregar("[Torneo] Se interrumpio la espera de las semifinales.");
        }

        // 3. Estado despues de join(): TERMINATED (la tarea concluyo exitosamente)
        registro.agregar("[Torneo] Estado Semifinal A despues de join(): %s (TERMINATED)", hiloSemiA.getState());
        registro.agregar("[Torneo] Estado Semifinal B despues de join(): %s (TERMINATED)", hiloSemiB.getState());

        // Consultar los ganadores de la memoria compartida SOLO despues de que ambos join hayan retornado
        Personaje finalista1 = finalistas[0];
        Personaje finalista2 = finalistas[1];

        return simularFinal(finalista1, finalista2);
    }

    public Personaje simularSemifinal(Personaje a, Personaje b) {
        registro.agregar("=== SEMIFINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        Personaje ganador = arena.duelo(a, b);
        registro.agregar("Clasifica a la final: %s", ganador.getNombre());
        return ganador;
    }

    private Personaje simularFinal(Personaje a, Personaje b) {
        a.restaurarVida();
        b.restaurarVida();
        registro.agregar("=== FINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        return arena.duelo(a, b);
    }

    private Escuadron crearGladiadores() {
        Escuadron escuadron = new Escuadron("Gladiadores");
        escuadron.agregar(new Guerrero("Maximo"));
        escuadron.agregar(new Mago("Circe"));
        escuadron.agregar(new Guerrero("Aquiles"));
        escuadron.agregar(new Mago("Merlina"));
        return escuadron;
    }
}
