# FNC Converter

Aplicativo para depuración y conversión de Gramáticas Libres de Contexto (GLC)
a Forma Normal de Chomsky (FNC), con visualización paso a paso.

Teoría de la Computación — Microproyecto #1.

## Descripción

El aplicativo permite:

1. Ingresar una Gramática Libre de Contexto G = (V, T, P, S).
2. Validar sus componentes antes de transformar.
3. Ejecutar la depuración y transformación paso a paso o en proceso completo.
4. Obtener una gramática equivalente en Forma Normal de Chomsky (FNC).

## Proceso de transformación (orden del archivo Entrenamiento Algoritmo)

0. Definición de gramática.
1. Eliminación de variables inútiles (no generadoras, incluye no definidas).
2. Eliminación de variables inalcanzables (desde S).
3. Eliminación de producciones unitarias (A → B).
4. Eliminación de producciones nulas (ε-producciones, una por una).
5. Conversión a FNC: partición de producciones de más de 2 símbolos
   con Xn nuevas sin reutilizar (los terminales no se reemplazan).
6. Organización gramática (resultado final).

Cada etapa muestra la gramática antes y después, con las producciones
eliminadas y agregadas (historial completo).

## Requisitos

- JDK 25 o superior (probado con JDK 26).
- Apache Maven 3.9+ (o usar el plugin de Maven integrado de IntelliJ).
- IntelliJ IDEA (recomendado).
- Las dependencias de JavaFX se descargan automáticamente vía Maven
  (`org.openjfx`, versión 25).

## Estructura del proyecto

```text
src/main/java/com/fnc/
├── Main.java                 # Punto de entrada JavaFX (+ atajos F5/F10)
├── model/
│   ├── Grammar.java          # G = (V, T, P, S)
│   ├── Production.java       # Regla de producción
│   ├── GrammarFormatter.java # Formato G={...}, Sigma agrupado y tachado
│   └── TransformationStep.java # Historial antes/después + detalles
├── engine/                   # Lógica de transformación
│   ├── GrammarService.java   # Pipeline completo (pasos 0-6)
│   ├── GrammarValidator.java
│   ├── NullProductionEliminator.java
│   ├── UnitProductionEliminator.java
│   ├── UselessVariableEliminator.java
│   ├── UnreachableVariableEliminator.java
│   └── ChomskyNormalFormConverter.java
├── ui/                       # Interfaz gráfica JavaFX
│   ├── MainFrame.java        # Ventana, menú y modos
│   ├── GrammarInputPanel.java# Formulario G = (V, T, P, S) con autocompletado
│   └── TransformationPanel.java # Historial + detalle con diffs visuales
├── src/main/resources/
│   └── styles.css            # Estilos JavaFX
└── src/test/java/com/fnc/engine/ # Pruebas unitarias JUnit 5
```

## Ejecución

```bash
# Compilar
mvn compile

# Correr pruebas unitarias (JUnit 5, 39 tests)
mvn test

# Ejecutar la interfaz gráfica
mvn javafx:run
```

En IntelliJ también se puede crear una configuración `Application`
con `Main class: com.fnc.Main` (ver `.idea/runConfigurations/`).

## Formato de entrada

- Cada caracter es un símbolo: los espacios y comas son opcionales.
- Variables y terminales: Ej: `S, A, B` o `SAB`.
- Los símbolos usados pero no declarados no bloquean: se asumen
  automáticamente (mayúscula → variable, otro → terminal) y el proceso
  los clasifica (ej: una variable sin producciones sale en inútiles).
- Producciones: una por línea, con `->` o `→` y alternativas con `|`. Ej:

```text
S -> AB | B
A -> aA | a | ε
B -> bB | b
```

## Uso de la interfaz

- Al escribir producciones, los campos V y T se pueblan automáticamente
  (mayúscula → variable, otro → terminal) y la gramática se valida en
  segundo plano (~500 ms); un borde rojo indica error de sintaxis.
- Botones (a la derecha de la lista de pasos):
  - **▶ Proceso completo** (atajo `F5`): ejecuta todas las etapas de una vez.
  - **Ejecutar el paso a paso** (atajo `F10`): muestra solo el primer paso;
    se habilita únicamente cuando la gramática es válida.
  - **Continuar**: revela el siguiente paso hasta la gramática final.
- El historial es interactivo: seleccionar un paso anterior muestra el
  estado exacto de la gramática en ese momento, con diff visual
  (eliminadas en rojo tachado, agregadas en verde) y tarjeta final.

## Ramas de trabajo

- `master`: rama protegida (requiere Pull Request + 1 aprobación).
- `feature/engine-logic`: motor de transformación (mergeado vía PR #1).
- `feature/ui`: interfaz gráfica JavaFX.

Flujo: trabajar en ramas `feature/*`, abrir PR hacia `master`,
revisar/aprobar y mergear. No hacer push directo a `master`.

## Entregables del microproyecto

- [x] Código fuente funcional (motor + interfaz).

## Equipo

- Juan Arguello (Arguellis17) — motor de transformación (`engine`).
- Miguel Romero (miguelRomero1025) — interfaz gráfica (`ui`).
