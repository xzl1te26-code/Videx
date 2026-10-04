# Пошаговая инструкция по публикации Videx на GitHub

Подробное руководство для публикации исходного кода и готового APK-файла приложения **Videx** на GitHub.

---

## Шаг 1. Создание репозитория на GitHub

1. Зайдите на сайт [github.com](https://github.com) и войдите в свой аккаунт.
2. В правом верхнем углу нажмите кнопку **`+`** ➔ **New repository** (или зеленую кнопку **New**).
3. Заполните форму:
   - **Repository name:** `Videx`
   - **Description (Описание):** скопируйте любой из вариантов ниже:
     - **На английском (рекомендуется для GitHub):**
       `Android app for downloading video, audio and photos using yt-dlp & Jetpack Compose`
     - **На русском:**
       `Загрузка видео, аудио и фото с популярных сервисов на базе yt-dlp и Jetpack Compose`
   - **Public / Private:** Выберите **Public** (публичный репозиторий).
   - ⚠️ **ВАЖНО:** Галочки *Add a README file*, *Add .gitignore*, *Choose a license* **НЕ СТАВЬТЕ** (все эти файлы уже созданы и настроены в проекте).
4. Нажмите кнопку **Create repository**.

---

## Шаг 2. Загрузка кода из Android Studio на GitHub

### Способ А (Через графический интерфейс Android Studio — рекомендуется)
1. В верхнем меню Android Studio выберите: **VCS** (или **Git**) ➔ **Share Project on GitHub**.
2. Авторизуйтесь под своим аккаунтом GitHub (если потребуется).
3. Убедитесь, что название репозитория указано как `Videx`.
4. Нажмите кнопку **Share**.
5. Нажмите **Add** во всплывающем окне — Android Studio автоматически загрузит весь исходный код.

### Способ Б (Через встроенный терминал Android Studio)
1. Откройте нижнюю вкладку **Terminal** в Android Studio.
2. Выполните поочередно следующие команды (замените `ТВОЙ_ЛОГИН` на ваш логин на GitHub):

```bash
git init
git add .
git commit -m "Initial commit - Videx v1.0.0"
git branch -M main
git remote add origin https://github.com/ТВОЙ_ЛОГИН/Videx.git
git push -u origin main
```

---

## Шаг 3. Создание релиза и публикация APK-файла

1. Перейдите на страницу вашего репозитория: `https://github.com/ТВОЙ_ЛОГИН/Videx`
2. В правой колонке найдите раздел **Releases** и нажмите **Create a new release** (или *Draft a new release*).
3. Заполните поля релиза:
   - **Choose a tag:** введите `v1.0.0` и нажмите *+ Create new tag: v1.0.0*.
   - **Release title:** `Videx v1.0.0`
   - **Description:** скопируйте описание из блока ниже:
     ```markdown
     Первый официальный релиз Videx v1.0.0.

     Основные возможности:
     - Скачивание видео (до 4K/60FPS), извлечение аудио (MP3/M4A), а также фото/каруселей с 1000+ сайтов.
     - Локальный движок yt-dlp (Python 3.13) через Chaquopy.
     - Встроенный плеер на базе ExoPlayer с жестами и режимом Картинка-в-картинке (PiP).
     - Фоновые загрузки через WorkManager с уведомлениями.
     - Интерфейс Material 3 на Jetpack Compose.
     ```
4. **Прикрепление APK:**
   Внизу страницы в поле с иконкой скрепки (*Attach binaries by dropping them here...*) перетащите файл релиза с вашего компьютера:
   `D:\AndroidStudioProjects\MyApplication\app\build\outputs\apk\release\app-release.apk`
5. Нажмите зеленую кнопку **Publish release**.

---

## Что делать дальше?

После того как репозиторий будет создан и первая версия `v1.0.0` будет опубликована:
1. Напишите сюда ваш логин на GitHub.
2. Мы сразу встроим модуль автоматического обновления (OTA) прямо в код приложения, чтобы в следующих версиях пользователи обновляли Videx в один клик!
