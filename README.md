# my-market-app-reactive

Реактивный интернет-магазин спортивных товаров на стеке Spring WebFlux.

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

**Локально:**

```bash
./gradlew :app:bootRun
```

**Docker Compose:**

```bash
docker compose up --build
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
my-market-app-reactive/
├── app/
│   ├── Dockerfile
│   └── src/main/java/org/mymarketapp/reactive/
│       ├── config/        # Инициализация схемы БД
│       ├── controller/    # WebFlux-контроллеры (Item, Cart, Order, Image)
│       ├── dto/           # Record-классы (ItemDto, OrderDto, PageDto, SortType, ActionType)
│       ├── exception/     # Кастомные исключения (ItemNotFoundException, …)
│       ├── model/         # Сущности R2DBC (Item, CartItem, Order, OrderItem)
│       ├── repository/    # ReactiveCrudRepository
│       ├── service/       # Бизнес-логика (Mono/Flux)
│       └── util/          # GridUtils (разбивка товаров по строкам)
└── docker-compose.yaml
```
