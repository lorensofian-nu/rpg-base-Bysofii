/**
 * Paquete para el taller de concurrencia e hilos aplicados al RPG.
 * Contiene cuatro clases especializadas:
 * - TemporizadorTurnoTask: Runnable que ejecuta la cuenta regresiva del turno.
 * - GestorTurno: Administrador del hilo temporizador y su cancelacion por interrupcion.
 * - EfectoContinuoTask: Hilos independientes para efectos periodicos de dano/curacion (DoT/HoT).
 * - AutoGuardadoDaemon: Hilo daemon en segundo plano para auto-guardado en memoria.
 */
package com.rpg.concurrency;
