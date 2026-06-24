# my-market-app-reactive

Реактивный аналог интернет-магазина спортивных товаров, реализованный на реактивном стеке Spring.

## Стек

| Слой | Технология |
|---|---|
| HTTP-сервер | Spring WebFlux (Netty) |
| Шаблоны | Thymeleaf |
| БД | H2 in-memory (R2DBC) |
| ORM | Spring Data R2DBC |
| Сборка | Gradle 9, Java 21 |

## Функциональность

- Витрина товаров с поиском, сортировкой (по алфавиту / по цене) и пагинацией
- Детальная страница товара
- Корзина: добавление, уменьшение количества, удаление товара
- Оформление заказа и просмотр истории заказов

## Запуск

```bash
./gradlew :app:bootRun
```

Приложение поднимается на [http://localhost:8080](http://localhost:8080).  
База данных создаётся и заполняется автоматически при старте (`schema.sql` + `data.sql`).

## Тесты

```bash
# Все тесты
./gradlew :app:test

# Конкретный класс
./gradlew :app:test --tests "*.CartServiceTest"

# Принудительный перезапуск (без кэша)
./gradlew :app:test --rerun
```

HTML-отчёт: `app/build/reports/tests/test/index.html`

### Структура тестов

| Тип | Классы | Что используется |
|---|---|---|
| Юнит | `CartServiceTest`, `ItemServiceTest`, `OrderServiceTest` | Mockito + StepVerifier |
| Репозиторий | `ItemRepositoryTest`, `OrderItemRepositoryTest` | `@DataR2dbcTest` + StepVerifier |
| Интеграционные | `ItemControllerIntegrationTest`, `CartControllerIntegrationTest`, `OrderControllerIntegrationTest` | `@SpringBootTest` + WebTestClient |

## Структура проекта

```
app/src/main/java/org/mymarketapp/reactive/
├── config/        # Инициализация схемы БД
├── controller/    # WebFlux-контроллеры (Mono<String>)
├── dto/           # Record-классы (ItemDto, OrderDto, PageDto, …)
├── model/         # Сущности R2DBC (@Table, @Id)
├── repository/    # ReactiveCrudRepository
└── service/       # Бизнес-логика (Mono/Flux)
```
