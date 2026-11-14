# DleBox 2.0

Biblioteca Android nativa de juegos diarios de navegador. El proyecto existente se amplía de **6 a 278 juegos reales, en 19 categorías**. Kotlin, Compose, Material 3, DataStore, Coil y Custom Tabs; Android 8.0 o posterior. Sin cuentas, publicidad propia, suscripciones ni analíticas.

## Instalar

Descarga **DleBox-v2.0.0.apk** en [GitHub Releases](https://github.com/adritankian-bit/dlebox-android/releases/tag/v2.0.0). Abre el APK en Android y permite la instalación desde el navegador cuando el sistema lo solicite. Los archivos “Source code” no son instaladores.

**Compilación debug de prueba. Firma distinta de la v1.0.0 publicada:** no se ha conservado la clave efímera de aquella ejecución de Actions, y la clave local disponible tiene otro certificado. Android puede exigir desinstalar la versión anterior, borrando sus datos. No desinstales esperando que se conserven favoritos. El formato de datos y los seis IDs originales sí son compatibles para actualizaciones con la misma firma.

## Uso

- **Explorar:** buscador instantáneo por nombre, categoría y descripción; chips de categorías no vacías; contador real; orden por nombre, incorporación, últimos jugados o aleatorio. «Sorpréndeme» respeta búsqueda/categoría y excluye enlaces conocidos como rotos o inválidos.
- **Tarjetas:** dos columnas compactas, arte protagonista, nombre una sola vez, categoría y favorito. El botón de información abre la ficha; tocar la tarjeta abre el juego original.
- **Favoritos:** cuadrícula independiente de los filtros de Explorar, eliminación directa y estado vacío con acceso a Explorar.
- **Recientes:** lista compacta con miniatura, categoría, fecha relativa y eliminación. Borrado completo con confirmación. Reabrir un juego actualiza su fecha, sin duplicarlo.
- **Ajustes:** tema claro/oscuro/sistema, actualización manual, versión, atribuciones y repositorio.

Las pestañas conservan el estado de búsqueda, categoría y desplazamiento de Explorar. Los juegos abren con Custom Tabs o navegador compatible; Atrás devuelve al catálogo. Solo se registra una apertura si Android acepta el lanzamiento. Esto no permite conocer si una página termina de cargar o si se completa una partida.

## Catálogo y procedencia

Listdle se utiliza como referencia de estructura y categorías. Su robots.txt permite rastreo, pero no se localizó licencia para copiar masivamente su directorio o imágenes ni API pública documentada. Se utiliza una fuente adicional con permiso explícito: [Awesome Daily Minigames](https://github.com/guilyx/awesome-daily-minigames), publicada bajo **CC0-1.0**.

La instantánea, revisión inmutable, licencia y comprobaciones están en `catalog/sources/`. De 284 candidatos web se incluyen 278: **266 respuestas HTTP satisfactorias y 12 pendientes por HTTP 403**. Se excluyeron cinco destinos con fallos de conexión/certificado/resolución y un duplicado con la misma URL final. No se eluden bloqueos. Una respuesta HTTP no certifica que toda la lógica del juego funcione.

Las **278 miniaturas son composiciones originales** por categoría e ID, con símbolos, iniciales y patrones diferenciados. No son capturas ni logotipos oficiales. No se han copiado imágenes de terceros. Coil conserva soporte de imagen autorizada, caché, placeholder y fallback. La licencia de un directorio no autoriza automáticamente las imágenes de sus juegos.

Parte de las descripciones reutilizables está en inglés. El idioma del juego se mantiene sin confirmar cuando la fuente no lo acredita. Se conservan las fichas de origen y los IDs anteriores.

## Actualizaciones sin una APK nueva

La app incluye `catalog/games.json` y descarga su versión pública:

`https://raw.githubusercontent.com/adritankian-bit/dlebox-android/main/catalog/games.json`

Mantiene el esquema 1 para compatibilidad anterior, con campos opcionales nuevos. Valida versión, IDs, URLs, duplicados, fechas e imágenes antes de escribir atómicamente la caché. Rechaza un catálogo vacío, corrupto o anterior y conserva el último válido. Carga la copia válida más reciente entre la incluida y la caché. Las fichas de favoritos e historial siguen disponibles aunque un juego desaparezca del catálogo.

## Importación reproducible

Sin dependencias Python adicionales:

```sh
# Reconstrucción exacta sin red desde las pruebas e instantánea guardadas
python scripts/import_cc0.py
python scripts/validate_catalog.py
python -m unittest discover -s scripts -p 'test_*.py' -v

# Nueva instantánea CC0 y comprobación HTTP con límites por dominio
python scripts/import_cc0.py --fetch-source --verify
```

La segunda opción incrementa la versión del catálogo. Consulta la fuente fijada, excluye entradas Reddit, preserva IDs, normaliza categorías al español, elimina duplicados y comprueba destinos con máximo cuatro trabajadores y una solicitud por segundo por host. No copia imágenes ni rastrea Listdle.

El workflow manual **Actualizar catálogo CC0** ejecuta la importación y las pruebas y solo publica datos validados. Para añadir un lote revisado manualmente: `python scripts/import_manual.py entradas.json`. Usa IDs estables, HTTPS, procedencia y estado de verificación honesto. Las imágenes externas necesitan evidencia en `thumbnailPermission`; sin autorización usa `generated` y URL nula.

## Compilar y publicar

JDK 17, SDK 35 y Build Tools 35.0.0. Configura `ANDROID_HOME` o `sdk.dir` en un `local.properties` sin subir a Git.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

En Windows utiliza `gradlew.bat`. APK: `app/build/outputs/apk/debug/app-debug.apk`.

`Android APK` valida catálogo y compila en `main`, PR, ejecución manual y etiquetas `v*`. El nombre del APK se obtiene de `versionName`; al etiquetar se exige que coincida con la versión y que existan las notas correspondientes. Publica APK y SHA-256, no una Release vacía.

`Android device tests` prueba interfaz/persistencia en API 35 y guarda capturas del recorrido en `DleBox-device-check`. Las imágenes proceden de un emulador real, no de mockups.

Para futuras versiones con firma estable está preparado `APK con firma estable`, que recibe `DLEBOX_KEYSTORE_BASE64`, `DLEBOX_STORE_PASSWORD`, `DLEBOX_KEY_ALIAS` y `DLEBOX_KEY_PASSWORD` como secretos. No hay claves privadas en este repositorio.

## Arquitectura y documentación

Se conserva MVVM y las dependencias de producción. `Catalog.kt`: datos, compatibilidad, validación, búsqueda/ordenación. `LibraryViewModel.kt`: DataStore y descarga/caché. `MainActivity.kt`: navegación y pantallas. `LibraryComponents.kt`: tarjetas/listas/vacíos. `GameArtwork.kt`: ilustraciones originales y Coil. `GameBrowser.kt`: apertura y registro condicionado al éxito.

Consulta [auditoría previa](docs/AUDITORIA-v2.md), [informe v2](docs/VERIFICACION-v2.md) y [notas v2](docs/RELEASE-v2.0.0.md). MIT para el código del proyecto; CC0 para la fuente indicada. Los juegos y marcas pertenecen a sus autores. El historial permanece local; la copia de seguridad automática está desactivada. El navegador tiene sus propias cookies y políticas.
