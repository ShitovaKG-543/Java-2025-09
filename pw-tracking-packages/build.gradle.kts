plugins {
    id("java")
    id("application")
}

version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Telegram Bot API
    implementation("org.telegram:telegrambots:6.9.7.1")

    // PostgreSQL
    implementation("org.postgresql:postgresql:42.7.1")

    // HikariCP (connection pool)
    implementation("com.zaxxer:HikariCP:5.0.1")

    // SOAP client for Russian Post
    implementation("jakarta.xml.ws:jakarta.xml.ws-api:4.0.0")
    implementation("com.sun.xml.ws:jaxws-rt:4.0.2")

    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Scheduling
    implementation("org.quartz-scheduler:quartz:2.3.2")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Configuration
    implementation("org.yaml:snakeyaml:2.2")

    // Utils
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")


    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}

application {
    mainClass.set("com.postbot.Main")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = "17"
    targetCompatibility = "17"
}