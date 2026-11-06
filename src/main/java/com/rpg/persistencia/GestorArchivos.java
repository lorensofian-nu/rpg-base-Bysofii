package com.rpg.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Gestor central de persistencia y manejo de archivos para el Simulador RPG.
 *
 * INVESTIGACION OBLIGATORIA (Taller de archivos):
 * 1) FileWriter vs Files.writeString:
 *    - FileWriter (usado con BufferedWriter) es un flujo de salida continuo que permite escribir
 *      bloques de texto progresivamente en buffer, hacer append eficiente sin cargar todo el archivo
 *      en memoria y realizar flush() controlado antes de cerrar. Ideal para logs y flujos continuos.
 *    - Files.writeString (Java 11+ NIO) escribe una cadena completa de texto al archivo en una sola
 *      llamada atomica de alto nivel, abriendo y cerrando el flujo internamente. Es ideal para archivos
 *      pequenos de configuracion donde el contenido completo ya reside en un unico String en memoria.
 *
 * 2) Seguridad de Serializable:
 *    - No se deben deserializar datos (ObjectInputStream.readObject()) provenientes de fuentes no confiables
 *      o sin validar, ya que la reconstruccion de grafos de objetos puede desencadenar la ejecucion de
 *      secuencias de codigo arbitrarias ("gadget chains") presentes en el classpath o provocar ataques de
 *      denegacion de servicio (consumo excesivo de memoria o CPU al crear estructuras recursivas).
 *    - Alternativas seguras recomendadas: formatos de texto estructurados como JSON (con librerias como
 *      Jackson o Gson configuradas sin tipado polimorfico libre), XML seguro sin entidades externas (XXE),
 *      o formatos binarios con esquemas estrictos como Protocol Buffers (protobuf) o MessagePack.
 */
public class GestorArchivos {

    public static final String CARPETA_PARTIDAS = "partidas";
    public static final String NOMBRE_ARCHIVO_PARTIDA = "partida.dat";
    public static final String NOMBRE_ARCHIVO_LOG = "combates.txt";

    // Objeto candado para sincronizar lectura y escritura entre el hilo principal y el hilo daemon
    private static final Object CANDADO_PERSISTENCIA = new Object();

    private final File directorioBase;

    public GestorArchivos() {
        this(new File(CARPETA_PARTIDAS));
    }

    public GestorArchivos(File directorioBase) {
        this.directorioBase = directorioBase;
        asegurarDirectorio(this.directorioBase);
    }

    /**
     * Asegura la creacion de la carpeta utilizando mkdirs() sin fallar si ya existe.
     */
    public boolean asegurarDirectorio(File directorio) {
        if (!directorio.exists()) {
            return directorio.mkdirs();
        }
        return true;
    }

    /**
     * Devuelve el archivo de partida binaria (.dat) en ruta relativa.
     */
    public File getArchivoPartida() {
        return new File(directorioBase, NOMBRE_ARCHIVO_PARTIDA);
    }

    /**
     * Devuelve el archivo de log en texto (.txt) en ruta relativa.
     */
    public File getArchivoLog() {
        return new File(directorioBase, NOMBRE_ARCHIVO_LOG);
    }

    /**
     * Describe la informacion de un archivo usando exists(), length(), getName() y getAbsolutePath().
     * Explica ademas la diferencia entre ruta relativa y ruta absoluta.
     */
    public String inspeccionarArchivo(File archivo) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Informacion de archivo ---").append(System.lineSeparator());
        sb.append("Nombre: ").append(archivo.getName()).append(System.lineSeparator());
        sb.append("Ruta relativa: ").append(archivo.getPath()).append(System.lineSeparator());
        sb.append("Ruta absoluta: ").append(archivo.getAbsolutePath()).append(System.lineSeparator());
        sb.append("Existe en disco: ").append(archivo.exists() ? "SI" : "NO").append(System.lineSeparator());
        if (archivo.exists()) {
            sb.append("Tamano en bytes: ").append(archivo.length()).append(" B").append(System.lineSeparator());
            sb.append("Es archivo regular: ").append(archivo.isFile() ? "SI" : "NO").append(System.lineSeparator());
        }
        return sb.toString();
    }

    /**
     * Crea un archivo si no existe usando createNewFile() y capturando IOException.
     */
    public boolean crearArchivoSiNoExiste(File archivo) {
        asegurarDirectorio(archivo.getParentFile() != null ? archivo.getParentFile() : directorioBase);
        try {
            return archivo.createNewFile();
        } catch (IOException e) {
            System.out.println("Error de E/S al crear archivo " + archivo.getName() + ": " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // SECCION 2: ESCRITURA DE TEXTO (LOG DE COMBATE)
    // ==========================================

    /**
     * Vuelca el log de combate a un archivo .txt con BufferedWriter y FileWriter.
     * Utiliza modo append para conservar combates anteriores sin sobrescribir.
     * Usa try-with-resources y newLine() para saltos de linea limpios.
     */
    public void volcarLogCombate(File archivo, String contenido, boolean agregar) {
        if (contenido == null || contenido.isBlank()) {
            return;
        }
        asegurarDirectorio(archivo.getParentFile() != null ? archivo.getParentFile() : directorioBase);

        synchronized (CANDADO_PERSISTENCIA) {
            try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo, agregar))) {
                String[] lineas = contenido.split("\\R");
                for (String linea : lineas) {
                    escritor.write(linea);
                    escritor.newLine();
                }
                escritor.flush();
            } catch (IOException e) {
                System.out.println("No se pudo escribir en el log de combate: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // SECCION 3: LECTURA DE TEXTO (LOG DE COMBATE)
    // ==========================================

    /**
     * Lee el log de combate linea por linea usando BufferedReader y FileReader.
     * Captura FileNotFoundException en primer lugar con un mensaje claro sin tildes.
     */
    public String leerLogCombate(File archivo) {
        synchronized (CANDADO_PERSISTENCIA) {
            try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
                StringBuilder sb = new StringBuilder();
                String linea;
                while ((linea = lector.readLine()) != null) {
                    sb.append(linea).append(System.lineSeparator());
                }
                return sb.toString();
            } catch (FileNotFoundException e) {
                System.out.println("El archivo de log no existe todavia: " + archivo.getName());
                return "";
            } catch (IOException e) {
                System.out.println("Error al leer el archivo de log: " + e.getMessage());
                return "";
            }
        }
    }

    // ==========================================
    // SECCION 4: BORRADO DE ARCHIVOS
    // ==========================================

    /**
     * Borra un archivo utilizando File.delete() y reporta el boolean resultante.
     */
    public boolean borrarConFile(File archivo) {
        synchronized (CANDADO_PERSISTENCIA) {
            if (!archivo.exists()) {
                System.out.println("El archivo no existe, no se puede borrar: " + archivo.getName());
                return false;
            }
            boolean borrado = archivo.delete();
            if (borrado) {
                System.out.println("Archivo borrado exitosamente con File.delete(): " + archivo.getName());
            } else {
                System.out.println("No se pudo borrar el archivo con File.delete(): " + archivo.getName());
            }
            return borrado;
        }
    }

    /**
     * Borra un archivo utilizando Files.deleteIfExists(Path).
     * Diferencia con File.delete():
     * Files.deleteIfExists arroja una IOException detallada si ocurre un error del sistema de archivos
     * (permisos, bloqueo por otro proceso), a diferencia de File.delete() que solo retorna false
     * sin explicar la razon del fallo.
     */
    public boolean borrarConFiles(Path ruta) {
        synchronized (CANDADO_PERSISTENCIA) {
            try {
                boolean borrado = Files.deleteIfExists(ruta);
                if (borrado) {
                    System.out.println("Archivo borrado exitosamente con Files.deleteIfExists: " + ruta.getFileName());
                } else {
                    System.out.println("El archivo no existia para borrar: " + ruta.getFileName());
                }
                return borrado;
            } catch (IOException e) {
                System.out.println("Error de E/S al borrar con Files.deleteIfExists: " + e.getMessage());
                return false;
            }
        }
    }

    // ==========================================
    // SECCION 5: SERIALIZACION BINARIA (.DAT)
    // ==========================================

    /**
     * Guarda la partida en disco serializando el EstadoPartida con ObjectOutputStream.
     * Utiliza un archivo temporal previo para garantizar atomicidad y evitar que un cierre
     * abrupto o una interrupcion del daemon deje un archivo corrupto a medio escribir.
     * Sincronizado para proteger contra accesos concurrentes de hilos.
     */
    public boolean guardarPartida(File archivoDestino, EstadoPartida estado) {
        if (estado == null) {
            System.out.println("No hay estado de partida para guardar.");
            return false;
        }
        asegurarDirectorio(archivoDestino.getParentFile() != null ? archivoDestino.getParentFile() : directorioBase);

        File archivoTemporal = new File(archivoDestino.getParentFile(), archivoDestino.getName() + ".tmp");

        synchronized (CANDADO_PERSISTENCIA) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(archivoTemporal))) {
                oos.writeObject(estado);
                oos.flush();
            } catch (NotSerializableException e) {
                System.out.println("Error critico: objeto no serializable en el estado: " + e.getMessage());
                archivoTemporal.delete();
                return false;
            } catch (IOException e) {
                System.out.println("Error de E/S al serializar la partida: " + e.getMessage());
                archivoTemporal.delete();
                return false;
            }

            // Reemplazo atomico del archivo final
            try {
                Files.move(archivoTemporal.toPath(), archivoDestino.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return true;
            } catch (IOException e) {
                // Si el sistema de archivos no soporta movimiento atomico, usar reemplazo estandar
                try {
                    Files.move(archivoTemporal.toPath(), archivoDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return true;
                } catch (IOException ex) {
                    System.out.println("Error al consolidar archivo de partida: " + ex.getMessage());
                    return false;
                }
            }
        }
    }

    /**
     * Carga y deserializa la partida desde disco usando ObjectInputStream.
     * Retorna null si el archivo no existe o ocurre un error controlado.
     */
    public EstadoPartida cargarPartida(File archivo) {
        synchronized (CANDADO_PERSISTENCIA) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(archivo))) {
                Object obj = ois.readObject();
                if (obj instanceof EstadoPartida estado) {
                    return estado;
                } else {
                    System.out.println("El archivo contiene un objeto de tipo incompatible.");
                    return null;
                }
            } catch (FileNotFoundException e) {
                System.out.println("No se encontro el archivo de partida guardada: " + archivo.getName());
                return null;
            } catch (ClassNotFoundException e) {
                System.out.println("Error: la clase de partida guardada no coincide con la version actual: " + e.getMessage());
                return null;
            } catch (IOException e) {
                System.out.println("El archivo de partida esta corrupto o no se puede leer: " + e.getMessage());
                return null;
            }
        }
    }

    // ==========================================
    // DEMOSTRACION / AUTO-EVALUACION DEL TALLER
    // ==========================================

    /**
     * Ejecuta una comprobacion integral de cada punto del taller de persistencia.
     */
    public static void main(String[] args) {
        System.out.println("=== COMPROBACION TALLER DE PERSISTENCIA Y ARCHIVOS ===");

        GestorArchivos gestor = new GestorArchivos();

        // 1. Ruta relativa vs ruta absoluta
        File relativo = new File("partidas/demo.txt");
        File absoluto = new File(relativo.getAbsolutePath());
        System.out.println("[OK] Ruta relativa: " + relativo.getPath());
        System.out.println("[OK] Ruta absoluta: " + absoluto.getAbsolutePath());

        // 2. mkdirs() y createNewFile()
        gestor.asegurarDirectorio(new File("partidas"));
        gestor.crearArchivoSiNoExiste(relativo);

        // 3. exists(), length(), getName()
        System.out.print(gestor.inspeccionarArchivo(relativo));

        // 4. Escritura con BufferedWriter (append y newLine)
        gestor.volcarLogCombate(relativo, "Combate 1: Victoria de Heroe", false);
        gestor.volcarLogCombate(relativo, "Combate 2: Victoria contra Goblin", true);

        // 5. Lectura con BufferedReader
        String leido = gestor.leerLogCombate(relativo);
        System.out.println("[OK] Contenido leido:\n" + leido.trim());

        // 6. Borrado con File.delete() y Files.deleteIfExists()
        boolean borradoFile = gestor.borrarConFile(relativo);
        System.out.println("[OK] Archivo demo borrado: " + borradoFile);

        // 7. Prueba de captura de FileNotFoundException
        gestor.leerLogCombate(new File("partidas/archivo_fantasma.txt"));

        // 8. Demostracion de NotSerializableException capturada
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("partidas/objeto_invalido.dat"))) {
            oos.writeObject(new Object()); // Object no es Serializable
        } catch (NotSerializableException e) {
            System.out.println("[OK] NotSerializableException capturada y controlada como esperado: " + e.getMessage());
            new File("partidas/objeto_invalido.dat").delete();
        } catch (IOException ignored) {}

        System.out.println("=== FIN DE LA COMPROBACION DE PERSISTENCIA ===");
    }
}
