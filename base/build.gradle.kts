plugins {
	id("java")
}

dependencies {

}

dependencies {
	implementation(project(":annotations"))
	
	implementation("net.luis:LUtils:${rootProject.properties["LUtils"]}")
	implementation("org.ow2.asm:asm:${rootProject.properties["ASM"]}")
	implementation("org.ow2.asm:asm-commons:${rootProject.properties["ASM"]}")
	implementation("org.jetbrains:annotations:${rootProject.properties["JetBrainsAnnotations"]}")
}
