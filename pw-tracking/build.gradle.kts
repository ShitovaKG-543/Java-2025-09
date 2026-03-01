group = "ru.otus.trackingbot"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // PostgreSQL
    implementation("org.postgresql:postgresql")

    implementation("com.github.ben-manes.caffeine:caffeine")

    // Telegram Bot
    implementation("org.telegram:telegrambots-spring-boot-starter")

    // SOAP support for Russian Post
    implementation("org.springframework.ws:spring-ws-core")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api")
    implementation("org.glassfish.jaxb:jaxb-runtime")
    implementation("jakarta.xml.soap:jakarta.xml.soap-api")
    implementation("com.sun.xml.messaging.saaj:saaj-impl")
    implementation("jakarta.xml.ws:jakarta.xml.ws-api")
    implementation("wsdl4j:wsdl4j")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Logging
    implementation("ch.qos.logback:logback-classic")

    // Flyway
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// Глобальное исключение проблемных модулей
configurations.all {
    exclude(group = "com.fasterxml.jackson.module", module = "jackson-module-jaxb-annotations")

    resolutionStrategy {
        force("org.checkerframework:checker-qual:3.37.0")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}