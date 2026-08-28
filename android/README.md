# Am I Awake — Android MVP

Отдельный Android-клиент для существующего Spring Boot API.

## Запуск

1. Откройте папку `android` в Android Studio.
2. Дождитесь Gradle Sync и запустите конфигурацию `app` на устройстве с Android 10 (API 29) или новее.
3. Backend должен быть доступен на `http://localhost:8080` компьютера. В эмуляторе клиент обращается к нему через `http://10.0.2.2:8080`.

Для сборки используются JDK 17 или 21 и Android SDK 35. JDK 24 пока не подходит к выбранной стабильной связке Android Gradle Plugin/Gradle.

Для физического телефона измените `API_BASE_URL` в `app/build.gradle.kts` на IP компьютера в локальной сети и убедитесь, что backend слушает этот интерфейс.

## Что входит в MVP

- регистрация и вход;
- access/refresh JWT с автоматическим refresh;
- профиль и availability status;
- друзья и входящие/исходящие заявки;
- sleep schedule;
- текущий inferred user state;
- offline-очередь `DeviceEvent`, batch sync и ручная отправка событий;
- heartbeat через WorkManager (минимальный интервал Android — 15 минут);
- сбор `SCREEN_ON`, `SCREEN_OFF`, `PHONE_UNLOCKED`, `CHARGING_STARTED` и `CHARGING_STOPPED`, пока процесс приложения активен.

Android может останавливать фоновые процессы. Для production-grade непрерывной телеметрии потребуется отдельно выбрать UX и стратегию foreground service/permissions; backend-контракт для текущего MVP менять не нужно.

## Проверка Sleep API

1. Запустите приложение на физическом устройстве с Google Play services и разрешите распознавание физической активности.
2. В Logcat установите фильтр по тегу `AmIAwakeSleepApi`.
3. Сначала должно появиться сообщение об успешной подписке. Classification-события приходят периодически (обычно примерно раз в 10 минут) и логируются в формате:

```text
SleepClassifyEvent(timestamp=..., sleepConfidence=..., motion=..., light=...)
```

После получения classification-события приложение отправляет его на backend через текущую авторизованную сессию. Результат отправки логируется с тем же тегом.
