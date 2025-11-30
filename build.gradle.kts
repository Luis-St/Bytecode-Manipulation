plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.luis-st.net/libraries/")
    }
}

val lUtils: String by project
val asm: String by project
val googleGuava: String by project
val log4jAPI: String by project
val log4jCore: String by project
val apacheLang: String by project
val jetBrainsAnnotations: String by project

dependencies {
    // Modules
    compileOnly(project(":agent"))
    // Maven
    implementation("net.luis:LUtils:$lUtils") // Utility
    // ASM
    implementation("org.ow2.asm:asm:$asm")
    implementation("org.ow2.asm:asm-commons:$asm")
    implementation("org.ow2.asm:asm-tree:$asm")
    // Google
    implementation("com.google.guava:guava:$googleGuava") // Utility
    // Apache
    implementation("org.apache.logging.log4j:log4j-api:$log4jAPI") // Logging
    implementation("org.apache.logging.log4j:log4j-core:$log4jCore") // Logging
    implementation("org.apache.commons:commons-lang3:$apacheLang") // Utility
    // Other
    implementation("org.jetbrains:annotations:$jetBrainsAnnotations") // Annotations
}

tasks.register<JavaExec>("run") {
    dependsOn(project(":agent").tasks.named("build"))
    group = "run"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.luis.Main")
    jvmArgs("-javaagent:${projectDir}/agent/build/libs/agent.jar")
}

tasks.register<JavaExec>("testing") {
    dependsOn(project(":agent").tasks.named("build"))
    group = "run"
    classpath = sourceSets.main.get().runtimeClasspath + project(":agent").sourceSets.main.get().runtimeClasspath
    mainClass.set("net.luis.Testing")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
