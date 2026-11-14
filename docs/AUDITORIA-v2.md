# Auditoría previa a DleBox 2.0

14-11-2026. Revisión inicial: `7674411`, sincronizada con `origin/main`, sin cambios locales. Mismo repositorio `adritankian-bit/dlebox-android`; única versión publicada observada: v1.0.0.

## Estructura y funcionamiento existente

- Un módulo `app`, Kotlin 2.1.20, Compose/Material 3, AGP 8.9.2, Gradle 8.11.1, SDK 35/min 26. Se conservan estas versiones.
- MVVM sencillo: `Catalog.kt` reúne modelos, validación y búsqueda; `LibraryViewModel.kt` carga catálogo y gestiona DataStore; `MainActivity.kt` contiene la interfaz.
- `catalog/games.json`: seis juegos, cinco categorías. Incluido como asset y descargado desde raw.githubusercontent.com del mismo repositorio. Caché interna con escritura atómica y control de versión. Un fallo de descarga conserva los datos.
- Categorías de texto procedentes del JSON. El buscador/filtro se aplican también a Favoritos y Recientes, creando filtros invisibles al usuario y desperdiciando espacio.
- Favoritos: IDs en DataStore `library`, clave `library_v1`, con snapshots de fichas. Se conservan ambos nombres y los seis IDs para compatibilidad.
- Recientes: mapa ID/fecha, máximo 200, ya evita duplicar juegos. Falta borrado completo y una lista compacta.
- Apertura: Custom Tabs y fallback ACTION_VIEW. Registra el historial después de lanzar; si falla no registra. Se mantiene y se añade validación previa y prueba de ida/vuelta.
- Coil soporta imágenes remotas y error/loading. Las seis entradas utilizan el mismo diseño gráfico, con nombre duplicado. Se sustituye su presentación por composiciones originales por categoría/juego.
- Navegación: tres pestañas y estados de grid; búsqueda/filtros guardados con rememberSaveable. Se conserva el estado de Explorar y se separan las otras secciones.
- Workflows: `android.yml` valida/compila/prueba y publica al etiquetar, pero tiene nombre y guardia fijos v1.0.0. `device.yml` ejecuta dos pruebas API 35 y captura claro/oscuro. `signed.yml` admite firma privada por secretos sin publicar claves.
- v1.0.0 es debug de Actions, sin clave estable guardada en el repositorio. Se comprobará si existe una clave segura compatible; de no existir, se advertirá de la incompatibilidad de actualización directa.

## Refactorización decidida

Ampliar el JSON mediante una fuente con permiso explícito y comprobación HTTP separada; conservar esquema 1 y campos anteriores. Mejorar carga inicial ante corrupción. Separar componentes visuales y apertura de enlaces sin cambiar arquitectura ni dependencias de producción. Cabecera compacta, cuadrícula de dos columnas, ilustraciones geométricas originales sin repetir nombres, ordenación y sorpresa; Favoritos sin filtros globales, Recientes como lista y estados vacíos con acción.

## Fuentes

Listdle se revisa como referencia: portada, fichas, destinos originales, about y robots.txt (`Allow: /`). No se encontró licencia de reutilización masiva o API documentada. Se evita copiar su base de datos o miniaturas. Se identificó `guilyx/awesome-daily-minigames`, que publica explícitamente sus datos bajo CC0-1.0: permite una importación reproducible y amplia con procedencia. La licencia de los datos no autoriza imágenes o marcas externas; las miniaturas iniciales de v2 serán obras gráficas originales.
