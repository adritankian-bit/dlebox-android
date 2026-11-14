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

Resultado final: **34 pruebas aprobadas: 21 Kotlin, 11 Python y 2 instrumentadas en Android 15/API 35**, sin fallos ni errores. Compilan la aplicación y la APK de pruebas. Android lint termina sin errores; conserva advertencias no bloqueantes de versiones nuevas de dependencias y recomendaciones de API.

- [Compilación y pruebas de la revisión verificada](https://github.com/adritankian-bit/dlebox-android/actions/runs/43544181748).
- [Recorrido API 35 aprobado y artifact de capturas/informes](https://github.com/adritankian-bit/dlebox-android/actions/runs/43544188282).
- Las pruebas Python verifican validación, procedencia, duplicados, preservación de IDs, reconstrucción offline y conservación de incorporaciones manuales.
- Las pruebas Kotlin verifican búsqueda, tildes, ordenación, sorpresa, historial, favoritos, compatibilidad antigua, apertura fallida sin registrar y recuperación de catálogos corruptos/vacíos.
- Las dos pruebas API 35 verifican reapertura real de DataStore y recorrido de interfaz: búsqueda/categoría, estado de scroll al cambiar de sección, favoritos tras recreación, borrado individual/completo del historial con confirmación, temas y navegador.

Se revisaron visualmente diez capturas reales: catálogo completo en claro, categoría Geografía, búsqueda Waffle, favoritos vacíos/con cuatro juegos, recientes con tres juegos/vacíos, catálogo en oscuro, tablero real de Waffle en Custom Tabs y regreso con la búsqueda Waffle conservada. Se corrigieron desbordamiento de ilustraciones, legibilidad de miniaturas pequeñas y fondos/contraste de las barras del sistema en tema manual. La prueba espera las transiciones iniciales de Chrome y no envía pulsaciones Atrás extra al catálogo.

La URL pública del JSON respondió HTTP 200 y su contenido coincide con el catálogo incluido: 278 entradas, 19 categorías. El funcionamiento sin conexión y con archivos inválidos se verifica mediante selección/validación de caché y catálogo incluido; no se afirma una partida web sin Internet ni una prueba física en modo avión.

Las capturas de favoritos/recientes con varios elementos usan juegos reales del catálogo y datos de prueba locales en el emulador, no actividad del usuario. La prueba de apertura sí pulsa una tarjeta, comprueba el navegador y vuelve con Atrás, verificando que se mantiene la búsqueda.

## Firma

No hay secretos de firma configurados en el repositorio al auditarlo. El certificado de v1 publicada es `78ca3958ed4aca7a0c14f141d85c5a86a2a6633d94fe79d9577591439839057d`; la clave debug local tiene otro certificado. No se dispone de la clave privada de la APK publicada. La nueva firma impide garantizar actualización directa conservando datos; se advierte antes de instalar.

## Límites

No se dispone de Samsung físico. No se completa una partida en los 278 sitios ni se certifican todos los navegadores/Android anteriores. Las comprobaciones de rendimiento de interfaz y desplazamiento utilizan el catálogo real de 278 entradas en API 35. Los resultados de red son puntuales y reproducibles; nunca se eluden certificados inválidos, bloqueos o autenticación.
