plugins {
    java
    application
    //id("com.github.johnrengelman.shadow") version "8.1.1"
}

//group = "ru.otus.trackingbot"
//version = "1.0.0"
//
//repositories {
//    mavenCentral()
//    // Добавляем дополнительные репозитории для надежности
//    maven {
//        url = uri("https://repo.maven.apache.org/maven2/")
//    }
//    maven {
//        url = uri("https://plugins.gradle.org/m2/")
//    }
//}
//
//dependencies {
//    // Telegram Bot API
//    implementation("org.telegram:telegrambots:6.9.7.1")
//
//    // OkHttp для HTTP запросов
//    implementation("com.squareup.okhttp3:okhttp:4.12.0")
//
//    // JSON обработка
//    implementation("com.google.code.gson:gson:2.10.1")
//
//    // Логирование
//    implementation("org.slf4j:slf4j-simple:2.0.9")
//
//    // Lombok (опционально)
//    compileOnly("org.projectlombok:lombok:1.18.30")
//    annotationProcessor("org.projectlombok:lombok:1.18.30")
//
//    // Тестирование - исправленная версия с явным указанием BOM
//    testImplementation(platform("org.junit:junit-bom:5.9.2"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
//
//    // SnakeYAML для парсинга YAML
//    implementation("org.yaml:snakeyaml:2.2")
//
//    // Для SOAP клиента (замена retrofit)
//    implementation("org.springframework.ws:spring-ws-core")
//    implementation("javax.xml.bind:jaxb-api:2.3.1")
//    implementation("com.sun.xml.bind:jaxb-impl:2.3.1")
//    implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
//    implementation("javax.xml.soap:javax.xml.soap-api:1.4.0")
//    implementation("com.sun.xml.messaging.saaj:saaj-impl:1.5.3")
//
//    // Для генерации классов из WSDL
//    implementation("wsdl4j:wsdl4j:1.6.3")
//
//    // Для работы с XML
//    implementation("javax.xml.ws:jaxws-api:2.3.1")
//}
//
//application {
//    mainClass.set("com.trackingbot.TrackingBotApplication")
//}
//
//tasks.withType<JavaCompile> {
//    options.encoding = "UTF-8"
//}
//
//tasks.withType<Jar> {
//    manifest {
//        attributes["Main-Class"] = "com.trackingbot.TrackingBotApplication"
//    }
//}
//
//tasks.shadowJar {
//    archiveBaseName.set("tracking-bot")
//    archiveClassifier.set("")
//    archiveVersion.set(version.toString())
//
//    mergeServiceFiles()
//    exclude("META-INF/*.SF")
//    exclude("META-INF/*.DSA")
//    exclude("META-INF/*.RSA")
//}
//
//tasks.test {
//    useJUnitPlatform()
//    // Добавляем настройки для тестов
//    testLogging {
//        events("passed", "skipped", "failed")
//    }
//}
//
//// Добавляем конфигурацию для разрешения конфликтов версий
//configurations.all {
//    resolutionStrategy {
//        // Принудительно используем конкретные версии
//        force("org.junit.jupiter:junit-jupiter-api:5.9.2")
//        force("org.junit.platform:junit-platform-commons:1.9.2")
//        force("org.junit:junit-bom:5.9.2")
//    }
//}


/////////

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // JavaCV для создания видео
    implementation("org.bytedeco:javacv-platform:1.5.9")

    // JavaFX для графики
    implementation("org.openjfx:javafx-base:21")
    implementation("org.openjfx:javafx-graphics:21")
    implementation("org.openjfx:javafx-controls:21")
    implementation("org.openjfx:javafx-swing:21")

    // Логирование
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

application {
    mainClass.set("com.example.KaleidoscopeGenerator")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Важно для JavaFX на разных платформах
val osName = System.getProperty("os.name").lowercase()
val platform = when {
    osName.contains("win") -> "win"
    osName.contains("mac") -> "mac"
    osName.contains("linux") -> "linux"
    else -> throw IllegalStateException("Unknown OS: $osName")
}

// Добавляем платформозависимые зависимости JavaFX
dependencies {
    implementation("org.openjfx:javafx-base:21:$platform")
    implementation("org.openjfx:javafx-graphics:21:$platform")
    implementation("org.openjfx:javafx-controls:21:$platform")
}