# Investigación y estrategia de catálogo

Consulta: 14 de noviembre de 2026.

- Fuente: https://listdle.com/ y páginas de detalle `/games/{slug}`.
- La portada muestra 878 juegos y categorías (cifra observada, no importada).
- Las fichas enlazan al juego original mediante “Play”. Sus enlaces pueden llevar parámetros de atribución; el catálogo utiliza el destino original limpio.
- `https://listdle.com/robots.txt` respondió: `User-agent: *`, `Allow: /` y sitemap público. Esto permite rastreo técnico; no acredita permiso de reutilización de la base de datos ni de las imágenes.
- Se consultó https://listdle.com/about/. Describe un directorio seleccionado manualmente. No se localizó API pública documentada ni autorización explícita de extracción masiva. No se afirma que no existan condiciones adicionales.
- No se realizó importación masiva, acceso autenticado, elusión de controles, capturas ni copia de miniaturas. Las descripciones breves en español se escribieron para este proyecto.
- La alternativa implementada es incorporación manual revisada con `scripts/import_manual.py` y validación automática. Para automatizar una fuente futura hay que documentar primero su autorización y límites.

## Entradas iniciales

| Juego | Categoría | Destino |
|---|---|---|
| Wordle | Palabras | https://www.nytimes.com/games/wordle/index.html |
| Waffle | Palabras | https://wafflegame.net/ |
| Globle | Geografía | https://globle-game.com/ |
| Murdle | Misterio | https://murdle.com/ |
| TimeGuessr | Historia | https://timeguessr.com/ |
| FilmLink | Cine y series | https://www.filmlink.io/ |

Todos respondieron HTTP 200 el 14-11-2026. Esto verifica accesibilidad HTTP en esa fecha; no certifica una partida completa, compatibilidad de cada navegador ni disponibilidad futura. Idiomas no confirmados se guardan como `null`. Cada entrada conserva su ficha de Listdle, fecha, tipo y origen de miniatura.

Las seis miniaturas son diseños tipográficos propios con nombre completo y composición de casillas. No representan capturas reales. Se podrán sustituir individualmente por imágenes oficiales o autorizadas con evidencia en `thumbnailPermission`. Coil usa caché y vuelve al diseño original si una imagen falla.
