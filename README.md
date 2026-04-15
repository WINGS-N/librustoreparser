# librustoreparser

## Методы

- `RuStoreCrawlerConfig.builder()`: создаёт настройки обхода
- `baseUrl(String)`: задаёт базовый URL RuStore
- `seedDeveloperIds(Collection<String>)`: задаёт ID разработчиков для обхода
- `directPackages(Collection<String>)`: добавляет package name напрямую
- `maxDeveloperPages(int)`: ограничивает число страниц одного разработчика, `0` без лимита
- `maxSeedDevelopers(int)`: ограничивает число разработчиков, `0` без лимита
- `concurrency(int)`: задаёт число параллельных обходов
- `requestTimeout(Duration)`: задаёт таймаут HTTP запроса
- `requestDelay(Duration)`: задаёт задержку между запросами
- `maxAttempts(int)`: задаёт число попыток запроса
- `userAgent(String)`: задаёт user agent
- `logger(RuStoreLogger)`: подключает обработчик логов
- `build()`: возвращает `RuStoreCrawlerConfig`
- `new RuStoreCrawler(config)`: создаёт crawler со стандартным HTTP загрузчиком
- `new RuStoreCrawler(config, fetcher)`: создаёт crawler со своим `RuStorePageFetcher`
- `crawl()`: запускает обход и возвращает `CrawlResult`
- `CrawlResult.getPackageNames()`: возвращает список package name
- `CrawlResult.getApps()`: возвращает список `AppInfo`
- `CrawlResult.getDevelopers()`: возвращает список `DeveloperSeed`
- `AppInfo.getPackageName()`: возвращает package name
- `AppInfo.getAppName()`: возвращает название приложения
- `AppInfo.getDeveloperName()`: возвращает имя разработчика
- `AppInfo.getDeveloperPath()`: возвращает путь разработчика в RuStore
- `DeveloperSeed.getId()`: возвращает ID разработчика
- `DeveloperSeed.getPath()`: возвращает путь разработчика
- `DeveloperSeed.getName()`: возвращает имя разработчика

## Пример: package name

```java
RuStoreCrawlerConfig config = RuStoreCrawlerConfig.builder().build();
RuStoreCrawler crawler = new RuStoreCrawler(config);
CrawlResult result = crawler.crawl();

for (String packageName : result.getPackageNames()) {
    System.out.println(packageName);
}
```

## Пример: настройки

```java
RuStoreCrawlerConfig config = RuStoreCrawlerConfig.builder()
        .seedDeveloperIds(List.of("df91b73f"))
        .directPackages(List.of("ru.plazius.cofix"))
        .maxDeveloperPages(0)
        .maxSeedDevelopers(0)
        .concurrency(3)
        .requestTimeout(Duration.ofSeconds(20))
        .requestDelay(Duration.ofMillis(300))
        .maxAttempts(5)
        .logger(System.out::println)
        .build();
```

## Пример: свой загрузчик

```java
RuStorePageFetcher fetcher = uri -> new RuStorePageFetcher.PageResponse(
        200,
        "<html>...</html>"
);

RuStoreCrawler crawler = new RuStoreCrawler(config, fetcher);
CrawlResult result = crawler.crawl();
```
