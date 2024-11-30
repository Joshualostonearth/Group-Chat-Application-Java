This repository contains the source code for a basic chat application developed as a college project. It demonstrates fundamental concepts of client-server communication and database interaction.

**Important Note**

Security Considerations: This code is provided for educational purposes and has not been thoroughly audited for security vulnerabilities. Exercise caution when running it in a production environment.

Project Scope: The code is intended as a foundational example and may require further enhancements to meet robust security standards and advanced functionalities.

Project Structure
ChatApplicationBeginsHere.java: Main entry point for the application, starting the server and launching login windows for multiple users.

ChatServer.java: Manages the server-side logic, including client connections, message broadcasting, and port binding.

DatabaseManager.java: Handles database interactions for user authentication and chat history persistence.

ChatWindow.java: Implements the graphical user interface (GUI) for each chat client, displaying messages, sending input, and connecting to the server.

CreateAccount.java: Provides a separate window for creating new user accounts in the database.

LoginWindow.java: Handles user login attempts by validating credentials against the database.

**Disclaimer:**

This code is provided "as is" without warranty of any kind, express or implied. The authors are not liable for any damages arising from its use.
