# Captive Portal Analyzer

## Overview

This app focuses on analyzing captive portals. It provides an interface for interacting with captive portals directly through the app by gathering POST request bodies, headers, creating sessions, capturing screenshots, and analyzing their data collection and privacy using AI-powered analysis. Additionally, the app allows uploading analyzed data for further examination by our team.
> **Note:** The app is designed to work with physical devices only since it interacts with real captive portals. 

---
## Project Structure
For detailed information about the project files structure and architecture, see [Project Structure](APP_STRUCTURE.md).

## Features

- Interact with captive portals seamlessly.
- Automatically gather and manage POST request bodies and headers.
- Filter and view collected requests details.
- Automatically create and manage sessions effortlessly.
- Automatically capture screenshots of portal interactions.
- Perform AI-powered privacy and data collection analysis.
- Upload analyzed data for extended review by our team.
> All login/sign up data is stored locally and not sent to the backend unless users choose to upload,
> nonetheless it is preferable to use fake data when signing up/logging in.
---

## Tech Stack

- **Language:** Kotlin
- **Framework:** Jetpack Compose
- **Architecture:** MVVM (Model-View-ViewModel)
- **Libraries:**
    - Room for local database
    - Coil for image loading
    - Gemini AI SDK for AI integration
    - Compose Markdown for rendering markdown content
    - Android Request Inspector WebView for network analysis
- **Backend:**
    - Firebase Firestore for data storage
    - Firebase Storage for image storage

---

## Preview

### Video Demonstrations:

#### Collecting information about a captive portal network:
![Create Session](preview/gifs/create_session.gif)
<br>
[Download video for better quality](preview/videos/create-session.mp4)

#### Analyze collected data with AI:
![Mark and Analyze with AI](preview/gifs/analyze_with_ai.gif)
<br>
[Download video for better quality](preview/videos/mark-analyze-with-ai_blurred.mp4)

### Screenshots:
| Feature                                                                              | Screenshot                          |
|--------------------------------------------------------------------------------------|-------------------------------------|
| **Interacting with captive portal from the app**                                     | ![2.png](preview/screenshots/2.png) |
| **Detailed View of collected data**                                                  | ![3.png](preview/screenshots/3.png) |
| **Filter collected requests**                                                        | ![8.png](preview/screenshots/8.png) |
| **Show each collected request in details**                                           | ![9.png](preview/screenshots/9.png) |
| **AI Insights about the captive portal**                                             | ![4.png](preview/screenshots/4.png) |
| **Control language & theme**                                                         | ![5.png](preview/screenshots/5.png) |
| **Fetched request bodies saved to backend**                                          | ![6.png](preview/screenshots/6.png) |
| **Screenshots related to Privacy policy/ToS<br/> uploaded to back end for analysis** | ![7.png](preview/screenshots/7.png) |
---

### 🆕: Integration with [PCAPDroid](https://github.com/emanuele-f/PCAPdroid) (See branch `android-pcap-api`)
| Feature                                                                                                                  | Screenshot                                     |
|--------------------------------------------------------------------------------------------------------------------------|------------------------------------------------|
| ** Instructions screen to help user do the one time setup of PCAPDroid **                                                | ![pcap-setup.gif](preview/gifs/pcap-setup.gif) |
| ** User can choose to use the JS injection WebView only or JS injection WebView + PCAPDroid packet capture **            | ![10.jpg](preview/screenshots/10.jpg)          |
| ** The resulting .pcap file from PCAPDroid is uploaded to the remote server with the previously collected network data** | ![11.jpg](preview/screenshots/11.jpg)          |
---
> **Note:** The starting and ending of packet capture is directly handled within the app.

## How to Run the App

1. Clone the repository:
   ```bash
   git clone https://github.com/ganainy/captive-portal-analyzer-kotlin.git
   ```

2. Set up API Keys and Configuration:

   a. **Gemini AI API Key:**
    - Get your API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
    - Add your Gemini AI API key to `local.properties` file in the project root 
      ```properties
      apiKey=your_api_key_here
      ```

   b. **Firebase Setup:**
    - Go to [Firebase Console](https://console.firebase.google.com/)
    - Create a new project or select an existing one
    - Download the `google-services.json` file
    - Place `google-services.json` in the `app` directory

3. Build and run the project on a physical device.


---

## Contributions

Contributions are welcome! Feel free to submit a pull request or report issues to enhance the app.

---
