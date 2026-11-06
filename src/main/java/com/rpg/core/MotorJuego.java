package com.rpg.core;

import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.concurrency.GestorTurno;
import com.rpg.concurrency.TemporizadorTurnoTask;
import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;
import com.rpg.persistencia.EstadoPartida;
import com.rpg.persistencia.GestorArchivos;

import java.io.File;
import java.util.List;
import java.util.Random;

/** Coordina el flujo del juego: crear heroe, combatir, torneo, bestiario y guardado. */
public class MotorJuego {

    private static final String NOMBRE_GREMIO = "Guardianes del Alba";
    private static final int CURACION_POCION_VIDA = 30;
    private static final int SEGUNDOS_TURNO = 10;

    private final Consola consola;
    private final RegistroCombate registro = new RegistroCombate();
    private final Arena arena = new Arena(registro, new Random());
    private final Bestiario bestiario = new Bestiario();
    private final PerfilJugador perfil = new PerfilJugador();
    private final Gremio gremio = new Gremio(NOMBRE_GREMIO);
    private final TorneoManager torneo = new TorneoManager(arena, registro);
    private final GestorArchivos gestorArchivos = new GestorArchivos();
    private Personaje heroeActivo;
    private String ultimoGuardado = "";

    public MotorJuego(Consola consola) {
        this.consola = consola;
        // TALLER CONCURRENCIA: hilo daemon en segundo plano para auto-guardado en memoria
        AutoGuardadoDaemon daemon = new AutoGuardadoDaemon(this, 3000);
        daemon.start();
    }

    // ---- opciones del menu principal

    public void combateRapido() {
        registro.limpiar();
        Personaje jugador = crearJugador();
        Enemigo enemigo = elegirEnemigo();
        registro.agregar("%n=== %s vs %s ===", jugador.getNombre(), enemigo.getNombre());

        boolean huyo = combatir(jugador, enemigo);

        cerrarCombate(jugador, enemigo, huyo);
        guardarProgreso();
    }

    public void jugarTorneo() {
        registro.limpiar();
        Personaje campeon = torneo.jugarTorneo();
        registro.agregar("%nCAMPEON DEL TORNEO: %s", campeon.getNombre());
        perfil.desbloquearLogro("Campeon del Torneo");
        guardarProgreso();
    }

    public void mostrarBestiario() {
        System.out.println("\n=== BESTIARIO ===");
        System.out.print(bestiario.describir());
        System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
    }

    // ---- creacion de personajes

    private Personaje crearJugador() {
        int clase = consola.leerEntero("Clase (1. Guerrero, 2. Mago): ", 1, 2);
        String nombre = consola.leerNombre("Nombre del heroe: ");
        Personaje jugador = switch (clase) {
            case 1 -> new Guerrero(nombre);
            default -> new Mago(nombre);
        };
        entregarKitInicial(jugador);
        jugador.unirseAGremio(gremio);
        this.heroeActivo = jugador;
        System.out.printf("%s se une al gremio %s (%d miembros).%n",
                jugador.getNombre(), gremio.getNombre(), gremio.getMiembros().size());
        return jugador;
    }

    private void entregarKitInicial(Personaje jugador) {
        List<Pocion> kit = List.of(
                new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN),
                new Pocion("Elixir de Regeneracion", 0, RaridadItem.RARO,
                        new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 3), false),
                new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true));
        try {
            for (Pocion pocion : kit) {
                jugador.getInventario().agregar(pocion);
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudo entregar todo el kit: " + e.getMessage());
        }
    }

    private Enemigo elegirEnemigo() {
        List<String> nombres = bestiario.nombres();
        System.out.println("Rivales disponibles:");
        for (int i = 0; i < nombres.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, nombres.get(i));
        }
        while (true) {
            String texto = consola.leerLinea("Rival (numero o nombre): ");
            Enemigo enemigo = resolverRival(texto, nombres);
            if (enemigo != null) {
                return enemigo;
            }
            System.out.println("No existe ese rival.");
        }
    }

    private Enemigo resolverRival(String texto, List<String> nombres) {
        try {
            int numero = Integer.parseInt(texto);
            boolean valido = numero >= 1 && numero <= nombres.size();
            return valido ? bestiario.crearEnemigo(nombres.get(numero - 1)) : null;
        } catch (NumberFormatException e) {
            return bestiario.crearEnemigo(texto);
        }
    }

    // ---- bucle de combate

    /** @return true si el jugador huyo */
    private boolean combatir(Personaje jugador, Enemigo enemigo) {
        int ronda = 1;
        while (jugador.estaVivo() && enemigo.estaVivo()) {
            registro.agregar("%n-- Ronda %d --", ronda);
            if (arena.iniciarTurno(jugador) && turnoJugador(jugador, enemigo)) {
                return true;
            }
            arena.turnoAutomatico(enemigo, jugador);
            ronda++;
        }
        return false;
    }

    /**
     * Turno del jugador. Las acciones que no gastan turno (ver estado, cancelar) vuelven a pedir.
     * @return true si el jugador decidio huir
     */
    private boolean turnoJugador(Personaje jugador, Enemigo enemigo) {
        boolean turnoGastado;
        do {
            registro.agregar(FormatoCombate.resumen(jugador));
            registro.agregar(FormatoCombate.resumen(enemigo));
            Accion accion = pedirAccion();
            if (accion == null) {
                // TALLER CONCURRENCIA: El tiempo se agoto antes de elegir accion; el turno se pierde
                registro.agregar("%s pierde el turno por tiempo agotado.", jugador.getNombre());
                return false;
            }
            if (accion == Accion.HUIR) {
                return true;
            }
            turnoGastado = switch (accion) {
                case ATACAR -> {
                    arena.atacar(jugador, enemigo);
                    yield true;
                }
                case ESPECIAL -> arena.intentarEspecial(jugador, enemigo);
                case POCION -> usarPocion(jugador);
                default -> {
                    mostrarEstado(jugador);
                    yield false;
                }
            };
        } while (!turnoGastado && enemigo.estaVivo());
        return false;
    }

    /**
     * Pide la accion al jugador supervisado por un temporizador concurrente en segundo plano.
     */
    private Accion pedirAccion() {
        // TALLER CONCURRENCIA: GestorTurno crea el Thread con TemporizadorTurnoTask y llama a .start()
        GestorTurno gestor = new GestorTurno(SEGUNDOS_TURNO);
        gestor.iniciar(() -> {
            System.out.println("\n[!] Tiempo agotado para elegir accion!");
        });

        System.out.printf("1. Atacar  2. Habilidad especial  3. Usar pocion  4. Ver estado  0. Huir  (Tiempo: %ds)%n", SEGUNDOS_TURNO);
        Integer opcion = consola.leerEnteroConTemporizador("Accion: ", 0, 4, gestor);

        // Si el tiempo se agoto antes de que el jugador elija, el turno se pierde
        if (opcion == null || gestor.haExpirado()) {
            return null;
        }

        // Si el jugador responde a tiempo, cancelar el temporizador con .interrupt() (sin banderas booleanas manuales)
        gestor.cancelar();

        return switch (opcion) {
            case 1 -> Accion.ATACAR;
            case 2 -> Accion.ESPECIAL;
            case 3 -> Accion.POCION;
            case 4 -> Accion.ESTADO;
            default -> Accion.HUIR;
        };
    }

    private boolean usarPocion(Personaje jugador) {
        Inventario<Pocion> inventario = jugador.getInventario();
        if (inventario.estaVacio()) {
            System.out.println("No tienes pociones.");
            return false;
        }
        int numero = 1;
        for (Pocion pocion : inventario) {
            System.out.printf("  %d. %s%n", numero++, pocion.getNombre());
        }
        int eleccion = consola.leerEntero("Pocion (0 = cancelar): ", 0, inventario.tamanio());
        if (eleccion == 0) {
            return false;
        }
        registro.agregar(jugador.usarPocion(inventario.quitar(eleccion - 1)));
        return true;
    }

    private void mostrarEstado(Personaje jugador) {
        System.out.printf("Habilidades: %s | Arma: %s | Defensa: %d | Pociones: %d%n",
                jugador.getHabilidades(),
                jugador.getArma() != null ? jugador.getArma().getNombre() : "ninguna",
                jugador.getDefensa(), jugador.getInventario().tamanio());
        for (EstadoEfecto efecto : jugador.getEfectos()) {
            System.out.printf("Efecto activo: %s (%d turnos)%n", efecto.getNombre(), efecto.getTurnosRestantes());
        }
    }

    // ---- resultado del combate

    private void cerrarCombate(Personaje jugador, Enemigo enemigo, boolean huyo) {
        // TALLER CONCURRENCIA: Al terminar el combate, todos los hilos de efectos continuos se interrumpen limpiamente
        jugador.detenerEfectos();
        enemigo.detenerEfectos();

        if (huyo) {
            registro.agregar("%s huye del combate.", jugador.getNombre());
        } else if (jugador.estaVivo()) {
            registro.agregar("%nVictoria de %s!", jugador.getNombre());
            perfil.registrarVictoria();
            bestiario.registrarEnemigoDerrotado(enemigo.getNombre());
            desbloquearLogros(jugador, enemigo);
            repartirBotin(jugador);
        } else {
            registro.agregar("%n%s ha caido ante %s.", jugador.getNombre(), enemigo.getNombre());
            perfil.registrarDerrota();
        }
    }

    private void desbloquearLogros(Personaje jugador, Enemigo enemigo) {
        boolean intacto = jugador.getVida() == jugador.getVidaMaxima();
        List<String> candidatos = List.of("Primera Victoria", "Cazador de " + enemigo.getNombre(),
                intacto ? "Intocable" : "");
        for (String logro : candidatos) {
            if (!logro.isEmpty() && perfil.desbloquearLogro(logro)) {
                registro.agregar("Logro desbloqueado: %s", logro);
            }
        }
    }

    private void repartirBotin(Personaje jugador) {
        try {
            jugador.getInventario().agregar(new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN));
            registro.agregar("Botin: Pocion de Vida.");
        } catch (CapacidadExcedidaException e) {
            registro.agregar("Botin perdido: %s", e.getMessage());
        }
    }

    // ---- guardado y persistencia en disco

    /**
     * Guarda las estadisticas en memoria periodicamente en segundo plano a traves del hilo daemon
     * sin interrumpir al usuario con mensajes en consola.
     */
    public synchronized void guardarProgresoEnMemoriaSilencioso() {
        StringBuilder contenido = new StringBuilder();
        contenido.append("=== PARTIDA RPG ===").append(System.lineSeparator());
        contenido.append("Victorias: ").append(perfil.getVictorias()).append(System.lineSeparator());
        contenido.append("Derrotas: ").append(perfil.getDerrotas()).append(System.lineSeparator());
        contenido.append("Logros: ").append(String.join(", ", perfil.getLogros())).append(System.lineSeparator());
        contenido.append("Gremio ").append(gremio.getNombre()).append(": ")
                .append(gremio.getMiembros().size()).append(" miembros").append(System.lineSeparator());
        contenido.append("Enemigos derrotados: ").append(bestiario.resumenDerrotados()).append(System.lineSeparator());
        contenido.append(System.lineSeparator()).append("--- Ultimo combate ---").append(System.lineSeparator());
        contenido.append(registro.getTexto());
        ultimoGuardado = contenido.toString();
    }

    /**
     * Seccion 7 (Concurrencia + Persistencia):
     * AutoGuardadoDaemon invoca este metodo para sincronizar el guardado a disco en segundo plano
     * de forma segura, sin interferir con lecturas/escrituras del hilo principal.
     */
    public synchronized void guardarProgresoEnDiscoSilencioso() {
        guardarProgresoEnMemoriaSilencioso();
        EstadoPartida estado = new EstadoPartida(perfil, gremio, bestiario, heroeActivo);
        gestorArchivos.guardarPartida(gestorArchivos.getArchivoPartida(), estado);
    }

    /**
     * Guarda el estado de la partida en memoria y en disco (.dat con ObjectOutputStream
     * y log de combates con BufferedWriter en append mode).
     */
    public synchronized void guardarProgreso() {
        guardarProgresoEnMemoriaSilencioso();
        registro.guardarEnArchivo(gestorArchivos.getArchivoLog(), true);
        EstadoPartida estado = new EstadoPartida(perfil, gremio, bestiario, heroeActivo);
        boolean guardadoDisco = gestorArchivos.guardarPartida(gestorArchivos.getArchivoPartida(), estado);
        if (guardadoDisco) {
            System.out.println("[Progreso guardado en disco y memoria]");
        } else {
            System.out.println("[Progreso guardado solo en memoria]");
        }
    }

    /**
     * Carga la partida guardada desde disco (.dat) usando ObjectInputStream.
     * Restaura perfil, gremio, bestiario y el heroe actual.
     * Comprueba que la relacion bidireccional Personaje <-> Gremio se preserve.
     */
    public synchronized boolean cargarProgreso() {
        EstadoPartida estado = gestorArchivos.cargarPartida(gestorArchivos.getArchivoPartida());
        if (estado == null) {
            System.out.println("No se pudo cargar la partida.");
            return false;
        }

        perfil.restaurar(estado.getPerfil());
        gremio.restaurar(estado.getGremio());
        bestiario.restaurar(estado.getBestiario());
        this.heroeActivo = estado.getHeroeActual();

        System.out.println("\n[Partida cargada exitosamente]");
        System.out.println("Fecha de guardado: " + estado.getFechaGuardado());
        System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
        System.out.printf("Gremio %s: %d miembros registrados.%n",
                gremio.getNombre(), gremio.getMiembros().size());

        if (heroeActivo != null) {
            System.out.printf("Heroe restaurado: %s (%s, HP %d/%d).%n",
                    heroeActivo.getNombre(), heroeActivo.getTipo(),
                    heroeActivo.getVida(), heroeActivo.getVidaMaxima());
            // Verificacion del ciclo Personaje - Gremio
            boolean cicloCorrecto = heroeActivo.getGremio() != null &&
                    heroeActivo.getGremio().getNombre().equals(gremio.getNombre());
            if (cicloCorrecto) {
                System.out.println("[OK] Relacion ciclica Heroe <-> Gremio reconstruida correctamente.");
            }
        }
        return true;
    }

    /**
     * Elimina el archivo de partida guardada en disco previa confirmacion.
     */
    public synchronized void borrarPartida() {
        File archivo = gestorArchivos.getArchivoPartida();
        if (!archivo.exists()) {
            System.out.println("No existe ninguna partida guardada en disco para borrar.");
            return;
        }
        boolean borrado = gestorArchivos.borrarConFiles(archivo.toPath());
        if (borrado) {
            System.out.println("Partida guardada borrada con exito.");
        }
    }

    /**
     * Lee y muestra por consola el log de combate persistido en combates.txt.
     */
    public void mostrarHistorialLog() {
        System.out.println("\n=== HISTORIAL DE COMBATES EN DISCO ===");
        String historial = gestorArchivos.leerLogCombate(gestorArchivos.getArchivoLog());
        if (historial.isBlank()) {
            System.out.println("(No hay combates registrados aun en disco)");
        } else {
            System.out.print(historial);
        }
    }

    /**
     * Inspecciona los archivos del sistema usando la clase File.
     */
    public void inspeccionarArchivos() {
        System.out.println("\n=== INSPECCION DE ARCHIVOS DE GUARDADO ===");
        System.out.println(gestorArchivos.inspeccionarArchivo(gestorArchivos.getArchivoPartida()));
        System.out.println(gestorArchivos.inspeccionarArchivo(gestorArchivos.getArchivoLog()));
    }

    public synchronized String getUltimoGuardado() {
        return ultimoGuardado;
    }

    public Personaje getHeroeActivo() {
        return heroeActivo;
    }

    public GestorArchivos getGestorArchivos() {
        return gestorArchivos;
    }
}
