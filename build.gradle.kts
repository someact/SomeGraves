plugins {
    `java-library`
}

group = "com.someact.somegraves"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    flatDir {
        dirs("/home/byact/minecraft-server/pcMSMP/libraries/io/papermc/paper/paper-api/26.2.build.112-stable")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.112-stable")
    compileOnly(files("/home/byact/minecraft-server/pcMSMP/libraries/io/papermc/paper/paper-api/26.2.build.112-stable/paper-api-26.2.build.112-stable.jar"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(26)
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
}

tasks.register<Copy>("copyToProductionServer") {
    dependsOn(tasks.jar)
    from(tasks.jar.get().archiveFile)
    into("/home/byact/minecraft-server/pcMSMP/plugins")
    rename { "SomeGraves.jar" }
}

tasks.register("copyToServer") {
    dependsOn(tasks.named("copyToTestServer"), tasks.named("copyToProductionServer"))
}
