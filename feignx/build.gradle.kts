
idea{
    module{
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.5.2"
}

group = "com.lyflexi"
version = "5.7.1"

repositories {
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
    mavenCentral()
    gradlePluginPortal()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2021.2")
//    type.set("IU") // Target IDE Platform
    type.set("IC") // Target IDE Platform
    //gradle的下载idea安装包位置: %USERPROFILE%\.gradle\caches\modules-2\files-2.1\com.jetbrains.intellij.idea
    plugins.set(listOf("com.intellij.java"))
}

dependencies {
//    implementation("com.softwareloop:mybatis-generator-lombok-plugin:1.0")
    compileOnly("org.projectlombok:lombok:1.18.22")
    implementation("org.yaml:snakeyaml:1.29")
//    annotationProcessor("org.projectlombok:lombok:1.18.2");
//    testAnnotationProcessor("org.projectlombok:lombok:1.18.2");
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    withType<Javadoc> {
        options.encoding = "UTF-8"

    }

    patchPluginXml {
        // 起始支持版本，2020.3 (IDEA 201)
        sinceBuild.set("203")
        // 支持至更高版本的IDEA. 版本不设限
        untilBuild.set("")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
    runIde {
        jvmArgs("-Xmx4096m", "-XX:ReservedCodeCacheSize=512m", "-Xms128m")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
