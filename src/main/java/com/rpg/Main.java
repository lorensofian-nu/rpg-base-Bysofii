package com.rpg;

import com.rpg.view.HeroeUI;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Punto de entrada principal del Simulador RPG.
 * Regla de arquitectura MVC: Main unicamente arranca la aplicacion invocando a la View (HeroeUI).
 */
public class Main {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            new HeroeUI(scanner).iniciar();
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        }
    }
}
