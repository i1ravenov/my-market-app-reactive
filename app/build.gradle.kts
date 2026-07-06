import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.9.0"
    id("java")
}

group = "ru.yandex.practicum"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("io.r2dbc:r2dbc-h2")
    runtimeOnly("com.h2database:h2")

    implementation("org.openapitools:jackson-databind-nullable:0.2.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")
}

val generatedPaymentClientDir = layout.buildDirectory.dir("generated-payment-client").get().asFile

tasks.openApiGenerate {
    generatorName.set("java")
    library.set("webclient")
    inputSpec.set("$rootDir/openapi/payment-api.yaml")
    outputDir.set(generatedPaymentClientDir.path)
    apiPackage.set("org.mymarketapp.reactive.paymentclient.api")
    modelPackage.set("org.mymarketapp.reactive.paymentclient.model")
    invokerPackage.set("org.mymarketapp.reactive.paymentclient")
    configOptions.set(
        mapOf(
            "useJakartaEe" to "true",
            "useTags" to "true"
        )
    )
}

sourceSets {
    main {
        java {
            srcDir(generatedPaymentClientDir.resolve("src/main/java"))
        }
    }
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

tasks.test {
    useJUnitPlatform()
}
