dependencies {
    implementation(project(":blockball-api"))
    implementation(project(":blockball-core"))
    compileOnly("com.google.inject:guice:5.0.1")
    compileOnly("org.spigotmc:spigot:1.17-R0.1-SNAPSHOT:remapped-mojang")
    testCompile("org.spigotmc:spigot:1.17-R0.1-SNAPSHOT:remapped-mojang")
}