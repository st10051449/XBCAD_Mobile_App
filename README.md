Antonie Motors Mobile Application
Overview
The Antonie Motors Mobile Application enhances business management by providing tailored tools for Admins and Employees. With role-based access control (RBAC), each user accesses only the information and features relevant to their role.
This application is built using Kotlin in Android Studio (Iguana) and uses Firebase for data storage and authentication.

Key Features
Features Common to All Users
1	Registration: Users (business owners, managers, employees) can register. Admin/employee accounts require approval.
2	Login: Users log in via credentials or biometrics (if supported).
3	Data Storage: Persistent data storage using Firebase.
4	Settings: Update passwords, delete accounts, and log out.

Admin-Specific Features
1	Quote and Receipt Generator: Create, edit, and share quotes/receipts as PDFs with biometric authentication for edits.
2	Customer Management: View, edit, and manage customer profiles.
3	Vehicle Management: Manage vehicles, add details, and store images.
4	Stock and Parts Management: Track inventory, set minimum limits, and receive low-stock notifications.
5	Employee Registration Approval: Approve or deny employee registration requests.
6	Leave Management: Approve/deny employee leave requests.
7	Task System: Assign, track, and update employee tasks with status approvals.
8	Employee Analytics: Track performance and task completion.
9	Vehicle Services Management: Track, filter, and update vehicle services.
10	Home Page Vehicle Analytics: Quick stats on service progress.
11	In-Depth Vehicle Analytics: Analyze vehicle demographics, popular models, and registration trends.
12	Customer Analytics: Analyze customer demographics, types, and activity.

Employee-Specific Features
1	Leave Requests: Request leave and track approval status.
2	Task Management: View and update task statuses, set reminders, and track task history.
3	Leaderboard: Opt into a monthly-reset leaderboard for task completion.
4	Profile Management: View personal details and update profile pictures.

System Requirements
Software Requirements
•	IDE: Android Studio (Iguana)
•	Programming Language: Kotlin
•	Database: Firebase (Realtime Database, Storage, Authentication)
•	Target SDK: 35
•	Minimum SDK: 27
Hardware Requirements
•	Processor: Minimum Quad-Core 1.8 GHz.
•	RAM: 4GB or higher (8GB recommended).
•	Storage: At least 500 MB free disk space.
•	OS: Windows 10/11 or macOS Mojave (or later).
•	Device: Android device with API Level 27 (Android 8.1) or higher.

Setup Instructions
Step 1: Clone the Repository
1	Open Android Studio.
2	Navigate to File > New > Project from Version Control.
3	Enter the repository URL:


https://github.com/PerlaJbara/XBCAD7319-MobileAppPOE.git 
4	


5	Choose the desired directory and click Clone.


Step 2: Configure Firebase
1	Add your google-services.json file to the app/ directory.
2	Ensure Firebase services are enabled for:
◦	Realtime Database
◦	Authentication
◦	Storage

Step 3: Set the Target SDK
1	Open the build.gradle file in the app module.
2	Confirm that the target SDK is set to 35 and the minimum SDK to 27.

Step 4: Build and Run the App
1	Connect your Android device or start an emulator with API Level 27 or higher.
2	Click Run or press Shift+F10 in Android Studio.
3	Log in or register to explore features.


Contributors
•	Perla Jbara
•	Daniel Antonie
•	Gabriella Janssen
•	Mauro Coelho
•	Lee Knowles


For any questions, feel free to contact us via the repository issues section.
