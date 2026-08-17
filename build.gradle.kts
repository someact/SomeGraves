plugins {
    `java-library`
}

group = "com.someact.somegraves"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.register<Copy>("copyToTestServer") {
    dependsOn(tasks.jar)
    from(tasks.jar.get().archiveFile)
    into("/home/byact/minecraft-server/test_myplugin/plugins")
    rename { "SomeGraves.jar" }
    onlyIf { file("/home/byact/minecraft-server/test_myplugin/plugins").exists() }
}

tasks.register<Copy>("copyToProductionServer") {
    dependsOn(tasks.jar)
    from(tasks.jar.get().archiveFile)
    into("/home/byact/minecraft-server/pcMSMP/plugins")
    rename { "SomeGraves.jar" }
    onlyIf { file("/home/byact/minecraft-server/pcMSMP/plugins").exists() }
}

tasks.register("copyToServer") {
    dependsOn(tasks.named("copyToTestServer"), tasks.named("copyToProductionServer"))
}
