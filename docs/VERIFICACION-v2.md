# Verificación DleBox 2.0

## Datos comprobados

- Referencia inicial: 6 juegos, 5 categorías, APK v1.0.0.
- Fuente adicional: Awesome Daily Minigames, CC0-1.0, revisión y SHA-256 en `catalog/sources/manifest.json`. Instantánea y licencia completas incluidas.
- 284 candidatos web consultados. 278 incorporados, 19 categorías. 266 respuestas HTTP 2xx; 12 respuestas 403 conservadas con estado `unverified`. Cinco fallos excluidos y un duplicado de FilmLink eliminado conservando el ID original.
- Recuento desglosado: 261 juegos y 17 colecciones de juegos. Las 17 colecciones se presentan en su categoría y no se suman como juegos individuales en el informe final.
- `link-checks.json` registra fecha UTC, método, código y destino final o error. No se equipara sintaxis HTTPS válida con disponibilidad comprobada ni HTTP 2xx con una partida terminada.
- 0 miniaturas de terceros; 278 representaciones originales. Coil mantiene carga de imágenes autorizadas y fallback.
- Los seis IDs y el nombre/clave de DataStore de v1 se conservan. Esquema JSON 1 con campos opcionales compatibles.

## Comprobaciones automatizadas

Los resultados definitivos deben contrastarse con las ejecuciones de GitHub Actions y sus informes XML. El proyecto contiene pruebas Python de validación, procedencia, duplicados, preservación de IDs y reconstrucción offline; pruebas Kotlin de búsqueda, ordenación, sorpresa, historial, favoritos, compatibilidad antigua y recuperación de catálogos corruptos/vacíos; y pruebas API 35 de persistencia e interfaz, con recorrido visual y navegador.

Las capturas de favoritos/recientes con varios elementos usan juegos reales del catálogo y datos de prueba locales en el emulador, no actividad del usuario. La prueba de apertura sí pulsa una tarjeta, comprueba el navegador y vuelve con Atrás, verificando que se mantiene la búsqueda.

## Firma

No hay secretos de firma configurados en el repositorio al auditarlo. El certificado de v1 publicada es `78ca3958ed4aca7a0c14f141d85c5a86a2a6633d94fe79d9577591439839057d`; la clave debug local tiene otro certificado. No se dispone de la clave privada de la APK publicada. La nueva firma impide garantizar actualización directa conservando datos; se advierte antes de instalar.

## Límites

No se dispone de Samsung físico. No se completa una partida en los 278 sitios ni se certifican todos los navegadores/Android anteriores. Las comprobaciones de rendimiento de interfaz y desplazamiento utilizan el catálogo real de 278 entradas en API 35. Los resultados de red son puntuales y reproducibles; nunca se eluden certificados inválidos, bloqueos o autenticación.
