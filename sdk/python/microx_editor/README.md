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
* `.level` має фільтр і окрему таблицю для `room`, `floor`, `ceiling`, `edge`,
  `portal`, `spawn`, `transition` та `entity`. Кнопки **Add**, **Delete**,
  **Move Up/Down** змінюють records; serializer автоматично перераховує
  `counts`, зберігаючи capacity, comments, порожні рядки та stable ID.
* Gameplay `.txt` має окрему таблицю rows із колонками `id`, `key`,
  `description`, `metadata`. Metadata-форми залежать від таблиці, а для items —
  від `weapon`, `armor`, `artifact`, `consumable` або `ammo`; доступні ті самі
  операції додавання, видалення та впорядкування.
* **Validate Project** перевіряє одночасно всі gameplay-таблиці. **Save** і
  **Save All** повторюють повну проєктну перевірку та лише після неї атомарно
  записують UTF-8. Перемикання файла/проєкту, Replace і закриття використовують
  один guard **Save/Discard/Cancel**.
* Raw source показано лише як read-only **Advanced**-режим; структуровані
  таблиці й форми є авторитетним способом редагування.
* Згенеровані `.mesh`, `.lvl`, `.tex` і packed gameplay files не редагуються.

Headless-тести: `python -m unittest discover -s tests -v`.
