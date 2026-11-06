package com.rpg.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/** Imprime cada evento del combate y lo guarda para incluirlo en el guardado de la partida. */
public class RegistroCombate {

    private final StringBuilder texto = new StringBuilder();

    public synchronized void agregar(String linea) {
        System.out.println(linea);
        texto.append(linea).append(System.lineSeparator());
    }

    public synchronized void agregar(String formato, Object... argumentos) {
        agregar(String.format(formato, argumentos));
    }

    public synchronized void limpiar() {
        texto.setLength(0);
    }

    public synchronized String getTexto() {
        return texto.toString();
    }

    /**
     * Vuelca el registro de combate a un archivo .txt con BufferedWriter y FileWriter.
     * Soporta modo append (agregar = true) para preservar combates previos o sobrescribir (agregar = false).
     * Utiliza try-with-resources y newLine().
     */
    public synchronized void guardarEnArchivo(File archivo, boolean agregar) {
        if (texto.length() == 0) {
            return;
        }
        if (archivo.getParentFile() != null && !archivo.getParentFile().exists()) {
            archivo.getParentFile().mkdirs();
        }
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo, agregar))) {
            String[] lineas = texto.toString().split("\\R");
            for (String linea : lineas) {
                escritor.write(linea);
                escritor.newLine();
            }
            escritor.flush();
        } catch (IOException e) {
            System.out.println("Error al guardar registro en archivo: " + e.getMessage());
        }
    }

    /**
     * Lee un registro de combate desde disco usando BufferedReader y FileReader.
     * Captura FileNotFoundException de forma especifica informando al usuario sin tildes.
     */
    public static String leerDeArchivo(File archivo) {
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = lector.readLine()) != null) {
                sb.append(linea).append(System.lineSeparator());
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            System.out.println("El archivo no existe todavia: " + archivo.getName());
            return "";
        } catch (IOException e) {
            System.out.println("Error al leer archivo de registro: " + e.getMessage());
            return "";
        }
    }
}
