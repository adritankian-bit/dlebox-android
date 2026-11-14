# DleBox

Biblioteca Android nativa para descubrir y abrir juegos diarios en sus páginas originales. Kotlin, Jetpack Compose y Material 3. Compatible con Android 8.0 (API 26) y posteriores.

## Qué incluye

- Explorar, Favoritos y Recientes, búsqueda instantánea y categorías.
- Cuadrícula virtualizada de dos columnas, temas claro, oscuro y del sistema.
- Apertura mediante Custom Tabs o navegador; ficha secundaria y apertura externa explícita.
- Favoritos e historial locales con DataStore; conserva fichas de juegos guardados aunque desaparezcan del catálogo remoto.
- Catálogo incluido en la APK, caché local y actualización HTTPS en segundo plano al abrir la aplicación o desde Ajustes.
- Coil para imágenes autorizadas; tarjetas tipográficas originales como alternativa sin conexión.

Catálogo inicial: **6 juegos reales y 5 categorías**, con comprobación HTTP 200 el 14 de noviembre de 2026. No se ha importado masivamente Listdle. No se han reutilizado sus imágenes. Ver [investigación](docs/CATALOGO.md).

## Descargar e instalar

Abre [Releases](https://github.com/adritankian-bit/dlebox-android/releases) desde el móvil. En `v1.0.0`, descarga el asset **DleBox-v1.0.0.apk**. Los ZIP “Source code” no son la aplicación.

Abre el APK descargado y permite a tu navegador instalar aplicaciones cuando Android lo solicite. Confirma la instalación y abre DleBox. Los juegos requieren conexión.

La primera versión es **debug de prueba**, no una release firmada de producción. No garantiza la misma firma entre recompilaciones de Actions. Una APK con firma diferente exige desinstalar la anterior, lo que puede borrar datos locales. Para actualizaciones que conserven favoritos, usa versiones con la misma clave privada estable. No se incluye ninguna clave en este repositorio.

## Compilar

Requiere JDK 17, Android SDK 35 y Build Tools 35.0.0. Android Studio puede instalar el SDK; configura `ANDROID_HOME` o `sdk.dir` en un `local.properties` que no se sube a Git.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

En Windows: `gradlew.bat testDebugUnitTest lintDebug assembleDebug`.
Resultado: `app/build/outputs/apk/debug/app-debug.apk`.

## Actualizar el catálogo sin cambiar la APK

Edita `catalog/games.json`, incrementa `version`, valida y publica en `main`. La aplicación descarga:

`https://raw.githubusercontent.com/adritankian-bit/dlebox-android/main/catalog/games.json`

Se valida antes de sustituir la copia local con escritura atómica. Un error de red, JSON inválido o retroceso de versión mantiene el catálogo anterior. Máximo 2 MB y 5.000 entradas. No se descarga código ejecutable ni se requiere una nueva APK. El catálogo incluido se actualiza también al compilar.

## Añadir juegos sin Kotlin

Duplica una entrada JSON. Asigna un ID estable, nombre, descripción propia, categoría en español y `gameUrl` HTTPS original. Conserva `sourceUrl` como atribución. Comprueba el enlace, indica `linkStatus` y `verifiedAt` honestamente. No reutilices un ID para otro juego.

Sin permiso de imagen utiliza `thumbnailType: "generated"`, `thumbnailUrl: null` y explica el origen en `thumbnailSource`. Una imagen externa exige HTTPS, tipo `official` o `authorized` y evidencia de autorización en `thumbnailPermission`. Un metadato Open Graph por sí solo no concede licencia.

Para importar un lote manual revisado, crea un array JSON de entradas con el mismo formato:

```sh
python scripts/import_manual.py entradas.json
python scripts/validate_catalog.py
python -m unittest discover -s scripts -p 'test_*.py' -v
```

El importador fusiona por ID, incrementa versión y rechaza duplicados de URL antes de escribir. No rastrea sitios ni descarga imágenes.

## GitHub Actions y publicación

`Android APK` se ejecuta en push a `main`, pull requests, etiquetas `v*` y manualmente. Valida JSON, ejecuta pruebas Kotlin/Python y lint, compila APK real y guarda un artifact. La etiqueta `v1.0.0` crea una prerelease con el APK y notas. Una etiqueta distinta exige actualizar versión, nombre del APK y notas en el workflow antes de publicar.

```sh
git tag v1.0.0
git push origin v1.0.0
```

Firma futura: el workflow manual `APK con firma estable` recibe los secretos `DLEBOX_KEYSTORE_BASE64`, `DLEBOX_STORE_PASSWORD`, `DLEBOX_KEY_ALIAS` y `DLEBOX_KEY_PASSWORD`. La clave debe ser aportada y custodiada por el titular; no se genera ni publica aquí. Produce un artifact firmado sin publicarlo automáticamente.

## Privacidad y límites

Sin cuentas, notificaciones, anuncios propios, compras ni analíticas. Único permiso: Internet. El historial no se transmite por DleBox y la copia de seguridad automática está desactivada. Las páginas de juegos mantienen sus propias políticas, cookies y publicidad.

Custom Tabs no permite detectar fallos HTTP internos ni saber si una partida se completó: “Recientes” registra la apertura. Ante un fallo del sitio, vuelve con Atrás y usa Jugar de nuevo o Abrir en navegador desde la ficha. No se modifica contenido de los juegos.

Pruebas unitarias cubren modelo y serialización; no sustituyen la prueba de persistencia real tras reinicio, actualización e instalación en Samsung. Consulta [estado de verificación](docs/VERIFICACION.md).

## Estructura y ampliación

`Catalog.kt` contiene formato, validación, búsqueda y modelo local. `LibraryViewModel.kt` gestiona DataStore y descarga/caché. `MainActivity.kt` contiene Compose y navegación. `catalog/`, `scripts/` y `.github/workflows/` mantienen datos y automatización separados de la interfaz. Los IDs y snapshots permiten añadir exportación de favoritos, fuentes y filtros de idioma en versiones posteriores.

Código del proyecto bajo MIT. Nombres, juegos y marcas pertenecen a sus autores; la licencia no concede derechos sobre contenido externo.
