package com.rpg.entities;

import com.rpg.concurrency.EfectoContinuoTask;
import com.rpg.entities.enums.EstadoPersonaje;
import com.rpg.entities.enums.TipoPersonaje;
import com.rpg.exceptions.SinManaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

// TALLER ARCHIVOS: para guardar la partida con ObjectOutputStream, Personaje debe volverse Serializable
// (con serialVersionUID). Piensa que campos son transient y que clases anidadas (Inventario, Pocion, Arma,
// Gremio, EstadoEfecto, enums) tambien deben ser Serializable, o tendras NotSerializableException.
// Ojo: Gremio y Personaje se referencian entre si (la serializacion maneja el ciclo).
public abstract class Personaje implements ICombatiente {

    private static final int VIDA_BASE = 100;
    private static final int ATAQUE_BASE = 10;
    private static final int DEFENSA_BASE = 5;
    private static final int CAPACIDAD_INVENTARIO = 3;
    private static final int DANIO_MINIMO = 1;
    private static final double PORCENTAJE_TOTAL = 100.0;

    private String nombre;
    private final TipoPersonaje tipo;
    private int vidaMaxima;
    private int vida;
    private final int ataqueBase;
    private final int defensaBase;
    private EstadoPersonaje estado = EstadoPersonaje.VIVO;

    private Arma arma;                  // asociacion 0..1
    private Gremio gremio;              // asociacion bidireccional 0..1
    private final Inventario<Pocion> inventario = new Inventario<>(CAPACIDAD_INVENTARIO); // composicion
    private final Set<String> habilidades = new HashSet<>();
    private final Map<String, EstadoEfecto> efectos = new LinkedHashMap<>();
    // Hilos dedicados a cada efecto continuo activo
    private final Map<String, Thread> hilosEfectos = new LinkedHashMap<>();
    // Cerrojo de exclusion mutua para proteger secciones criticas de vida y estado
    private final ReentrantLock lock = new ReentrantLock();

    protected Personaje(String nombre, TipoPersonaje tipo) {
        this(nombre, tipo, VIDA_BASE, ATAQUE_BASE, DEFENSA_BASE);
    }

    protected Personaje(String nombre, TipoPersonaje tipo, int vidaMaxima, int ataqueBase, int defensaBase) {
        validarNombre(nombre);
        if (vidaMaxima <= 0 || ataqueBase < 0 || defensaBase < 0) {
            throw new IllegalArgumentException("Estadisticas invalidas para " + nombre);
        }
        this.nombre = nombre;
        this.tipo = tipo;
        this.vidaMaxima = vidaMaxima;
        this.vida = vidaMaxima;
        this.ataqueBase = ataqueBase;
        this.defensaBase = defensaBase;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
    }

    // ---- combate

    /**
     * Ataque especial propio de cada clase.
     * @return descripcion de lo ocurrido, para el registro de combate
     */
    public abstract String realizarAtaqueEspecial(Personaje objetivo) throws SinManaException;

    /** Hook que se ejecuta al comenzar el turno (por ejemplo, el mago regenera mana). */
    public void alIniciarTurno() {
    }

    public int calcularDanio() {
        double bruto = ataqueBase + tipo.getBonificadorAtaque();
        if (arma != null) {
            bruto += arma.getDanioEfectivo();
        }
        return (int) Math.round(bruto);
    }

    @Override
    public void defender(int danoRecibido) {
        // Con enteros, 100 / 123 daria 0: por eso se divide con un double
        double factorMitigacion = PORCENTAJE_TOTAL / (PORCENTAJE_TOTAL + getDefensa());
        int danioFinal = (int) Math.round(danoRecibido * factorMitigacion);
        recibirDanio(Math.max(DANIO_MINIMO, danioFinal));
    }

    // ---- vida

    /**
     * Aplica danio al personaje protegiendo la seccion critica con ReentrantLock.
     *
     * ELECCION DE SYNCHRONIZED (Mecanismo principal visto en la Unidad 4.2):
     * 'synchronized' es el mecanismo principal y recomendado de la unidad porque se apoya en el
     * monitor intrinseco del objeto (this), garantizando exclusion mutua de manera limpia, sin
     * codigo repetitivo y asegurando la liberacion automatica del cerrojo al salir del bloque o metodo,
     * incluso si ocurre una excepcion imprevista. Protege coordinadamente multiples variables dependientes
     * (vida, estado, mapa de efectos) evitando actualizaciones perdidas y lecturas inconsistentes.
     *
     * INVESTIGACION OBLIGATORIA (ReentrantLock vs synchronized):
     * Se reescribe recibirDanio con lock() / unlock() en try-finally segun el punto de investigacion.
     * Se preferiria ReentrantLock sobre synchronized cuando se necesita mayor control:
     * 1) Adquisicion no bloqueante (tryLock()) o con tiempo limite (tryLock(timeout)).
     * 2) Adquisicion de cerrojo de forma interrumpible (lockInterruptibly()).
     * 3) Soporte de politicas de equidad (fairness) y variables de condicion multiples (Condition).
     * ADVERTENCIA: Si el unlock() no esta en el bloque finally, cualquier excepcion intermedia provocaria
     * que el cerrojo quede tomado permanentemente, congelando cualquier otro hilo que intente operar y
     * causando interbloqueos irreparables.
     */
    public void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        lock.lock();
        try {
            setVida(vida - cantidad);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Aplica curacion protegiendo la seccion critica con ReentrantLock.
     *
     * ¿Por que AtomicInteger.addAndGet(-danio) no basta aqui?
     * AtomicInteger solo garantiza atomicidad en una variable simple aislada.
     * En Personaje, cambiar la vida requiere acotarla al rango [0, vidaMaxima] y, ademas,
     * cambiar de forma atomica el 'estado' (si vida == 0 pasa a MUERTO). Por su parte,
     * curar() valida estaVivo() antes de modificar vida. Como son multiples variables
     * dependientes (vida y estado), se requiere exclusion mutua completa para evitar
     * condiciones de carrera.
     *
     * @return vida realmente recuperada
     */
    public int curar(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La curacion no puede ser negativa");
        }
        lock.lock();
        try {
            if (!estaVivo()) {
                return 0;
            }
            int vidaAntes = vida;
            setVida(vida + cantidad);
            return vida - vidaAntes;
        } finally {
            lock.unlock();
        }
    }

    public void restaurarVida() {
        detenerEfectos();
        lock.lock();
        try {
            estado = EstadoPersonaje.VIVO;
            vida = vidaMaxima;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean estaVivo() {
        lock.lock();
        try {
            return estado != EstadoPersonaje.MUERTO;
        } finally {
            lock.unlock();
        }
    }

    // ---- efectos DoT / HoT concurrentes (procesados en hilos continuos)

    public void aplicarEfecto(EstadoEfecto efecto) {
        lock.lock();
        try {
            // Si ya existia un hilo para este efecto, interrumpirlo limpiamente
            Thread previo = hilosEfectos.remove(efecto.getNombre());
            if (previo != null && previo.isAlive()) {
                previo.interrupt();
            }

            EstadoEfecto copia = efecto.copia();
            efectos.put(copia.getNombre(), copia);
            if (copia.esDanio() && estado == EstadoPersonaje.VIVO) {
                estado = EstadoPersonaje.ENVENENADO;
            }

            // Cada efecto corre en su propio hilo independiente
            EfectoContinuoTask task = new EfectoContinuoTask(this, copia, 1000);
            Thread hilo = new Thread(task, "Efecto-" + copia.getNombre() + "-" + nombre);
            hilo.setDaemon(true);
            hilosEfectos.put(copia.getNombre(), hilo);
            hilo.start();
        } finally {
            lock.unlock();
        }
    }

    public boolean quitarEfecto(String nombreEfecto) {
        lock.lock();
        try {
            Thread hilo = hilosEfectos.remove(nombreEfecto);
            if (hilo != null && hilo.isAlive()) {
                hilo.interrupt();
            }
            if (efectos.remove(nombreEfecto) == null) {
                return false;
            }
            normalizarEstado();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void quitarEfectosDanio() {
        lock.lock();
        try {
            for (String nombreEfecto : new ArrayList<>(efectos.keySet())) {
                EstadoEfecto efecto = efectos.get(nombreEfecto);
                if (efecto != null && efecto.esDanio()) {
                    quitarEfecto(nombreEfecto);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Interrumpe todos los hilos de efectos continuos activos y limpia la lista de efectos.
     */
    public void detenerEfectos() {
        lock.lock();
        try {
            for (Thread hilo : hilosEfectos.values()) {
                if (hilo != null && hilo.isAlive()) {
                    hilo.interrupt();
                }
            }
            hilosEfectos.clear();
            efectos.clear();
            normalizarEstado();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Informa del estado de los efectos continuos y limpia aquellos hilos que hayan finalizado.
     * Reemplaza la logica secuencial por turnos por hilos continuos cancelables.
     * @return mensajes para el registro de combate
     */
    public List<String> procesarEfectos() {
        lock.lock();
        try {
            List<String> mensajes = new ArrayList<>();
            Iterator<Map.Entry<String, EstadoEfecto>> iterador = efectos.entrySet().iterator();
            while (iterador.hasNext()) {
                Map.Entry<String, EstadoEfecto> entrada = iterador.next();
                EstadoEfecto efecto = entrada.getValue();
                Thread hilo = hilosEfectos.get(entrada.getKey());
                if (efecto.haTerminado() || (hilo != null && !hilo.isAlive())) {
                    if (hilo != null && hilo.isAlive()) {
                        hilo.interrupt();
                    }
                    iterador.remove();
                    hilosEfectos.remove(entrada.getKey());
                    mensajes.add(nombre + ": termina " + efecto.getNombre());
                } else {
                    mensajes.add(String.format("%s bajo efecto continuo %s (%d ticks restantes)",
                            nombre, efecto.getNombre(), efecto.getTurnosRestantes()));
                }
                if (!estaVivo()) {
                    break;
                }
            }
            normalizarEstado();
            return mensajes;
        } finally {
            lock.unlock();
        }
    }

    private boolean tieneEfectoDanio() {
        lock.lock();
        try {
            for (EstadoEfecto efecto : efectos.values()) {
                if (efecto.esDanio()) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /** Si ya no queda ningun DoT, un personaje ENVENENADO vuelve a estar VIVO. */
    public void normalizarEstado() {
        lock.lock();
        try {
            if (vida == 0) {
                estado = EstadoPersonaje.MUERTO;
                return;
            }
            if (estado == EstadoPersonaje.ENVENENADO && !tieneEfectoDanio()) {
                estado = EstadoPersonaje.VIVO;
            }
        } finally {
            lock.unlock();
        }
    }

    // ---- aturdimiento

    public void aturdir() {
        lock.lock();
        try {
            if (estaVivo()) {
                estado = EstadoPersonaje.ATURDIDO;
            }
        } finally {
            lock.unlock();
        }
    }

    /** @return true si estaba aturdido (y por tanto pierde este turno) */
    public boolean consumirAturdimiento() {
        lock.lock();
        try {
            boolean estabaAturdido = estado == EstadoPersonaje.ATURDIDO;
            if (estabaAturdido) {
                estado = tieneEfectoDanio() ? EstadoPersonaje.ENVENENADO : EstadoPersonaje.VIVO;
            }
            return estabaAturdido;
        } finally {
            lock.unlock();
        }
    }

    // ---- inventario, arma, gremio, habilidades

    public String usarPocion(Pocion pocion) {
        int curado = curar(pocion.getCuracionReal());
        if (pocion.getEfecto() != null) {
            aplicarEfecto(pocion.getEfecto());
        }
        if (pocion.isPurifica()) {
            quitarEfectosDanio();
        }
        return String.format("%s usa %s (+%d de vida)", nombre, pocion.getNombre(), curado);
    }

    public void equiparArma(Arma nuevaArma) {
        this.arma = nuevaArma;
    }

    public void desequiparArma() {
        this.arma = null;
    }

    public void unirseAGremio(Gremio nuevoGremio) {
        if (nuevoGremio == null || nuevoGremio == gremio) {
            return;
        }
        if (gremio != null) {
            gremio.removerMiembro(this);
        }
        gremio = nuevoGremio;
        nuevoGremio.agregarMiembro(this);
    }

    public void abandonarGremio() {
        Gremio anterior = gremio;
        if (anterior == null) {
            return;
        }
        gremio = null;
        anterior.removerMiembro(this);
    }

    /** @return false si la habilidad ya estaba aprendida (el Set rechaza duplicados) */
    public boolean aprenderHabilidad(String habilidad) {
        return habilidades.add(habilidad);
    }

    // ---- getters y setters validados

    public void setVida(int nuevaVida) {
        lock.lock();
        try {
            vida = Math.max(0, Math.min(nuevaVida, vidaMaxima));
            if (vida == 0) {
                estado = EstadoPersonaje.MUERTO;
            }
        } finally {
            lock.unlock();
        }
    }

    public void setNombre(String nombre) {
        validarNombre(nombre);
        this.nombre = nombre;
    }

    public void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser positiva");
        }
        this.vidaMaxima = vidaMaxima;
        this.vida = Math.min(vida, vidaMaxima);
    }

    public int getDefensa() {
        return defensaBase + tipo.getBonificadorDefensa();
    }

    public String getNombre() {
        return nombre;
    }

    public TipoPersonaje getTipo() {
        return tipo;
    }

    public int getVida() {
        lock.lock();
        try {
            return vida;
        } finally {
            lock.unlock();
        }
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    public EstadoPersonaje getEstado() {
        lock.lock();
        try {
            return estado;
        } finally {
            lock.unlock();
        }
    }

    public Arma getArma() {
        return arma;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Inventario<Pocion> getInventario() {
        return inventario;
    }

    public Set<String> getHabilidades() {
        return Collections.unmodifiableSet(habilidades);
    }

    public Collection<EstadoEfecto> getEfectos() {
        lock.lock();
        try {
            return Collections.unmodifiableCollection(new ArrayList<>(efectos.values()));
        } finally {
            lock.unlock();
        }
    }
}
