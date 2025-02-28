plugins {
	id("java")
}

allprojects {
	repositories {
		mavenCentral()
		maven {
			url = uri("https://maven.luis-st.net/libraries/")
		}
	}
}

dependencies {
	implementation(project(":agent"))
	implementation("net.luis:LUtils:${project.properties["LUtils"]}")
	implementation("org.ow2.asm:asm:${rootProject.properties["ASM"]}")
	implementation("com.google.guava:guava:${project.properties["GoogleGuava"]}")
	implementation("org.apache.logging.log4j:log4j-api:${project.properties["Log4jAPI"]}")
	implementation("org.apache.logging.log4j:log4j-core:${project.properties["Log4jCore"]}")
	implementation("org.apache.commons:commons-lang3:${project.properties["ApacheLang"]}")
	implementation("org.jetbrains:annotations:${rootProject.properties["JetBrainsAnnotations"]}")
}

tasks.register<JavaExec>("run") {
	dependsOn(project(":agent").tasks.named("build"))
	group = "run"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("net.luis.Main")
	jvmArgs("-javaagent:${projectDir}/agent/build/libs/agent.jar")
}

tasks.compileJava {
	options.compilerArgs.add("-parameters")
}
