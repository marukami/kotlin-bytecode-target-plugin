package dev.tilbrook.gradle.kotlin.bytecode

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

class KotlinBytecodeTargetPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val jvmTarget = properties.getOrDefault("dev.tilbrook.kotlin.bytecodeTarget", "17")
            .toString()
            .let { JavaVersion.toVersion(it) }

        pluginManager.withPlugin("com.android.application") {
            androidTarget<ApplicationExtension>(target, jvmTarget)
        }
        pluginManager.withPlugin("com.android.library") {
            androidTarget<LibraryExtension>(target, jvmTarget)
        }
        pluginManager.withPlugin("org.jetbrains.kotlin.android") {
            jvmTarget(target, jvmTarget)
        }
        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            jvmTarget(target, jvmTarget)
        }
    }

    private fun jvmTarget(project: Project, javaVersion: JavaVersion) {
        project.tasks.withType<KotlinCompilationTask<KotlinJvmCompilerOptions>> {
            project.logger.info("Configuring kotlin jdk-target with $javaVersion")
            compilerOptions {
                freeCompilerArgs.addAll("-Xjdk-release=$javaVersion")
                jvmTarget.set(JvmTarget.valueOf("JVM_${javaVersion.majorVersion}"))
            }
        }

        // Kotlin requires the Java compatibility matches despite have no sources.
        project.tasks.withType<JavaCompile> {
            project.logger.info("Configuring java source & target compatibility with jdk $javaVersion")
            sourceCompatibility = javaVersion.toString()
            targetCompatibility = javaVersion.toString()
        }
    }

    private inline fun <reified T : CommonExtension<*, *, *, *, *, *>> androidTarget(project: Project, javaVersion: JavaVersion) {
        project.logger.info("Configuring android source & target compatibility with jdk $javaVersion")
        project.tasks.withType<KotlinCompilationTask<KotlinJvmCompilerOptions>> {
            compilerOptions {
              noJdk.set(false)
            }
        }
        project.extensions.configure<T> {
            compileOptions {
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
            }
        }
    }
}