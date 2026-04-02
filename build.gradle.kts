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
    implementation("com.google.genai:google-genai:0.1.0") 
    
    // MariaDB JDBC Treiber
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.3")

    // SLF4J Simple Logger um die SLF4J Warnung zu beheben
    implementation("org.slf4j:slf4j-simple:2.0.12")


}

tasks.test {
    useJUnitPlatform()
}