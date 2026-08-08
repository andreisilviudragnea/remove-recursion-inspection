

# Inspección para Eliminar Recursión

Esta inspección extiende la funcionalidad de la inspección "Recursión Final"
del grupo "Rendimiento" para Java en Intellij IDEA.

Esta inspección detecta métodos que contienen llamadas recursivas (no solo llamadas recursivas 
finales) y elimina la recursión del cuerpo del método, preservando la semántica original
del código. Sin embargo, el código resultante se vuelve bastante ofuscado si el 
flujo de control en el método recursivo es complejo.

## Estructura del repositorio
- El directorio [inspection](inspection) contiene el código fuente de la inspección.
- Una descripción detallada del algoritmo se incluye en [thesis/thesis.pdf](thesis/thesis.pdf).
- El proyecto [test-recursion](test-recursion) contiene ejemplos de métodos recursivos y pruebas
 para verificar que se preserva la semántica del código.
