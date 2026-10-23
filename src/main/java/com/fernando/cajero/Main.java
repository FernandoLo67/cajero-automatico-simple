package com.fernando.cajero;

import java.math.BigDecimal;
import java.util.Scanner;

/**
 * Menu de consola del cajero.
 *
 * <p>Toda la logica vive en {@link CuentaBancaria}; esta clase solo lee del
 * teclado y escribe en pantalla. Esa separacion es lo que permite que
 * {@code CuentaBancaria} se pruebe entera sin simular una consola.
 */
public class Main {

    private static final int MAX_INTENTOS_PIN = 3;

    public static void main(String[] args) {
        CuentaBancaria cuenta = new CuentaBancaria("Daniel", 1234, "500.00");
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Cajero Automatico Simple ===");
        boolean autenticado = false;
        for (int intento = 1; intento <= MAX_INTENTOS_PIN && !autenticado; intento++) {
            System.out.print("Ingresa tu PIN: ");
            try {
                int pin = Integer.parseInt(scanner.nextLine().trim());
                autenticado = cuenta.validarPin(pin);
                if (!autenticado) {
                    System.out.println("PIN incorrecto. Intento " + intento + "/" + MAX_INTENTOS_PIN);
                }
            } catch (NumberFormatException e) {
                System.out.println("Ingresa solo numeros.");
            }
        }

        if (!autenticado) {
            System.out.println("Demasiados intentos fallidos. Tarjeta bloqueada.");
            scanner.close();
            return;
        }

        System.out.println("Bienvenido, " + cuenta.getTitular() + "!");
        boolean salir = false;
        while (!salir) {
            System.out.println("\n1. Consultar saldo");
            System.out.println("2. Depositar");
            System.out.println("3. Retirar");
            System.out.println("4. Ver historial");
            System.out.println("5. Salir");
            System.out.print("Elige una opcion: ");

            switch (scanner.nextLine().trim()) {
                case "1":
                    System.out.println("Saldo actual: " + cuenta.getSaldo());
                    break;
                case "2":
                    operar(scanner, "depositar", cuenta::depositar, cuenta);
                    break;
                case "3":
                    operar(scanner, "retirar", cuenta::retirar, cuenta);
                    break;
                case "4":
                    if (cuenta.getHistorial().isEmpty()) {
                        System.out.println("(sin movimientos)");
                    } else {
                        cuenta.getHistorial().forEach(System.out::println);
                    }
                    break;
                case "5":
                    salir = true;
                    System.out.println("Gracias por usar el cajero!");
                    break;
                default:
                    System.out.println("Opcion invalida.");
                    break;
            }
        }
        scanner.close();
    }

    /** Pide un monto, ejecuta la operacion y reporta el resultado o el error. */
    private static void operar(Scanner scanner, String nombre,
                               java.util.function.Consumer<BigDecimal> operacion,
                               CuentaBancaria cuenta) {
        BigDecimal monto = leerMonto(scanner);
        if (monto == null) {
            System.out.println("Ingresa un monto numerico valido, por ejemplo 150.50");
            return;
        }
        try {
            operacion.accept(monto);
            System.out.println("Operacion realizada. Nuevo saldo: " + cuenta.getSaldo());
        } catch (IllegalArgumentException | SaldoInsuficienteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Lee el monto como texto y lo pasa directo al constructor de
     * {@link BigDecimal} que recibe una cadena.
     *
     * <p>Convertirlo antes a {@code double} arruinaria el proposito de usar
     * {@code BigDecimal}: el numero ya llegaria con el error de redondeo
     * incorporado.
     *
     * @return el monto, o {@code null} si lo escrito no es un numero valido
     */
    private static BigDecimal leerMonto(Scanner scanner) {
        System.out.print("Monto: ");
        String texto = scanner.nextLine().trim();
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
