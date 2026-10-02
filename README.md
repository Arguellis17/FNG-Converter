# FNG Converter — Microproyecto #2

Software en Java que convierte una Gramática Libre de Contexto expresada en
**Forma Normal de Chomsky (FNC)** en una gramática equivalente expresada en
**Forma Normal de Greibach (FNG)**, mostrando cada transformación paso a paso.

Teoría de la Computación — Microproyecto #2.

> Este README se actualiza a medida que el proyecto avanza.

## Pipeline

| # | Etapa | Qué hace |
|---|-------|----------|
| 1 | Validación FNC | Verifica que toda producción sea `A -> BC` o `A -> a` |
| 2 | Ordenamiento de variables | Ordena V para el proceso de sustitución |
| 3 | Sustitución | Sustituye las producciones que requieren reescritura |
| 4 | Recursividad izquierda | Detecta y elimina recursividad izquierda directa |
| 5 | Conversión a FNG | Toda producción inicia con un terminal |
| 6 | Validación FNG | Confirma la forma normal de Greibach |

Cada etapa expone la gramática resultante y las diferencias aplicadas.

## Requisitos

- JDK 25 o superior
- Apache Maven 3.9+
- JavaFX se descarga vía Maven (`org.openjfx`)

## Ejecución

```bash
mvn compile          # Compilar
mvn test             # Pruebas unitarias (JUnit 5)
mvn javafx:run       # Interfaz gráfica
```

## Estructura

```text
src/main/java/com/fnc/
├── Main.java              # Punto de entrada JavaFX
├── model/                 # Grammar, Production, TransformationStep, GrammarFormatter
├── engine/                # Motor de transformación
└── ui/                    # Interfaz JavaFX
src/main/resources/
└── styles.css
src/test/java/com/fnc/     # Pruebas unitarias JUnit 5
```

## Modos de funcionamiento

- **Automático** — ejecuta toda la conversión y muestra todos los pasos.
- **Paso a paso** — el usuario avanza manualmente y verifica cada transformación.

## Entregables

- [ ] Código fuente funcional y documentado
- [ ] Documento técnico
- [ ] Manual de usuario

## Equipo

- Juan Arguello — motor de transformación
- Miguel Romero — interfaz gráfica

Flujo: trabajo en ramas `feature/*`, Pull Request hacia `master`.
No hacer push directo a `master`.