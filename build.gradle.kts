plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        create(
            providers.gradleProperty("platformType").get(),
            providers.gradleProperty("platformVersion").get()
        )
        bundledPlugin("Git4Idea")
    }
}


intellijPlatform {
    pluginConfiguration {

        version.set(providers.gradleProperty("pluginVersion"))
        ideaVersion {
            sinceBuild.set(providers.gradleProperty("pluginSinceBuild").get())
        }
    }
}

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
tasks.processResources { filteringCharset = "UTF-8" }
