# Micro X Editor

Окремий desktop-пакет для редагування **вихідних** ресурсів Micro X. Інтерфейс
побудований на стандартному Python `tkinter`; сторонніх runtime-залежностей
немає. Пакет не є частиною Python SDK гри, runtime resources або J2ME JAR.

## Запуск

Потрібні Python 3.10+ і Tk (у Debian/Ubuntu: `python3-tk`).

```sh
cd sdk/python/microx_editor
python3 -m venv .venv
. .venv/bin/activate
python -m pip install -e .
microx-editor
```

Натисніть **Open project** і виберіть папку, яка містить `assets-src/`,
`assets-src/levels/` та `assets-src/data/`. Дерево показує ресурси тільки цього
проєкту. `*` у заголовку означає незбережені зміни; помилки контракту
показуються діалогом. Save використовує UTF-8 і атомарну заміну файла.

* PNG-перегляд показує atlas, розмір, палітру й footprint; Replace приймає
  PNG atlas `textures.png` до 256×256, 256 кольорів і 96 KiB runtime footprint.
* OBJ-перегляд перевіряє `v`, `vt`, `f`, `o`/`g room_N`, `usemtl` і Micro X
  comments; Replace перевіряє UV, індекси, вироджені грані та Q16.16.
* `.level` редагується як 2D/табличне представлення MXL2: рядки rooms, floors,
  ceilings, edges, portals, spawns, transitions та entities. Serializer зберігає
  comments/порожні рядки, stable IDs і перевіряє counts/references/bounds/links.
* gameplay `.txt` має колонки `id|key|description|meta`, зберігає comments і
  порожні рядки та перевіряє IDs, UTF-8 limits, refs, graph cycles і RMS budget.
* Згенеровані `.mesh`, `.lvl`, `.tex` і packed gameplay files не редагуються.

Headless-тести: `python -m unittest discover -s tests -v`.
