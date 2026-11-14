# Verificación de la primera versión

Fecha: 14 de noviembre de 2026.

## Comprobado localmente

- `python scripts/validate_catalog.py`: 6 juegos, 5 categorías, válido.
- `python -m unittest discover -s scripts -p 'test_*.py' -v`: 6 pruebas aprobadas. Validación, duplicados, URLs inseguras, autorización de imágenes y fechas de verificación.
- Gradle `testDebugUnitTest lintDebug assembleDebug`: compilación correcta y 10 pruebas Kotlin aprobadas, sin fallos ni errores. Lectura JSON, duplicados, URLs, búsqueda, tildes, categorías, serialización de favoritos y orden/límite/eliminación de historial.
- Gradle `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`: compilación correcta de aplicación y APK instrumentada.
- Android lint: 0 errores. Advertencias de versiones más recientes de dependencias y sugerencias de extensiones KTX. Se han mantenido versiones fijas compatibles verificadas.
- `apksigner verify --print-certs`: APK válida con certificado Android Debug. No se presenta como firma de producción.
- Los seis destinos originales respondieron HTTP 200. La URL pública del catálogo respondió HTTP 200 y pasó validación.
- Distribución Gradle 8.11.1 contrastada con SHA-256 oficial; el checksum está fijado en el wrapper.

## GitHub y dispositivo

La primera compilación de Actions falló por un paquete SDK obsoleto solicitado por defecto. Se corrigió instalando los paquetes concretos necesarios; ejecuciones posteriores compilan correctamente.

El workflow manual `Android device tests` prueba interfaz y reapertura real de DataStore en emulador Android 15/API 35. Sus informes y captura se guardan como artifact `DleBox-device-check`.

Las dos pruebas instrumentadas se ejecutaron y aprobaron (0 fallos, 0 errores): búsqueda/filtros/favoritos/recientes y persistencia después de cerrar y reabrir DataStore. El primer intento de captura posterior falló porque el ejecutor desinstala la aplicación al terminar las pruebas; el workflow la reinstala ahora antes de capturar.

La ejecución corregida completó las dos pruebas y produjo capturas reales en claro y oscuro. La inspección visual detectó poco contraste en los iconos de la barra de estado oscura; se ajusta su apariencia al tema de DleBox.

## Alcance y limitaciones

No se dispone de un Samsung físico conectado. No se ha certificado una partida completa en cada juego ni todos los navegadores o versiones de Android. HTTP 200 acredita respuesta de la página en una fecha, no que toda su lógica esté libre de errores.

No se ha configurado firma privada estable de producción. Para comprobar actualización sobre APK anterior manteniendo datos se necesita la misma clave. El mecanismo mediante secretos está preparado.

No se ha comprobado el caso de cientos de miniaturas remotas porque el catálogo inicial tiene seis tarjetas originales. Se utilizan LazyVerticalGrid y Coil; el formato admite hasta 5.000 juegos y un JSON de 2 MB.
