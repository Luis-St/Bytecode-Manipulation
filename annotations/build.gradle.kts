plugins {
	id("java")
}

dependencies {
	implementation("net.luis:LUtils:${rootProject.properties["LUtils"]}")
	implementation("org.ow2.asm:asm:${rootProject.properties["ASM"]}")
	implementation("org.jetbrains:annotations:${rootProject.properties["JetBrainsAnnotations"]}")
}
