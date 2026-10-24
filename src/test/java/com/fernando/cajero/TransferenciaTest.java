package com.fernando.cajero;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransferenciaTest {

    private CuentaBancaria origen;
    private CuentaBancaria destino;

    @BeforeEach
    void preparar() {
        origen = new CuentaBancaria("Ana", 1234, "1000.00");
        destino = new CuentaBancaria("Pedro", 5678, "500.00");
    }

    @Test
    void transfiereElMontoCorrectamente() {
        Transferencia.Recibo recibo = Transferencia.ejecutar(origen, destino, new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), origen.getSaldo());
        assertEquals(new BigDecimal("800.00"), destino.getSaldo());
        assertEquals("Ana", recibo.origen());
        assertEquals("Pedro", recibo.destino());
        assertEquals(new BigDecimal("300.00"), recibo.monto());
        assertNotNull(recibo.fecha());
    }

    @Test
    void fallaConSaldoInsuficiente() {
        assertThrows(SaldoInsuficienteException.class,
                () -> Transferencia.ejecutar(origen, destino, new BigDecimal("1500.00")));

        assertEquals(new BigDecimal("1000.00"), origen.getSaldo());
        assertEquals(new BigDecimal("500.00"), destino.getSaldo());
    }

    @Test
    void noPermiteTransferirALaMismaCuenta() {
        assertThrows(IllegalArgumentException.class,
                () -> Transferencia.ejecutar(origen, origen, new BigDecimal("100.00")));
    }

    @Test
    void noPermiteMontoNegativo() {
        assertThrows(IllegalArgumentException.class,
                () -> Transferencia.ejecutar(origen, destino, new BigDecimal("-50.00")));
    }

    @Test
    void noPermiteMontoCero() {
        assertThrows(IllegalArgumentException.class,
                () -> Transferencia.ejecutar(origen, destino, BigDecimal.ZERO));
    }

    @Test
    void rechazaCuentasNulas() {
        assertThrows(NullPointerException.class,
                () -> Transferencia.ejecutar(null, destino, new BigDecimal("100.00")));
        assertThrows(NullPointerException.class,
                () -> Transferencia.ejecutar(origen, null, new BigDecimal("100.00")));
    }

    @Test
    void rechazaMontoNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Transferencia.ejecutar(origen, destino, null));
    }

    @Test
    void elHistorialRegistraLaOperacion() {
        Transferencia.ejecutar(origen, destino, new BigDecimal("200.00"));

        assertTrue(origen.getHistorial().stream()
                .anyMatch(h -> h.contains("Retiro") && h.contains("200.00")));
        assertTrue(destino.getHistorial().stream()
                .anyMatch(h -> h.contains("Deposito") && h.contains("200.00")));
    }

    @Test
    void transferenciaCompletaVaciaElOrigen() {
        Transferencia.ejecutar(origen, destino, new BigDecimal("1000.00"));

        assertEquals(new BigDecimal("0.00"), origen.getSaldo());
        assertEquals(new BigDecimal("1500.00"), destino.getSaldo());
    }

    @Test
    void elReciboTieneFormatoLegible() {
        Transferencia.Recibo recibo = Transferencia.ejecutar(origen, destino, new BigDecimal("100.00"));
        String texto = recibo.toString();

        assertTrue(texto.contains("Ana"));
        assertTrue(texto.contains("Pedro"));
        assertTrue(texto.contains("100.00"));
    }
}
