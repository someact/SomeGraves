plugins {
    `java-library`
}

group = "com.someact.somegraves"
val projectVersion: String = project.findProperty("version")?.toString()?.takeIf { it != "unspecified" } ?: "1.0.3"
version = projectVersion

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

tasks.register("copyToServer") {
    dependsOn(tasks.jar)
    
    val targetDirs = listOf(
        "/home/byact/minecraft-server/test_26.2/plugins",
        "/home/byact/minecraft-server/test_1.21.11/plugins",
        "/home/byact/minecraft-server/test_1.21/plugins",
        "/home/byact/minecraft-server/test_1.20.6/plugins",
        "/home/byact/minecraft-server/pcMSMP/plugins"
    )
    
    doLast {
        val jarFile = tasks.jar.get().archiveFile.get().asFile
        for (dirPath in targetDirs) {
            val dir = file(dirPath)
            if (dir.exists()) {
                dir.listFiles { _, name -> name.startsWith(project.name) && name.endsWith(".jar") }?.forEach { it.delete() }
                copy {
                    from(jarFile)
                    into(dir)
                    rename { "${project.name}-${project.version}.jar" }
                }
            }
        }
    }
}



