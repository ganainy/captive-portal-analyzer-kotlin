# App Architecture

## Package Structure

### `screens` Package
Main package containing different screens, each with its dedicated ViewModel for business logic.

#### Screens Overview
- **Welcome Screen**
    - App introduction and goal presentation
    - Analysis startup guide

- **Analysis Screen**
    - Main interaction interface for captive portal
    - Database integration for interaction logging

- **About Screen**
    - FAQ section
    - Guidelines for optimal captive portal network reporting

- **Automatic Analysis Screen**
    - Network session data analysis
    - Google Gemini AI model integration

- **Manual Connect Screen**
    - Permission management
    - Step-by-step network connection guide
    - Captive portal detection setup

- **Settings Screen**
    - Theme control (Light/Dark)
    - Language selection (English/German)

- **Session Screen**
    - Network session data display
    - Create Privacy-focused image marking
    - Data review capabilities before submission
    - Upload and AI analysis options

- **Session List Screen**
    - Session overview display
    - Network type indicators (Normal/Captive Portal)
    - Storage status tracking (Local/Remote)

### Supporting Packages

#### `components` Package
- Pure UI composable components
- Reusable across different screens

#### `dataclasses` Package
- Data structure definitions
- Room database compatibility
- Captive portal data modeling

#### `datastore` Package
- DataStore interaction extensions
- Preference management:
    - First-time user detection
    - Theme mode storage
    - Language settings

#### `navigation` Package
- Navigation path definitions
- Screen dependency initialization
- Parameter passing management

#### `repository` Package
- ViewModel data operations
- Database interactions:
    - Local Room database operations
    - Remote server communications

#### `utils` Package
Helper methods for various functionalities:
- Network session management
- Captive portal hosting detection
- Connectivity status monitoring
