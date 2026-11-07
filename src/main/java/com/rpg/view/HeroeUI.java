package com.rpg.view;

import com.rpg.controller.HeroeController;
import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;
import com.rpg.model.Heroe;

import java.util.List;
import java.util.Scanner;

/**
 * Vista (View) en la arquitectura MVC para la gestion de heroes.
 * Regla MVC: la View nunca toca la base de datos ni importa java.sql.
 * Se comunica exclusivamente a traves de HeroeController y presenta datos por consola.
 */
public class HeroeUI {

    private final Scanner scanner;
    private final HeroeController controller;
    private final MotorJuego motorJuego;
    private final Consola consola;

    public HeroeUI(Scanner scanner) {
        this(scanner, new HeroeController(), new MotorJuego(new Consola(scanner)));
    }

    public HeroeUI(Scanner scanner, HeroeController controller, MotorJuego motorJuego) {
        this.scanner = scanner;
        this.controller = controller;
        this.motorJuego = motorJuego;
        this.consola = new Consola(scanner);
    }

    /**
     * Bucle principal de la interfaz de usuario.
     */
    public void iniciar() {
        int opcion;
        do {
            mostrarMenu();
            opcion = consola.leerEntero("Seleccione una opcion: ", 0, 7);
            switch (opcion) {
                case 1 -> insertarHeroe();
                case 2 -> listarHeroes();
                case 3 -> buscarPorTipo();
                case 4 -> actualizarNivel();
                case 5 -> eliminarHeroe();
                case 6 -> cargarHeroeParaCombate();
                case 7 -> menuSimuladorRPG();
                case 0 -> System.out.println("Saliendo del sistema de heroes. Hasta pronto.");
                default -> System.out.println("Opcion no reconocida.");
            }
        } while (opcion != 0);
    }

    private void mostrarMenu() {
        System.out.println("\n=== REGISTRO DE HEROES (MVC + JDBC) ===");
        System.out.println("1. Insertar heroe en la base de datos");
        System.out.println("2. Listar todos los heroes");
        System.out.println("3. Buscar heroes por tipo");
        System.out.println("4. Actualizar nivel de un heroe");
        System.out.println("5. Eliminar un heroe por ID");
        System.out.println("6. Cargar heroe desde BD y jugar combate rapido");
        System.out.println("7. Menu del Simulador RPG (torneos, bestiario, partidas)");
        System.out.println("0. Salir");
    }

    // 1. Insertar heroe
    private void insertarHeroe() {
        System.out.println("\n--- Insertar Nuevo Heroe ---");
        String nombre = consola.leerNombre("Nombre del heroe: ");
        int tipoOpcion = consola.leerEntero("Tipo de heroe (1. Guerrero, 2. Mago): ", 1, 2);
        String tipo = (tipoOpcion == 1) ? "guerrero" : "mago";
        int vidaMaxima = (tipoOpcion == 1) ? 120 : 80;
        int nivel = consola.leerEntero("Nivel inicial (minimo 1): ", 1, 100);

        String resultado = controller.registrarHeroe(nombre, tipo, vidaMaxima, nivel);
        System.out.println(resultado);
    }

    // 2. Listar todos los heroes
    private void listarHeroes() {
        System.out.println("\n--- Heroes Registrados en Base de Datos ---");
        List<Heroe> heroes = controller.listarHeroes();
        if (heroes.isEmpty()) {
            System.out.println("No hay heroes registrados en la base de datos actualmente.");
            return;
        }
        for (Heroe h : heroes) {
            System.out.println("  " + h);
        }
        System.out.printf("Total: %d heroe(s) encontrado(s).%n", heroes.size());
    }

    // 3. Buscar heroes por tipo
    private void buscarPorTipo() {
        System.out.println("\n--- Buscar Heroes por Tipo ---");
        int tipoOpcion = consola.leerEntero("Seleccione tipo (1. Guerrero, 2. Mago): ", 1, 2);
        String tipo = (tipoOpcion == 1) ? "guerrero" : "mago";

        List<Heroe> heroes = controller.buscarHeroesPorTipo(tipo);
        if (heroes.isEmpty()) {
            System.out.printf("No se encontraron heroes del tipo '%s'.%n", tipo);
            return;
        }
        for (Heroe h : heroes) {
            System.out.println("  " + h);
        }
        System.out.printf("Total: %d %s(s) encontrado(s).%n", heroes.size(), tipo);
    }

    // 4. Actualizar nivel
    private void actualizarNivel() {
        System.out.println("\n--- Actualizar Nivel de Heroe ---");
        int id = consola.leerEntero("ID del heroe a actualizar: ", 1, Integer.MAX_VALUE);
        int nuevoNivel = consola.leerEntero("Nuevo nivel (minimo 1): ", 1, 100);

        String resultado = controller.actualizarNivel(id, nuevoNivel);
        System.out.println(resultado);
    }

    // 5. Eliminar heroe
    private void eliminarHeroe() {
        System.out.println("\n--- Eliminar Heroe ---");
        int id = consola.leerEntero("ID del heroe a eliminar: ", 1, Integer.MAX_VALUE);
        int confirmacion = consola.leerEntero("Confirmar eliminacion (1. Si, 2. No): ", 1, 2);
        if (confirmacion != 1) {
            System.out.println("Eliminacion cancelada.");
            return;
        }

        String resultado = controller.eliminarHeroe(id);
        System.out.println(resultado);
    }

    // 6. Cargar heroe desde BD y jugar combate rapido
    private void cargarHeroeParaCombate() {
        System.out.println("\n--- Cargar Heroe desde BD para Combate Rapido ---");
        List<Heroe> heroes = controller.listarHeroes();
        if (heroes.isEmpty()) {
            System.out.println("No hay heroes en la base de datos. Inserta uno primero con la opcion 1.");
            return;
        }

        for (Heroe h : heroes) {
            System.out.println("  " + h);
        }

        int id = consola.leerEntero("Ingrese el ID del heroe que desea usar: ", 1, Integer.MAX_VALUE);
        Heroe heroeDB = controller.buscarHeroePorId(id);
        if (heroeDB == null) {
            System.out.println("No existe un heroe con ese ID en la base de datos.");
            return;
        }

        Personaje combatiente;
        if (heroeDB.getTipo().equalsIgnoreCase("guerrero")) {
            combatiente = new Guerrero(heroeDB.getNombre());
        } else {
            combatiente = new Mago(heroeDB.getNombre());
        }

        System.out.printf("Cargando a %s (%s, Nivel %d) para combate...%n",
                heroeDB.getNombre(), heroeDB.getTipo(), heroeDB.getNivel());
        motorJuego.combateRapidoConPersonaje(combatiente);
    }

    // 7. Menu clasico del simulador
    private void menuSimuladorRPG() {
        int opcion;
        do {
            System.out.println("\n=== SIMULADOR DE BATALLA RPG ===");
            System.out.println("1. Combate rapido (crear heroe en consola)");
            System.out.println("2. Torneo");
            System.out.println("3. Ver bestiario");
            System.out.println("4. Cargar partida desde disco (.dat)");
            System.out.println("5. Guardar progreso actual en disco");
            System.out.println("6. Ver historial de combates en disco");
            System.out.println("7. Inspeccionar archivos de guardado");
            System.out.println("8. Borrar partida guardada en disco");
            System.out.println("0. Volver al menu de heroes");
            opcion = consola.leerEntero("Opcion: ", 0, 8);
            switch (opcion) {
                case 1 -> motorJuego.combateRapido();
                case 2 -> motorJuego.jugarTorneo();
                case 3 -> motorJuego.mostrarBestiario();
                case 4 -> motorJuego.cargarProgreso();
                case 5 -> motorJuego.guardarProgreso();
                case 6 -> motorJuego.mostrarHistorialLog();
                case 7 -> motorJuego.inspeccionarArchivos();
                case 8 -> {
                    int conf = consola.leerEntero("Esta seguro de borrar la partida guardada? (1. Si, 2. No): ", 1, 2);
                    if (conf == 1) {
                        motorJuego.borrarPartida();
                    } else {
                        System.out.println("Borrado cancelado.");
                    }
                }
                case 0 -> System.out.println("Regresando al menu de heroes...");
            }
        } while (opcion != 0);
    }
}
