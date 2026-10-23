package com.fernando.cajero;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de {@link CuentaBancaria}.
 *
 * <p>No hay ningun delta de tolerancia en este archivo, y esa ausencia es
 * deliberada. La version anterior de estas pruebas comparaba saldos con
 * {@code assertEquals(150.0, saldo, 0.001)}, y ese 0.001 estaba tapando
 * exactamente el error que las tres pruebas de precision de aqui abajo
 * demuestran. Un saldo bancario que necesita tolerancia para pasar sus pruebas
 * es un banco que pierde plata.
 */
class CuentaBancariaTest {

    private CuentaBancaria cuentaCon(String saldo) {
        return new CuentaBancaria("Ana", 1111, saldo);
    }

    // ---------------------------------------------------------------
    //  Operaciones basicas
    // ---------------------------------------------------------------

    @Test
    void depositarAumentaElSaldoYQuedaEnHistorial() {
        CuentaBancaria cuenta = cuentaCon("100.00");
        cuenta.depositar(new BigDecimal("50.00"));
        assertEquals(0, cuenta.getSaldo().compareTo(new BigDecimal("150.00")));
        assertTrue(cuenta.getHistorial().stream().anyMatch(h -> h.contains("Deposito: +50.00")));
    }

    @Test
    void retirarDisminuyeElSaldoCuandoHayFondos() {
        CuentaBancaria cuenta = cuentaCon("100.00");
        cuenta.retirar(new BigDecimal("30.00"));
        assertEquals(0, cuenta.getSaldo().compareTo(new BigDecimal("70.00")));
    }

    @Test
    void retirarMasDelSaldoLanzaSaldoInsuficiente() {
        CuentaBancaria cuenta = cuentaCon("100.00");
        assertThrows(SaldoInsuficienteException.class, () -> cuenta.retirar(new BigDecimal("200.00")));
    }

    @Test
    void retirarExactamenteTodoElSaldoDejaLaCuentaEnCero() {
        CuentaBancaria cuenta = cuentaCon("100.00");
        cuenta.retirar(new BigDecimal("100.00"));
        assertEquals(0, cuenta.getSaldo().compareTo(BigDecimal.ZERO));
    }

    @Test
    void depositarMontoNegativoLanzaExcepcion() {
        CuentaBancaria cuenta = cuentaCon("100.00");
        assertThrows(IllegalArgumentException.class, () -> cuenta.depositar(new BigDecimal("-10.00")));
    }

    @Test
    void retirarMontoCeroLanzaExcepcion() {
        CuentaBancaria cuenta = cuentaCon("100.00");
        assertThrows(IllegalArgumentException.class, () -> cuenta.retirar(BigDecimal.ZERO));
    }

    @Test
    void montoNuloLanzaExcepcion() {
        CuentaBancaria cuenta = cuentaCon("100.00");
        assertThrows(IllegalArgumentException.class, () -> cuenta.depositar(null));
        assertThrows(IllegalArgumentException.class, () -> cuenta.retirar(null));
    }

    @Test
    void validarPinDevuelveTrueSoloConElPinCorrecto() {
        CuentaBancaria cuenta = cuentaCon("100.00");
        assertTrue(cuenta.validarPin(1111));
        assertFalse(cuenta.validarPin(9999));
    }

    @Test
    void saldoInicialNegativoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new CuentaBancaria("Ana", 1111, "-5.00"));
    }

    @Test
    void titularVacioLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new CuentaBancaria("   ", 1111, "100.00"));
    }

    // ---------------------------------------------------------------
    //  Precision: las pruebas que la version con double no pasaba
    // ---------------------------------------------------------------

    /**
     * Diez depositos de diez centavos tienen que dar exactamente un peso.
     *
     * <p>Con {@code double} el saldo queda en 0.9999999999999999 y esta prueba
     * falla. Con {@code BigDecimal} da 1.00 exacto.
     */
    @Test
    void diezDepositosDeDiezCentavosDanExactamenteUnPeso() {
        CuentaBancaria cuenta = cuentaCon("0.00");
        for (int i = 0; i < 10; i++) {
            cuenta.depositar(new BigDecimal("0.10"));
        }
        assertEquals(0, cuenta.getSaldo().compareTo(new BigDecimal("1.00")));
    }

    /**
     * Cien pesos menos tres retiros de 33.33 tienen que dejar exactamente un
     * centavo.
     *
     * <p>Con {@code double} quedaban 0.010000000000005116: un saldo que no
     * existe en ninguna moneda.
     */
    @Test
    void restasSucesivasNoDejanCentavosFantasma() {
        CuentaBancaria cuenta = cuentaCon("100.00");
        for (int i = 0; i < 3; i++) {
            cuenta.retirar(new BigDecimal("33.33"));
        }
        assertEquals(0, cuenta.getSaldo().compareTo(new BigDecimal("0.01")));
        assertEquals("0.01", cuenta.getSaldo().toString());
    }

    /**
     * El saldo siempre tiene dos decimales, aunque el monto venga con mas.
     * 10.005 redondea a 10.01 (HALF_UP, el redondeo comercial).
     */
    @Test
    void losMontosSeRedondeanAdosDecimalesHaciaArriba() {
        CuentaBancaria cuenta = cuentaCon("0.00");
        cuenta.depositar(new BigDecimal("10.005"));
        assertEquals("10.01", cuenta.getSaldo().toString());
    }

    /**
     * La trampa clasica de {@link BigDecimal}: {@code equals} compara la escala
     * y {@code compareTo} compara el valor. Esta prueba documenta la diferencia
     * para que nadie la descubra depurando.
     */
    @Test
    void equalsCompararaLaEscalaYcompareToElValor() {
        BigDecimal unPeso = new BigDecimal("1.0");
        BigDecimal unPesoConDosDecimales = new BigDecimal("1.00");

        assertNotEquals(unPeso, unPesoConDosDecimales);       // distinta escala
        assertEquals(0, unPeso.compareTo(unPesoConDosDecimales)); // mismo valor
    }

    // ---------------------------------------------------------------
    //  Historial
    // ---------------------------------------------------------------

    @Test
    void elHistorialRegistraTodasLasOperacionesEnOrden() {
        CuentaBancaria cuenta = cuentaCon("100.00");
        cuenta.depositar(new BigDecimal("50.00"));
        cuenta.retirar(new BigDecimal("20.00"));

        assertEquals(3, cuenta.getHistorial().size());
        assertTrue(cuenta.getHistorial().get(0).contains("Deposito inicial"));
        assertTrue(cuenta.getHistorial().get(1).contains("Deposito: +50.00"));
        assertTrue(cuenta.getHistorial().get(2).contains("Retiro: -20.00"));
    }

    @Test
    void unaCuentaAbiertaEnCeroNoRegistraDepositoInicial() {
        CuentaBancaria cuenta = cuentaCon("0.00");
        assertTrue(cuenta.getHistorial().isEmpty());
    }

    /**
     * El historial que se entrega afuera no se puede modificar. Sin esto,
     * cualquiera podria borrar una operacion de la lista que devuelve el getter.
     */
    @Test
    void elHistorialQueSeEntregaNoSePuedeModificar() {
        CuentaBancaria cuenta = cuentaCon("100.00");
        assertThrows(UnsupportedOperationException.class,
                () -> cuenta.getHistorial().add("Retiro fraudulento: -100.00"));
    }

    @Test
    void unaOperacionRechazadaNoDejaRastroEnElHistorial() {
        CuentaBancaria cuenta = cuentaCon("100.00");
        int antes = cuenta.getHistorial().size();
        assertThrows(SaldoInsuficienteException.class, () -> cuenta.retirar(new BigDecimal("999.00")));
        assertEquals(antes, cuenta.getHistorial().size());
        assertEquals(0, cuenta.getSaldo().compareTo(new BigDecimal("100.00")));
    }
}
