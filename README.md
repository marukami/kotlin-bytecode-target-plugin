
### Installation

Root `build.gradle.kts`

```kotlin
plugins {
    id("dev.tilbrook.kotlin.bytecode-target") version "1.0.0" apply false
}
```

Each Kotlin module `build.gradle.kts`

```kotlin
plugins {
    id("dev.tilbrook.kotlin.bytecode-target")
}
```

### Bytecode Target

The default bytecode target is Java 17. You can set the Java target by adding `dev.tilbrook.kotlin.bytecodeTarget` to the root `gradle.properties`

```properties
dev.tilbrook.kotlin.bytecodeTarget=1.8
```