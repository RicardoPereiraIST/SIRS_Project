# SIRS_Project

README.md

- Platforms used to run the project:
  - Server side: Kali GNU/Rolling Linux 32-bit OR Ubuntu 15.04 64-bit
  - Server side code: Comipled with Java 1.8.0_111 

  - Client side: Android 6.0.1 (Marshmallow)
  - Client side code: Compiled with Android SDK 25

- Setup Instructions:
  Server setup:
  - 1. Navigate to Server/ folder and compile with the command: javac *.java
  - 2. Execute: java Main

  Client side:
  - 1. Connect the Android phone to the Android Studio.
  - 2. Compile and execute the code. OR
       Install the APK that comes with the Source Code found in: SIRSApp/app/app.apk

- Use case example:
  - 1. On the server terminal, register a user selecting the Register command (1) and providing a user name and a password.
  - 2. Login with the credentials used on step 1.
  - 2.1. Copy files to the folder: Server/Files/<username>/
  - 2.2. Test files can be found on the Resources/ folder.
  - 3. Select the Pairing command (1). The terminal will generate a random token so that the user can type it on the phone.
  - 4. Select the Settings option (top right corner) and enter the server's ip address.
  - 4.1. On the mobile phone, select the Pair Phone option and type in the token.
  - 5. The terminal will send the session key and ask the user if he want to unlock the files right now. Select "yes".
  - 6. On the mobile phone select Unlock Phone and the two devices will begin to exchange nonces.
  - 7. At any time, the files can be locked by selecting the option Lock Files on the phone. To unlock, the option "Unlock files with phone" must be selected on the server, afterwards, select the Unlock files option on the Android.
  - 8. On the folder where the files were inserted (Step 2.1) the files can be observed being ciphered and deciphered as the program runs. (Encrypted files will have a "_enc" string on the name)