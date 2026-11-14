# DleBox v1.0.0 — compilación de prueba

Primera versión Android nativa con catálogo de seis juegos en cinco categorías.

- Búsqueda por nombre, descripción y categoría.
- Favoritos e historial locales, con fecha y eliminación del historial.
- Apertura de juegos originales mediante Custom Tabs o navegador.
- Fichas, atribución, temas claro/oscuro/sistema y actualización remota con caché.
- Tarjetas gráficas originales; sin reutilizar miniaturas protegidas.

Descarga **DleBox-v1.0.0.apk** en los assets. Android 8.0 o posterior. Es una APK debug instalable, no una versión firmada de producción.

## Límites conocidos

Catálogo pequeño y manual; no se han importado los 878 juegos observados en Listdle. Los enlaces respondieron HTTP 200 durante la preparación, sin garantizar disponibilidad futura. No se ha certificado una partida completa en cada sitio ni probado en un Samsung físico. Custom Tabs no informa a DleBox de los fallos internos de una web: vuelve con Atrás y abre de nuevo o usa la opción externa.

Las compilaciones debug posteriores pueden utilizar otra firma y exigir desinstalar la anterior, perdiendo sus datos locales. No se ha configurado una clave estable de producción. El historial registra aperturas, no partidas terminadas. Para consultar offline se incluye catálogo; jugar requiere Internet.
