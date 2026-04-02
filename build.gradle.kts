plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Google Gen AI SDK (für einfache API Keys)
    implementation("com.google.genai:google-genai:0.1.0") // Überprüfe ggf. die neueste Version auf Maven Central
}

tasks.test {
    useJUnitPlatform()
}
