package com.fernando.cajero;

/**
 * Se lanza cuando se intenta retirar mas dinero del que hay en la cuenta.
 *
 * <p>Extiende {@link RuntimeException} y no {@link Exception} a proposito. Una
 * excepcion comprobada obligaria a poner {@code throws} en toda la cadena de
 * llamadas, y quedarse sin saldo no es una condicion excepcional del sistema:
 * es un resultado normal de una operacion, que quien llama decide si atrapa.
 *
 * <p>Es su propia clase y no un {@link IllegalArgumentException} generico
 * porque el cajero necesita distinguirla: "monto invalido" es un error de quien
 * escribe, y "saldo insuficiente" es informacion para el cliente.
 */
public class SaldoInsuficienteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicacion para mostrar al usuario, con el saldo disponible
     */
    public SaldoInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
