package dev.tilbrook.gradle.kotlin.jdk.target

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JdkTargetTest {

  private lateinit var buildFile: File
  private lateinit var gradleProperties: File

  @get:Rule
  val testProjectDir = TemporaryFolder()


  @Before
  fun setup() {
    val settingsFile = testProjectDir.newFile("settings.gradle.kts")
    settingsFile.writeText(setttings())
    File("./src/testFixture").copyRecursively(testProjectDir.root)
    gradleProperties = testProjectDir.newFile("gradle.properties")
    buildFile = testProjectDir.newFile("build.gradle.kts")
  }

  @Test
  fun `android targeting java17 using JDK21 emit Kotlin removeFirst`() {
    val gradleVersion = GradleVersion.version("8.7")
    val javaVersion = JavaVersion.VERSION_17
    setBytecodeTarget(javaVersion)
    build(
      listOf(
        "com.android.library",
        "org.jetbrains.kotlin.android",
      ),
      """
        android {
          namespace = "dev.tilbrook.mylibrary"
          compileSdk = 34
        
          defaultConfig {
            minSdk = 24
          }
        }
      """.trimIndent()
    )

    val result = runner(gradleVersion) {
      withArguments("--info", "assembleDebug")
    }.build()

    println(result.output)

    val bytecode = readBytecode(isAndroid = true)
    assertRemoveFirst(bytecode, javaVersion, isAndroid = true)
  }

  @Test
  fun `android targeting java21 using JDK21 emit Kotlin removeFirst`() {
    val gradleVersion = GradleVersion.version("8.7")
    val javaVersion = JavaVersion.VERSION_21
    setBytecodeTarget(javaVersion)
    build(
      listOf(
        "com.android.library",
        "org.jetbrains.kotlin.android",
      ),
      """
        android {
          namespace = "dev.tilbrook.mylibrary"
          compileSdk = 34
        
          defaultConfig {
            minSdk = 24
          }
        }
      """.trimIndent()
    )

    val result = runner(gradleVersion) {
      withArguments("--info", "assembleDebug")
    }.build()

    println(result.output)

    val bytecode = readBytecode(isAndroid = true)
    assertRemoveFirst(bytecode, javaVersion, isAndroid = true)
  }

  @Test
  fun `targeting java17 using JDK21 should emit Kotlin removeFirst`() {
    val gradleVersion = GradleVersion.version("8.7")
    val javaVersion = JavaVersion.VERSION_17
    setBytecodeTarget(javaVersion)
    build(listOf("org.jetbrains.kotlin.jvm"))

    val result = runner(gradleVersion) {
      withArguments("--info", "assemble")
    }.build()

    println(result.output)

    val bytecode = readBytecode(isAndroid = false)
    assertRemoveFirst(bytecode, javaVersion)
  }

  @Test
  fun `targeting java21 using JDK21 should emit Java removeFirst`() {
    val gradleVersion = GradleVersion.version("8.7")
    val javaVersion = JavaVersion.VERSION_21
    setBytecodeTarget(javaVersion)
    build(listOf("org.jetbrains.kotlin.jvm"))

    val result = runner(gradleVersion) {
      withArguments("--info", "assemble")
    }.build()

    println(result.output)

    val bytecode = readBytecode(isAndroid = false)
    assertRemoveFirst(bytecode, javaVersion)
  }

  private fun runner(
    gradleVersion: GradleVersion,
    gradleRunner: GradleRunner.() -> GradleRunner
  ): GradleRunner {
    return GradleRunner.create()
      .withGradleVersion(gradleVersion.version)
      .withPluginClasspath()
      .withProjectDir(testProjectDir.root)
      .run(gradleRunner)
  }

  private fun setBytecodeTarget(version: JavaVersion) {
    gradleProperties.writeText(
      """
        dev.tilbrook.kotlin.bytecodeTarget=${version}
        """.trimIndent()
    )
  }

  private fun readBytecode(isAndroid: Boolean): String {
    val file = File(testProjectDir.root, "bytecode.out")
    val process = ProcessBuilder(
      "javap",
      "-v",
      if (isAndroid) "${testProjectDir.root}/build/tmp/kotlin-classes/debug/dev/tilbrook/test/jvm/RemoveFirstKt.class"
      else "${testProjectDir.root}/build/classes/kotlin/main/dev/tilbrook/test/jvm/RemoveFirstKt.class"
    )
      .apply {
        redirectOutput(file)
      }
      .start()
    process.waitFor()
    return file.readText().also { println("bytecode:\n$it") }
  }

  private fun assertRemoveFirst(bytecode: String, javaVersion: JavaVersion, isAndroid: Boolean = false) {
    val isCollectionKt = javaVersion < JavaVersion.VERSION_21
    if (isAndroid || isCollectionKt) {
      assert(bytecode.contains("kotlin/collections/CollectionsKt.removeFirst:(Ljava/util/List;)Ljava/lang/Object;"))
      assert(!bytecode.contains("InterfaceMethod java/util/List.removeFirst:()Ljava/lang/Object;"))
    } else {
      assert(!bytecode.contains("kotlin/collections/CollectionsKt.removeFirst:(Ljava/util/List;)Ljava/lang/Object;"))
      assert(bytecode.contains("InterfaceMethod java/util/List.removeFirst:()Ljava/lang/Object;"))
    }
  }


  private fun setttings(): String = """
        pluginManagement {
            repositories {
                google {
                    content {
                        includeGroupByRegex("com\\.android.*")
                        includeGroupByRegex("com\\.google.*")
                        includeGroupByRegex("androidx.*")
                    }
                }
                mavenCentral()
                gradlePluginPortal()
            }
        }
        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
            repositories {
                google()
                mavenCentral()
            }
        }

        rootProject.name = "TestLibrary"
    """.trimIndent()

  private fun build(plugins: List<String>, block: String = "") {
    val ids = plugins.joinToString("\n", prefix = "    ") {
      "id(\"$it\")"
    }
    """      
      plugins {
      $ids
          id("dev.tilbrook.kotlin.bytecode-target")
      }
      $block
          
      """.trimIndent()
      .also { buildFile.writeText(it) }
  }
}