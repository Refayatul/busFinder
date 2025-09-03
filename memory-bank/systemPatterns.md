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

### Advanced Search Flow
1. **User Input** → SearchBar component with real-time typing
2. **Debounced Processing** → 300ms delay to prevent excessive API calls
3. **Fuzzy Search Algorithm** → Multi-strategy matching:
   - Exact matches (highest priority)
   - Partial substring matches
   - Word boundary matches
   - Character sequence matching (typo tolerance)
   - Scoring system with weighted results
4. **Auto-complete Suggestions** → Top 5 matches displayed in dropdown
5. **Query Execution** → BusRepository.searchBuses() with optimized algorithms
6. **Search History** → Automatic persistence and deduplication
7. **Results Processing** → Sorted by relevance score
8. **UI Update** → LiveData state updates with smooth transitions

### Complete Data Loading Flow
1. **App Initialization** → BusViewModel.loadBusRoutes()
2. **JSON Parsing** → BusRepository.getAllBusRoutes()
3. **Data Validation** → Comprehensive error handling and logging
4. **Stop Extraction** → BusRepository.getAllStopNames() - 500+ unique stops
5. **Search Index Building** → Fuzzy search preparation with normalization
6. **State Management** → LiveData updates with loading/error states
7. **UI Rendering** → Lazy loading and efficient display

### Navigation & State Flow
1. **Screen Navigation** → Jetpack Navigation with NavHost
2. **State Preservation** → ViewModel survives configuration changes
3. **Search State** → From/to queries maintained across navigation
4. **History Integration** → Recent searches accessible from any screen
5. **Back Navigation** → Proper stack management with saved states

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
