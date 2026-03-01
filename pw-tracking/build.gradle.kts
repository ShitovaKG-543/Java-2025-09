
group = "ru.otus.trackingbot"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Telegram Bot
    implementation("org.telegram:telegrambots-spring-boot-starter:6.9.7.1")

    // Database
    implementation("com.h2database:h2")

    // SOAP support for Russian Post
    implementation("org.springframework.ws:spring-ws-core")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.sun.xml.bind:jaxb-impl:2.3.1")
    implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
    implementation("javax.xml.soap:javax.xml.soap-api:1.4.0")
    implementation("com.sun.xml.messaging.saaj:saaj-impl:1.5.3")
    implementation("wsdl4j:wsdl4j:1.6.3")
    implementation("javax.xml.ws:jaxws-api:2.3.1")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Logging
    implementation("ch.qos.logback:logback-classic")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}