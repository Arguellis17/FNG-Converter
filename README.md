# FNG Converter — Microproyecto #2

Aplicativo de escritorio (Java 25, JavaFX y Maven) que manipula Gramáticas
Libres de Contexto y muestra cada transformación paso a paso:

- **Microproyecto #1:** conversión de una GLC a Forma Normal de Chomsky
  (FNC, variante del curso).
- **Microproyecto #2:** conversión a Forma Normal de Greibach (FNG).

Universidad Francisco de Paula Santander — Teoría de la Computación.

## Requisitos

- JDK 25
- Apache Maven 3.9 o superior
- JavaFX se descarga automáticamente con Maven (`org.openjfx`)

## Ejecución

```bash
mvn compile          # Compilar
mvn test             # Pruebas unitarias (136, JUnit 5)
mvn javafx:run       # Interfaz gráfica
```

## Uso

1. Escribe la gramática en los cuatro campos: variables, terminales,
   símbolo inicial (obligatorio) y producciones. Si dejas vacíos los
   campos de variables y terminales, se infieren de las producciones.
2. Reglas de escritura:
   - Un carácter por símbolo. Una letra mayúscula es una variable y
     cualquier otro carácter es un terminal.
   - Formato de producciones: `A -> aB | b`, una por línea.
   - Para la cadena vacía escribe `ε` o `epsilon`.
3. Elige el destino en el selector: **FNC (Chomsky)** o **FNG (Greibach)**.
4. Pulsa **Validar** para comprobar la gramática, o **Ejemplo** para cargar
   una de muestra. **Limpiar** borra los campos.
5. Ejecuta la conversión:
   - **Proceso completo** (F5): muestra todos los pasos de una vez.
   - **Ejecutar el paso a paso** (F10) y **Siguiente paso**: avanzas
     manualmente.
6. En el historial, selecciona un paso para ver su detalle: qué se hizo,
   las producciones eliminadas y agregadas, y la gramática resultante.

## Qué hace cada pipeline

### FNC (pasos 0 a 6)

| # | Paso | Qué hace |
|---|------|----------|
| 0 | Definición de gramática | Muestra la gramática y declara los símbolos que faltaban |
| 1 | Variables inútiles | Elimina variables que no generan cadenas de terminales |
| 2 | Variables inalcanzables | Elimina variables a las que no se llega desde el símbolo inicial |
| 3 | Producciones unitarias | Elimina producciones `A -> B` |
| 4 | Producciones nulas | Elimina producciones ε |
| 5 | Forma Normal de Chomsky | Parte las producciones de más de 2 símbolos con variables `Xn` |
| 6 | Organización | Muestra la gramática final completa |

En la variante del curso los terminales no se reemplazan por variables.

### FNG (pasos 0 a 13)

Primero se ejecutan los pasos 0 a 6 y después:

| # | Paso | Qué hace |
|---|------|----------|
| 7 | Validación de FNC | Comprueba la FNC del curso y aparta `S -> ε` |
| 8 | Terminales a variables | Reemplaza cada terminal dentro de una producción de 2 símbolos por `Tt` (con `Tt -> t`) |
| 9 | Ordenamiento de variables | `S` primero, luego por orden de aparición; las `Tt` al final |
| 10 | Recursividad izquierda | Sustituye variables anteriores y elimina la recursividad inmediata creando `V.1` |
| 11 | Sustitución hacia atrás | Hace que toda producción empiece con un terminal |
| 12 | Variables inalcanzables | Elimina las que quedaron sin uso |
| 13 | Validación final | Comprueba la forma `A -> a A1…An` y reincorpora `S -> ε` si corresponde |

Variables auxiliares que aparecen en pantalla: `Xn` (FNC), `Tt` (una por
terminal) y `V.1` (una por variable con recursividad inmediata).

## Ejemplo

Entrada, con destino FNG:

```text
S -> Aa | b
A -> Sc | d
```

`S ⇒ Aa ⇒ Sca` es recursividad izquierda indirecta. Al sustituir `S` en `A`
aparece `A -> Aac`, recursividad inmediata, que el paso 10 elimina.
El proceso tiene 15 pasos y la gramática final es:

```text
S   → b Tc Ta | d Ta | b Tc A.1 Ta | d A.1 Ta | b
Ta  → a
Tc  → c
A.1 → a Tc | a Tc A.1
```

Genera el lenguaje (b | da)(ca)*.

## Estructura

```text
src/main/java/com/fnc/
├── Main.java        # Punto de entrada JavaFX
├── model/           # Grammar, Production, TransformationStep y formateadores
├── engine/          # Motor: pipeline FNC (GrammarService) y FNG (GreibachService)
└── ui/              # Interfaz JavaFX
src/main/resources/styles.css
src/test/java/com/fnc/   # Pruebas JUnit 5
```

## Limitaciones conocidas

- La entrada admite un carácter por símbolo; los nombres compuestos
  (`X1`, `T1`, `A.1`) solo los genera el programa.
- Si la gramática no está en FNC, se normaliza automáticamente en vez de
  rechazarla.
- Los símbolos usados pero no declarados se declaran solos y se avisa.
- `S -> ε`: en FNG solo se reincorpora si `S` no aparece en ningún lado
  derecho; si aparece, el lenguaje es equivalente salvo la cadena vacía.
- No hay JAR autónomo: se ejecuta con `mvn javafx:run`.

## Entregables

- [x] Código fuente funcional y documentado
- [X] Documento técnico
- [X] Manual de usuario

## Equipo

- Juan Arguello — motor de transformación
- Miguel Romero — interfaz gráfica

Ramas: `main` (versión estable) y `develop` (desarrollo); los cambios nuevos
se hacen en ramas `feature/*`.
