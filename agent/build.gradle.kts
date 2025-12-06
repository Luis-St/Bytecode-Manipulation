plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.luis-st.net/libraries/")
    }
}

val asm: String by project
val jetBrainsAnnotations: String by project

dependencies {
    // ASM
    implementation("org.ow2.asm:asm:$asm")
    implementation("org.ow2.asm:asm-commons:$asm")
    implementation("org.ow2.asm:asm-tree:$asm")
    // Other
    implementation("org.jetbrains:annotations:$jetBrainsAnnotations") // Annotations
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Premain-Class" to "net.luis.agent.Main",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }
}
