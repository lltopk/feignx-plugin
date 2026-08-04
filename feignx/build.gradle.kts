plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "com.lyflexi"
version = "5.6.4.4"

repositories {
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
    mavenCentral()
}

dependencies {
    intellijPlatform {
        create("IC", "2023.3")
        bundledPlugin("com.intellij.java")
    }
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    implementation("org.yaml:snakeyaml:2.3")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("commons-collections:commons-collections:3.2.2")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "233"
            untilBuild = "999.*"
        }
        changeNotes = """
            Migrated to IntelliJ Platform Gradle Plugin 2.x<br/>
            Minimum supported IDE version: 2023.3+
        """.trimIndent()
    }
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks {
    runIde {
        jvmArgs("-Xmx4096m", "-XX:ReservedCodeCacheSize=512m", "-Xms128m")
    }
}
