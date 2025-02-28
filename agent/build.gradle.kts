plugins {
	id("java")
}

dependencies {
	implementation("net.luis:LUtils:${rootProject.properties["LUtils"]}")
	implementation("org.ow2.asm:asm:${rootProject.properties["ASM"]}")
	implementation("org.ow2.asm:asm-commons:${rootProject.properties["ASM"]}")
	implementation("org.jetbrains:annotations:${rootProject.properties["JetBrainsAnnotations"]}")
}

tasks.jar {
	manifest {
		attributes(
			"Premain-Class" to "net.luis.agent.PreMain",
			"Can-Redefine-Classes" to "true",
			"Can-Retransform-Classes" to "true"
		)
	}
}
