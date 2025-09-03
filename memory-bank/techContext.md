# BUSFinder Technical Context

## Technology Stack

### Core Technologies
- **Language**: Kotlin 1.8.10 (Latest stable)
- **Platform**: Android API 21+ (Android 5.0+) to API 34 (Android 14)
- **Build System**: Gradle 8.0+ with Kotlin DSL
- **Architecture**: MVVM with Repository pattern (Production-ready)

### Key Libraries & Frameworks (Actual Versions)
- **UI Framework**: Jetpack Compose BOM (2023.10.01)
- **Database**: Room 2.6.1 (SQLite abstraction with full type safety)
- **Async Programming**: Kotlin Coroutines 1.7.3 & Flow
- **JSON Processing**: Google Gson 2.10.1 (for JSON parsing)
- **Material Design**: Material 3 Components (2023.10.01)
- **Navigation**: Jetpack Navigation Compose 2.7.5
- **Lifecycle**: ViewModel & LiveData 2.7.0
- **Architecture Components**: Activity Compose 1.8.2

### Development Tools
- **IDE**: Android Studio
- **Version Control**: Git
- **CI/CD**: GitHub Actions (planned)
- **Code Quality**: Kotlin Lint, Detekt (planned)

## Development Environment Setup

### Prerequisites
- **JDK**: OpenJDK 11 or 17
- **Android Studio**: Arctic Fox or later
- **Android SDK**: API 33 (Android 13)
- **Device/Emulator**: Android 5.0+ for testing

### Project Configuration
- **Gradle Version**: 8.0+
- **Kotlin Version**: 1.8.10
- **Android Gradle Plugin**: 8.1.0
- **Compile SDK**: 34
- **Target SDK**: 34
- **Min SDK**: 21

## Dependencies Management

### Core Dependencies (app/build.gradle.kts)
```kotlin
dependencies {
    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    
    // Architecture Components
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.activity:activity-compose")
    
    // Room Database
    implementation("androidx.room:room-runtime")
    implementation("androidx.room:room-ktx")
    ksp("androidx.room:room-compiler")
    
    // JSON Processing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
}
```

### Version Catalog (gradle/libs.versions.toml)
- Centralized dependency management
- Easy version updates
- Consistent dependency versions across modules

## Technical Constraints

### Platform Limitations
- **Android API Level**: Minimum 21 (Android 5.0)
- **Storage**: Limited device storage for database
- **Memory**: Optimize for low-end devices
- **Network**: Offline-first design required

### Performance Requirements
- **Cold Start**: < 3 seconds
- **Search Response**: < 1 second
- **Memory Usage**: < 100MB
- **Battery Impact**: Minimal background processing

### Security Considerations
- **Data Storage**: Local SQLite encryption (future)
- **User Data**: No sensitive information stored
- **Permissions**: Minimal required (location optional)
- **Privacy**: GDPR compliance ready

## Build Configuration

### Build Variants
- **Debug**: Development builds with logging
- **Release**: Production builds with ProGuard
- **Staging**: Testing environment (planned)

### ProGuard Rules
- **Database Classes**: Keep Room entities
- **Compose Classes**: Preserve composable functions
- **Reflection**: Allow necessary reflection for libraries

## Tool Usage Patterns

### Code Organization
- **Package Structure**: Feature-based organization
- **Naming Conventions**: CamelCase for classes, snake_case for resources
- **File Structure**: Clear separation of concerns

### Testing Strategy
- **Unit Tests**: ViewModels and Repository logic
- **Integration Tests**: Database operations
- **UI Tests**: Compose component testing (planned)
- **Instrumentation Tests**: End-to-end scenarios

### Code Quality
- **Linting**: Kotlin Android Lint rules
- **Formatting**: Spotless with Kotlin style
- **Documentation**: KDoc for public APIs
- **Commit Messages**: Conventional commits

## Deployment & Distribution

### Build Process
1. **Code Quality**: Lint and test execution
2. **Build**: Gradle assembleRelease
3. **Signing**: Automatic signing configuration
4. **Distribution**: Google Play Store deployment

### Release Management
- **Versioning**: Semantic versioning (1.0.0)
- **Changelogs**: Generated from commit messages
- **Beta Testing**: Internal testing track
- **Rollout Strategy**: Phased rollout for stability

## Future Technical Roadmap
- **State Management**: Migrate to MVI pattern
- **Dependency Injection**: Full Hilt implementation
- **Testing**: Comprehensive test coverage
- **Performance**: App startup optimization
- **Security**: Data encryption implementation
