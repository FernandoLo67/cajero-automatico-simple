package com.fernando.cajero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cuenta bancaria con saldo, PIN e historial de operaciones.
 *
 * <h2>Por que el saldo es {@link BigDecimal} y no {@code double}</h2>
 *
 * Un {@code double} no puede representar 0.10 de forma exacta, porque en binario
 * ese numero es periodico igual que 1/3 lo es en decimal. El error individual es
 * minusculo, pero se acumula operacion tras operacion:
 *
 * <pre>
 * double saldo = 0.0;
 * for (int i = 0; i &lt; 10; i++) saldo += 0.10;
 * // saldo == 0.9999999999999999, y (saldo == 1.00) es false
 * </pre>
 *
 * En pantalla se ve {@code 1.00} porque {@code printf("%.2f")} redondea, asi que
 * el error es invisible hasta que alguien compara saldos, cuadra una caja o
 * calcula un interes. Peor todavia: {@code 100.00 - 33.33 * 3} da
 * {@code 0.010000000000005116}, un saldo que no existe en ninguna moneda.
 *
 * {@code BigDecimal} guarda el numero en base diez, que es la base en la que
 * esta definido el dinero. Es mas lento y mas incomodo de escribir, y es la
 * unica opcion correcta.
 *
 * <h2>El detalle que sorprende: equals contra compareTo</h2>
 *
 * {@code new BigDecimal("1.0").equals(new BigDecimal("1.00"))} devuelve
 * <b>false</b>: {@code equals} compara tambien la escala, y 1.0 tiene una
 * decimal mientras 1.00 tiene dos. Para comparar valores hay que usar
 * {@link BigDecimal#compareTo(BigDecimal)}, y esta clase lo hace en todas sus
 * validaciones.
 *
 * <p>Todos los montos se normalizan a dos decimales con
 * {@link RoundingMode#HALF_UP}, que es el redondeo comercial de toda la vida:
 * 0.005 sube a 0.01.
 */
public class CuentaBancaria {

    /** Decimales de la moneda. */
    private static final int ESCALA = 2;

    /** Redondeo comercial: 0.005 sube a 0.01. */
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final String titular;
    private final int pin;
    private BigDecimal saldo;
    private final List<String> historial = new ArrayList<>();

    /**
     * @param titular      nombre del titular; no puede ser nulo ni estar en blanco
     * @param pin          PIN de acceso
     * @param saldoInicial saldo de apertura; no puede ser negativo
     * @throws IllegalArgumentException si el titular esta vacio o el saldo es negativo
     */
    public CuentaBancaria(String titular, int pin, BigDecimal saldoInicial) {
        if (titular == null || titular.isBlank()) {
            throw new IllegalArgumentException("El titular no puede estar vacio");
        }
        if (saldoInicial == null) {
            throw new IllegalArgumentException("El saldo inicial no puede ser nulo");
        }
        BigDecimal inicial = normalizar(saldoInicial);
        if (inicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo");
        }
        this.titular = titular;
        this.pin = pin;
        this.saldo = inicial;
        if (inicial.compareTo(BigDecimal.ZERO) > 0) {
            historial.add("Deposito inicial: +" + inicial);
        }
    }

    /**
     * Atajo para no escribir {@code new BigDecimal("500.00")} en cada llamada.
     *
     * <p>Recibe el monto como <b>texto</b> y no como {@code double} a proposito:
     * {@code new BigDecimal(0.1)} guarda 0.1000000000000000055511151231257827,
     * porque el {@code double} ya venia mal antes de llegar aqui. El constructor
     * que recibe una cadena es el unico que guarda exactamente lo que se le pide.
     *
     * @param titular      nombre del titular
     * @param pin          PIN de acceso
     * @param saldoInicial saldo de apertura, por ejemplo {@code "500.00"}
     */
    public CuentaBancaria(String titular, int pin, String saldoInicial) {
        this(titular, pin, new BigDecimal(saldoInicial));
    }

    /**
     * @param intento PIN a comprobar
     * @return {@code true} si coincide con el PIN de la cuenta
     */
    public boolean validarPin(int intento) {
        return intento == pin;
    }

    /**
     * Suma un monto al saldo y lo anota en el historial.
     *
     * @param monto cantidad a depositar; tiene que ser mayor que cero
     * @throws IllegalArgumentException si el monto es nulo, cero o negativo
     */
    public void depositar(BigDecimal monto) {
        BigDecimal cantidad = validarMontoPositivo(monto, "depositar");
        saldo = saldo.add(cantidad);
        historial.add("Deposito: +" + cantidad);
    }

    /**
     * Resta un monto del saldo y lo anota en el historial.
     *
     * @param monto cantidad a retirar; tiene que ser mayor que cero y no superar el saldo
     * @throws IllegalArgumentException  si el monto es nulo, cero o negativo
     * @throws SaldoInsuficienteException si el monto supera el saldo disponible
     */
    public void retirar(BigDecimal monto) {
        BigDecimal cantidad = validarMontoPositivo(monto, "retirar");
        // compareTo y no equals: ver la nota de la clase.
        if (cantidad.compareTo(saldo) > 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente. Saldo actual: " + saldo);
        }
        saldo = saldo.subtract(cantidad);
        historial.add("Retiro: -" + cantidad);
    }

    /** @return el saldo actual, siempre con dos decimales */
    public BigDecimal getSaldo() {
        return saldo;
    }

    /** @return el nombre del titular */
    public String getTitular() {
        return titular;
    }

    /**
     * @return el historial de operaciones, en una lista que no se puede modificar
     *         desde afuera
     */
    public List<String> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    private BigDecimal validarMontoPositivo(BigDecimal monto, String operacion) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto a " + operacion + " no puede ser nulo");
        }
        BigDecimal cantidad = normalizar(monto);
        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a " + operacion + " debe ser mayor a 0");
        }
        return cantidad;
    }

    private static BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(ESCALA, REDONDEO);
    }
}
