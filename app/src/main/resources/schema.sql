CREATE TABLE IF NOT EXISTS items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255)   NOT NULL,
    description VARCHAR(1000),
    img_path    VARCHAR(255),
    price       BIGINT         NOT NULL
);

CREATE TABLE IF NOT EXISTS cart_items (
    item_id BIGINT PRIMARY KEY,
    count   INT    NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    total_sum BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS order_items (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    item_id  BIGINT NOT NULL,
    title    VARCHAR(255),
    price    BIGINT NOT NULL,
    count    INT    NOT NULL
);
