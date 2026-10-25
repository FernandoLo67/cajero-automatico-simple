# Cajero Automatico Simple

Cajero de consola en Java: autenticacion por PIN con intentos limitados, deposito, retiro, consulta de saldo e historial de movimientos.

Es un proyecto chico, y tiene una decision que no lo es: **el saldo es un `BigDecimal`, no un `double`**.

## Por que el dinero no puede ser un `double`

Un `double` no puede representar `0.10` de forma exacta. En binario ese numero es periodico, igual que 1/3 lo es en decimal. El error individual es minusculo — del orden de 10⁻¹⁷ — pero se acumula operacion tras operacion:

```java
double saldo = 0.0;
for (int i = 0; i < 10; i++) saldo += 0.10;   // diez depositos de diez centavos
```

| | |
|---|---|
| valor real de `saldo` | `0.9999999999999999` |
| lo que muestra la pantalla | `1.00` |
| resultado de `saldo == 1.00` | **`false`** |

La pantalla miente porque `printf("%.2f")` redondea. El error es **invisible** hasta que alguien compara dos saldos, cuadra una caja o calcula un interes.

Y hay casos peores. Cien pesos menos tres retiros de 33.33:

```
resultado con double : 0.010000000000005116
```

Un saldo que no existe en ninguna moneda del mundo.

`BigDecimal` guarda el numero en base diez, que es la base en la que esta definido el dinero. Es mas lento y mas incomodo de escribir. Es la unica opcion correcta.

### El detalle que sorprende: `equals` contra `compareTo`

```java
new BigDecimal("1.0").equals(new BigDecimal("1.00"))   // false
new BigDecimal("1.0").compareTo(new BigDecimal("1.00")) // 0
```

`equals` compara **tambien la escala**: `1.0` tiene un decimal y `1.00` tiene dos, asi que para `equals` son objetos distintos aunque valgan lo mismo. Para comparar valores hay que usar `compareTo`.

Toda validacion de esta clase usa `compareTo`, y hay una prueba que documenta la diferencia — para que nadie tenga que descubrirla depurando.

### Y el otro detalle: nunca construir un `BigDecimal` desde un `double`

```java
new BigDecimal(0.1)      // 0.1000000000000000055511151231257827...
new BigDecimal("0.1")    // 0.1
```

El primero recibe un `double` que **ya venia mal**, y `BigDecimal` lo guarda fielmente con todo su error. Por eso el cajero lee el monto del teclado como **texto** y se lo pasa directo al constructor que recibe una cadena, sin pasar por `Double.parseDouble` en el camino.

## Las pruebas que la version anterior no pasaba

Este proyecto empezo usando `double`, y sus pruebas comparaban saldos asi:

```java
assertEquals(150.0, cuenta.getSaldo(), 0.001);   // <- el delta
```

Ese `0.001` es la senal de alarma. **Un saldo bancario que necesita tolerancia para pasar sus pruebas es un banco que pierde plata.** En la version actual no hay ni un delta en todo el archivo de pruebas, y hay tres que fallarian con `double`:

| prueba | con `double` | con `BigDecimal` |
|---|---|---|
| `diezDepositosDeDiezCentavosDanExactamenteUnPeso` | 0.9999999999999999 | `1.00` |
| `restasSucesivasNoDejanCentavosFantasma` | 0.010000000000005116 | `0.01` |
| `losMontosSeRedondeanAdosDecimalesHaciaArriba` | dependia del formato | `10.01` |

## Las otras decisiones

**`SaldoInsuficienteException` es su propia clase**, y no un `IllegalArgumentException` generico, porque el cajero necesita distinguir dos cosas: *"monto invalido"* es un error de quien escribe, y *"saldo insuficiente"* es informacion para el cliente. Extiende `RuntimeException` y no `Exception` para no obligar a poner `throws` en toda la cadena: quedarse sin saldo no es una condicion excepcional del sistema, es un resultado normal de una operacion.

**El historial se entrega envuelto en `Collections.unmodifiableList`.** Sin eso, cualquiera que reciba la lista del getter puede borrarle una operacion. Hay una prueba que lo comprueba.

**Una operacion rechazada no deja rastro.** Si un retiro falla por falta de fondos, ni el saldo ni el historial cambian. Tambien tiene su prueba, porque es exactamente el tipo de cosa que se rompe cuando alguien reordena dos lineas.

**El redondeo es `HALF_UP`**, el redondeo comercial de toda la vida: `0.005` sube a `0.01`. Java usa `HALF_EVEN` por omision en algunas operaciones, que redondea al par mas cercano y no es lo que espera un contador.

## Transferencias atomicas

`Transferencia` mueve dinero entre dos `CuentaBancaria` de forma atomica: si el deposito en la cuenta destino falla despues de haber retirado de la cuenta origen, se hace rollback automatico. No puede quedar plata en el limbo.

Validaciones antes de tocar ningun saldo:
- Las dos cuentas no pueden ser la misma
- Ninguna puede ser nula
- El monto tiene que ser positivo

Cada transferencia devuelve un `Recibo` inmutable con el monto, las dos cuentas y la marca de tiempo. El recibo se construye solo si la operacion completa fue exitosa — no hay recibos de transferencias fallidas.

## Estructura

```
src/main/java/com/fernando/cajero/
├── CuentaBancaria.java              Saldo, PIN, historial. Toda la logica.
├── Transferencia.java               Movimiento atomico entre dos cuentas con rollback.
├── SaldoInsuficienteException.java  El error que el cajero sabe explicar.
└── Main.java                        Solo lee del teclado y escribe en pantalla.
src/test/java/com/fernando/cajero/
├── CuentaBancariaTest.java          18 pruebas, sin un solo delta.
└── TransferenciaTest.java           10 pruebas.
```

La logica no sabe que existe una consola. Es lo que permite probar `CuentaBancaria` entera sin simular entrada de teclado.

## Como ejecutar

```bash
mvn package
java -jar target/cajero-automatico-simple-1.0.0.jar
```

PIN de la cuenta de ejemplo: `1234`. Tres intentos y la tarjeta se bloquea.

## Como correr las pruebas

```bash
mvn test
```

28 pruebas: operaciones basicas, validaciones, precision decimal, integridad del historial y transferencias atomicas.
