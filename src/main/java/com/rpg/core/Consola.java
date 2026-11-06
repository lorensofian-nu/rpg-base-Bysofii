package com.rpg.core;

import com.rpg.concurrency.GestorTurno;
import com.rpg.concurrency.TemporizadorTurnoTask;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Entrada por teclado con validacion y reintento. */
public class Consola {

    private static final int LARGO_MAXIMO_NOMBRE = 15;
    private static final String PATRON_NOMBRE = "[\\p{L}0-9 ]+";

    private final Scanner scanner;
    private final ExecutorService lectorExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ConsolaLector");
        t.setDaemon(true);
        return t;
    });

    public Consola(Scanner scanner) {
        this.scanner = scanner;
    }

    public String leerLinea(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }

    public int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            String texto = leerLinea(mensaje);
            if (texto.isEmpty()) {
                System.out.println("No escribiste nada.");
                continue;
            }
            try {
                int valor = Integer.parseInt(texto);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.printf("Numero fuera de rango (%d a %d).%n", minimo, maximo);
            } catch (NumberFormatException e) {
                System.out.println("\"" + texto + "\" no es un numero.");
            }
        }
    }

    /**
     * Lee un entero con limite de tiempo supervisado por GestorTurno.
     * Retorna null si el temporizador expira antes de completar la lectura.
     */
    public Integer leerEnteroConTemporizador(String mensaje, int minimo, int maximo, GestorTurno gestor) {
        System.out.print(mensaje);
        while (!gestor.haExpirado()) {
            Future<String> future = lectorExecutor.submit(() -> {
                try {
                    return scanner.nextLine().trim();
                } catch (NoSuchElementException e) {
                    return null;
                }
            });

            try {
                String texto = future.get(gestor.getDuracionSegundos(), TimeUnit.SECONDS);
                if (texto == null || gestor.haExpirado()) {
                    return null;
                }
                if (texto.isEmpty()) {
                    System.out.println("No escribiste nada.");
                    System.out.print(mensaje);
                    continue;
                }
                try {
                    int valor = Integer.parseInt(texto);
                    if (valor >= minimo && valor <= maximo) {
                        return valor;
                    }
                    System.out.printf("Numero fuera de rango (%d a %d).%n", minimo, maximo);
                    System.out.print(mensaje);
                } catch (NumberFormatException e) {
                    System.out.println("\"" + texto + "\" no es un numero.");
                    System.out.print(mensaje);
                }
            } catch (TimeoutException e) {
                future.cancel(true);
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.cancel(true);
                return null;
            } catch (ExecutionException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Lee un entero con limite de tiempo supervisado por TemporizadorTurnoTask.
     * Retorna null si el temporizador expira antes de completar la lectura.
     */
    public Integer leerEnteroConTemporizador(String mensaje, int minimo, int maximo, TemporizadorTurnoTask temporizador) {
        System.out.print(mensaje);
        while (!temporizador.haExpirado()) {
            Future<String> future = lectorExecutor.submit(() -> {
                try {
                    return scanner.nextLine().trim();
                } catch (NoSuchElementException e) {
                    return null;
                }
            });

            try {
                String texto = future.get(temporizador.getDuracionSegundos(), TimeUnit.SECONDS);
                if (texto == null || temporizador.haExpirado()) {
                    return null;
                }
                if (texto.isEmpty()) {
                    System.out.println("No escribiste nada.");
                    System.out.print(mensaje);
                    continue;
                }
                try {
                    int valor = Integer.parseInt(texto);
                    if (valor >= minimo && valor <= maximo) {
                        return valor;
                    }
                    System.out.printf("Numero fuera de rango (%d a %d).%n", minimo, maximo);
                    System.out.print(mensaje);
                } catch (NumberFormatException e) {
                    System.out.println("\"" + texto + "\" no es un numero.");
                    System.out.print(mensaje);
                }
            } catch (TimeoutException e) {
                future.cancel(true);
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.cancel(true);
                return null;
            } catch (ExecutionException e) {
                return null;
            }
        }
        return null;
    }

    public String leerNombre(String mensaje) {
        while (true) {
            String nombre = leerLinea(mensaje);
            if (nombre.isEmpty()) {
                System.out.println("El nombre no puede estar vacio.");
            } else if (nombre.length() > LARGO_MAXIMO_NOMBRE) {
                System.out.println("Maximo " + LARGO_MAXIMO_NOMBRE + " caracteres.");
            } else if (!nombre.matches(PATRON_NOMBRE)) {
                System.out.println("Usa solo letras, numeros y espacios.");
            } else {
                return nombre;
            }
        }
    }
}
