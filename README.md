# Machly Android App

This is a native Android application (Java) that synchronizes Users and Roles from an ASP.NET Core Web API and supports offline mode using `SQLiteOpenHelper`.

## Configuration

### 1. API URL
Open `app/src/main/java/com/machly/app/AppConstants.java` and replace the `BASE_URL` with your deployed Somee URL:
```java
public static final String BASE_URL = "https://YOUR_SOMEE_URL/";
```

### 2. Security
- The app uses `cleartextTrafficPermitted="true"` for development. For production, remove this from `AndroidManifest.xml` and `network_security_config.xml` and use HTTPS.
- `PrefsManager` currently uses standard `SharedPreferences`. For production, switch to `EncryptedSharedPreferences`.

## Database Inspection (ADB)

To export the local SQLite database for inspection:

1. **Connect your device/emulator** via USB.
2. **Run the following command** in your terminal:
   ```bash
   adb exec-out run-as com.machly.app cat /databases/machly.db > machly.db
   ```
   *Note: If `run-as` fails on some devices, use the `DbExportUtil` included in the app to copy the DB to external storage first.*

3. **Open `machly.db`** using [DB Browser for SQLite](https://sqlitebrowser.org/).

## Testing Checklist

1. **Setup**: Update `BASE_URL` in `AppConstants.java`.
2. **First Run (Online)**:
   - Launch app.
   - Login with valid credentials (server must return token).
   - Verify that `SyncManager` fetches Roles and Users (check Logcat `MachlySync`).
   - Verify `RecyclerView` shows the list of users.
3. **Offline Mode**:
   - Enable Airplane Mode.
   - Close and reopen the app.
   - Login with the same credentials (verified against local bcrypt hash).
   - Verify "Offline mode" toast appears.
   - Verify list loads from SQLite.
4. **Sync on Reconnect**:
   - Disable Airplane Mode.
   - Observe Logcat for "Network connected, starting sync...".
   - Verify "Sync successful" message.
5. **CRUD**:
   - Use the "+" button to add a user (saved locally, TODO: implement API push).
   - Edit a user and verify changes persist.

## Project Structure

- **api**: Retrofit client and service interface.
- **db**: `DbHelper` for SQLite operations.
- **models**: Data models for API (Response) and Local DB.
- **ui**: Activities and Adapters.
- **sync**: `SyncManager` for data synchronization.
- **utils**: Helper classes for Network, Encryption, etc.
