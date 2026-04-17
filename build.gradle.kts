plugins {
    `java-library`
}

group = "wings.v.external"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

tasks.register<JavaExec>("runCrawlerCli") {
    group = "application"
    description = "Runs the manual RuStore crawler CLI."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("wings.v.rustore.parser.RuStoreCrawlerCli")
}
