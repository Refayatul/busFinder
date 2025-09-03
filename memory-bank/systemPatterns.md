# BUSFinder System Patterns

## Architecture Overview
BUSFinder follows the MVVM (Model-View-ViewModel) architecture pattern with repository pattern for data management. The app is structured into clear layers for maintainability and testability.

## Core Architecture Layers

### Data Layer
```
data/
├── local/          # Room database implementation
├── model/          # Data models and entities
└── repository/     # Data access abstraction
```

### UI Layer
```
ui/
├── component/      # Reusable UI components
├── screen/         # Screen-level composables
└── theme/         # App theming and styling
```

### ViewModel Layer
```
viewmodel/          # Business logic and state management
```

## Key Design Patterns

### Repository Pattern
- **BusRepository**: Abstracts data access logic
- Provides clean API for ViewModels
- Handles data source switching (local/remote)
- Implements error handling and data transformation

### MVVM Pattern
- **ViewModels**: Contain business logic and state
- **Views**: Pure UI components that observe ViewModel state
- **Models**: Data structures and business entities
- Clear separation of concerns

### Singleton Pattern
- **BusDatabase**: Single instance Room database
- Ensures thread-safe database operations
- Provides DAOs for data access

## Component Relationships

### Data Flow
```
UI Components → ViewModels → Repository → Database/Local Files
                      ↓
               State Updates → UI Recomposition
```

### Key Components
- **MainActivity**: App entry point and navigation host
- **BusViewModel**: Central state management for bus operations
- **BusDatabase**: Local SQLite database via Room
- **BusRepository**: Data access coordinator
- **SearchBar**: Reusable search input component
- **BusItem**: Route display component

## Critical Implementation Paths

### Search Flow
1. User input → SearchBar component
2. Input validation → BusViewModel
3. Query execution → BusRepository
4. Database query → SearchHistoryDao
5. Results processing → ViewModel state update
6. UI update → Search results display

### Route Display Flow
1. Route selection → BusItem click
2. Navigation trigger → MainActivity
3. Data loading → BusRepository
4. JSON parsing → Bus route data
5. UI rendering → Route detail screen

## Database Schema
- **BusRoute**: Route information with stops and metadata
- **SearchHistoryItem**: User search history with timestamps
- Foreign key relationships for data integrity

## State Management
- **LiveData/Flow**: Reactive data streams from ViewModels
- **State hoisting**: UI state managed at appropriate levels
- **Single source of truth**: Repository as central data authority

## Error Handling
- **Try-catch blocks**: Database operations
- **Nullable types**: Safe data access
- **User feedback**: Error states in UI
- **Graceful degradation**: Offline functionality

## Performance Considerations
- **Lazy loading**: Data loaded on demand
- **Background processing**: Database operations on IO threads
- **Memory management**: Efficient data structures
- **UI optimization**: Minimal recompositions
