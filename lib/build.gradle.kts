import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "dev.tilbrook"
version = "1.0.0"

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.plugin.agp)
    compileOnly(libs.plugin.kotlin)
    testImplementation(libs.plugin.agp)
    testImplementation(libs.plugin.kotlin)
    testImplementation(libs.junit)
    testImplementation(gradleTestKit())
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    website = "https://github.com/maruakmi/kotlin-jdk-target"
    vcsUrl = "https://github.com/maruakmi/kotlin-jdk-target"
    plugins {
        create("KotlinJvmTarget") {
            id = "dev.tilbrook.kotlin.bytecode-target"
            implementationClass = "dev.tilbrook.gradle.kotlin.bytecode.KotlinBytecodeTargetPlugin"
            displayName = "Kotlin Bytecode Target Plugin"
            description =
                "Sets the Kotlin and Java sourceCompatibility targetCompatibility and release flag to the target a specific bytecode target"
            tags = listOf("kotlin", "android")
        }
    }
}

tasks.withType<PluginUnderTestMetadata>().configureEach {
    dependsOn("compileKotlin", "compileTestKotlin", "compileJava", "compileTestJava")
    dependsOn("processResources", "processTestResources")

    pluginClasspath.setFrom(/* reset */)

    pluginClasspath.from(configurations.compileClasspath)
    pluginClasspath.from(configurations.testCompileClasspath)
    pluginClasspath.from(configurations.runtimeClasspath)
    pluginClasspath.from(provider { sourceSets.test.get().runtimeClasspath.files })
}

val javaVersion = JavaVersion.VERSION_17
tasks.withType(KotlinJvmCompile::class.java).configureEach {
    kotlinOptions.jvmTarget = javaVersion.toString()
}
// Kotlin requires the Java compatibility matches despite have no sources.
tasks.withType(JavaCompile::class.java).configureEach {
    sourceCompatibility = javaVersion.toString()
    targetCompatibility = javaVersion.toString()
}

// make sure to clean state before running tests after in-case of manual execution of fixtures.
tasks.register<Delete>("cleanTestFixtures") {
    doLast {
        delete("src/test/fixtures/jvm/build", "src/test/fixtures/android/build")
    }
}

tasks.withType<Test> {
    inputs.files(fileTree("src/test/fixtures"))

    // AGP 8 requires JDK 17+ and we want to to be compatible with previous JDKs
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })

    // clean up test fixtures
    dependsOn("cleanTestFixtures")
    // Our integration tests need a fully compiled jar
    dependsOn("assemble")

    // Those tests also need to know which version was built
    systemProperty("VERSION_NAME", version)
}