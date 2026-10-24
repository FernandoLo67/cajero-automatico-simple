package com.fernando.cajero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Transferencia atomica entre dos cuentas.
 *
 * <p>Si el retiro falla (saldo insuficiente), el deposito no se ejecuta.
 * Si el deposito falla por alguna razon inesperada, el retiro se revierte
 * para no perder dinero en el camino.
 *
 * <p>Registra un recibo inmutable con los datos de la operacion para
 * trazabilidad.
 */
public final class Transferencia {

    private Transferencia() {
    }

    /**
     * Transfiere un monto de una cuenta a otra.
     *
     * @param origen  cuenta que envia
     * @param destino cuenta que recibe
     * @param monto   cantidad a transferir, mayor que cero
     * @return recibo de la operacion
     * @throws IllegalArgumentException      si alguna cuenta es nula, son la misma,
     *                                       o el monto no es positivo
     * @throws SaldoInsuficienteException    si el origen no tiene fondos suficientes
     */
    public static Recibo ejecutar(CuentaBancaria origen, CuentaBancaria destino, BigDecimal monto) {
        validar(origen, destino, monto);

        origen.retirar(monto);
        try {
            destino.depositar(monto);
        } catch (Exception e) {
            origen.depositar(monto);
            throw new IllegalStateException("Transferencia revertida: fallo el deposito en destino", e);
        }

        return new Recibo(origen.getTitular(), destino.getTitular(), monto, LocalDateTime.now());
    }

    private static void validar(CuentaBancaria origen, CuentaBancaria destino, BigDecimal monto) {
        Objects.requireNonNull(origen, "La cuenta origen no puede ser nula");
        Objects.requireNonNull(destino, "La cuenta destino no puede ser nula");
        if (origen == destino) {
            throw new IllegalArgumentException("No se puede transferir a la misma cuenta");
        }
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }
    }

    /**
     * Recibo inmutable de una transferencia completada.
     */
    public static final class Recibo {

        private final String origen;
        private final String destino;
        private final BigDecimal monto;
        private final LocalDateTime fecha;

        public Recibo(String origen, String destino, BigDecimal monto, LocalDateTime fecha) {
            this.origen = origen;
            this.destino = destino;
            this.monto = monto;
            this.fecha = fecha;
        }

        public String origen() { return origen; }
        public String destino() { return destino; }
        public BigDecimal monto() { return monto; }
        public LocalDateTime fecha() { return fecha; }

        @Override
        public String toString() {
            return String.format("Transferencia: %s -> %s por %s el %s",
                    origen, destino, monto.toPlainString(), fecha);
        }
    }
}
