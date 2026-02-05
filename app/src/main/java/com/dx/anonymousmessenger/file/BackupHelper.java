package com.dx.anonymousmessenger.file;

import android.content.Context;
import android.util.Log;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.util.Hex;

import net.sqlcipher.database.SQLiteDatabase;

import org.whispersystems.libsignal.InvalidKeyException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupHelper {
    private static final String TAG = "BackupHelper";
    private static final int BUFFER_SIZE = 8192;

    /**
     * Creates an encrypted backup of all app data (database, files, Tor state)
     * @param app The application context
     * @param encryptionKey The encryption key to use for the backup file
     * @param outputDir Directory where backup should be saved
     * @return Path to the created backup file, or null if failed
     */
    public static String createBackup(DxApplication app, byte[] encryptionKey, File outputDir) {
        if (app == null || encryptionKey == null || outputDir == null) {
            Log.e(TAG, "Invalid parameters for backup");
            return null;
        }

        ZipOutputStream zos = null;
        File tempZipFile = null;
        File encryptedBackupFile = null;

        try {
            // Create output directory if it doesn't exist
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                Log.e(TAG, "Failed to create output directory: " + outputDir.getAbsolutePath());
                return null;
            }

            // Create timestamped filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String tempZipName = "backup_temp_" + timestamp + ".zip";
            String backupName = "AnonymousMessenger_Backup_" + timestamp + ".abk";

            tempZipFile = new File(app.getCacheDir(), tempZipName);
            encryptedBackupFile = new File(outputDir, backupName);

            // Create the ZIP output stream
            zos = new ZipOutputStream(new FileOutputStream(tempZipFile));

            // Backup 1: Database files
            backupDatabase(app, zos);

            // Backup 2: App files (encrypted attachments, etc.)
            backupAppFiles(app, zos);

            // Backup 3: Tor-related files and state
            backupTorFiles(app, zos);

            // Backup 4: Shared preferences
            backupSharedPreferences(app, zos);

            // Backup 5: Account data
            backupAccountData(app, zos);

            zos.finish();
            zos.close();
            zos = null;

            // Encrypt the backup ZIP file
            encryptBackupFile(tempZipFile, encryptedBackupFile, encryptionKey);

            // Clean up temp file
            if (tempZipFile.exists()) {
                tempZipFile.delete();
            }

            Log.i(TAG, "Backup created successfully: " + encryptedBackupFile.getAbsolutePath());
            return encryptedBackupFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Backup creation failed: " + e.getMessage(), e);

            // Clean up on failure
            if (tempZipFile != null && tempZipFile.exists()) {
                tempZipFile.delete();
            }
            if (encryptedBackupFile != null && encryptedBackupFile.exists()) {
                encryptedBackupFile.delete();
            }

            return null;
        } finally {
            if (zos != null) {
                try { zos.close(); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Restores app data from an encrypted backup file
     * @param app The application context
     * @param backupFilePath Path to the backup file
     * @param encryptionKey The encryption key used for the backup
     * @return true if restore succeeded, false otherwise
     */
    public static boolean restoreBackup(DxApplication app, String backupFilePath, byte[] encryptionKey) {
        if (app == null || backupFilePath == null || encryptionKey == null) {
            Log.e(TAG, "Invalid parameters for restore");
            return false;
        }

        File backupFile = new File(backupFilePath);
        if (!backupFile.exists()) {
            Log.e(TAG, "Backup file not found: " + backupFilePath);
            return false;
        }

        File tempZipFile = null;
        ZipInputStream zis = null;

        try {
            // Create temp file for decrypted backup
            String tempName = "restore_temp_" + System.currentTimeMillis() + ".zip";
            tempZipFile = new File(app.getCacheDir(), tempName);

            // Decrypt the backup file
            decryptBackupFile(backupFile, tempZipFile, encryptionKey);

            // Extract and restore from ZIP
            zis = new ZipInputStream(new FileInputStream(tempZipFile));
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.startsWith("databases/")) {
                    restoreDatabaseEntry(app, entry, zis);
                } else if (entryName.startsWith("files/")) {
                    restoreFileEntry(app, entry, zis);
                } else if (entryName.startsWith("shared_prefs/")) {
                    restoreSharedPrefsEntry(app, entry, zis);
                } else if (entryName.startsWith("tor/") || entryName.startsWith("app_tor/") || entryName.startsWith("app_data/")) {
                    restoreTorEntry(app, entry, zis);
                } else if (entryName.equals("account_data.xml")) {
                    restoreAccountData(app, zis);
                }

                zis.closeEntry();
            }

            // Reinitialize database after restore
//            app.initDb();

            Log.i(TAG, "Restore completed successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Restore failed: " + e.getMessage(), e);
            return false;
        } finally {
            if (zis != null) {
                try { zis.close(); } catch (IOException ignored) {}
            }
            if (tempZipFile != null && tempZipFile.exists()) {
                tempZipFile.delete();
            }
        }
    }

    // ==================== BACKUP METHODS ====================

    private static void backupDatabase(DxApplication app, ZipOutputStream zos) throws IOException {
        File dataDir = new File(app.getApplicationInfo().dataDir);
        File dbDir = new File(dataDir, "databases");

        if (dbDir.exists() && dbDir.isDirectory()) {
            File[] dbFiles = dbDir.listFiles();
            if (dbFiles != null) {
                for (File dbFile : dbFiles) {
                    if (dbFile.isFile() && dbFile.getName().endsWith(".db")) {
                        addToZip(zos, "databases/" + dbFile.getName(), dbFile);
                    }
                }
            }
        }

        // Also backup any journal files
        File[] journalFiles = dbDir.listFiles((dir, name) ->
                name.endsWith(".db-journal") || name.endsWith(".db-wal") || name.endsWith(".db-shm"));
        if (journalFiles != null) {
            for (File journalFile : journalFiles) {
                addToZip(zos, "databases/" + journalFile.getName(), journalFile);
            }
        }
    }

    private static void backupAppFiles(DxApplication app, ZipOutputStream zos) throws IOException {
        File filesDir = app.getFilesDir();
        if (filesDir.exists() && filesDir.isDirectory()) {
            addDirectoryToZip(zos, "files/", filesDir);
        }
    }

    private static void backupTorFiles(DxApplication app, ZipOutputStream zos) throws IOException {
        File dataDir = new File(app.getApplicationInfo().dataDir);

        // Check common Tor directory locations
        String[] torDirNames = {"app_tor", "app_data", "tor", "tor_data"};
        for (String dirName : torDirNames) {
            File torDir = new File(dataDir, dirName);
            if (torDir.exists() && torDir.isDirectory()) {
                addDirectoryToZip(zos, "tor/" + dirName + "/", torDir);
            }
        }

        // Also check in files directory
        File torInFiles = new File(app.getFilesDir(), "tor");
        if (torInFiles.exists() && torInFiles.isDirectory()) {
            addDirectoryToZip(zos, "tor/files_tor/", torInFiles);
        }

        // Backup Tor configuration if exists
        File torConfig = new File(dataDir, "torrc");
        if (torConfig.exists()) {
            addToZip(zos, "tor/torrc", torConfig);
        }
    }

    private static void backupSharedPreferences(DxApplication app, ZipOutputStream zos) throws IOException {
        File dataDir = new File(app.getApplicationInfo().dataDir);
        File prefsDir = new File(dataDir, "shared_prefs");

        if (prefsDir.exists() && prefsDir.isDirectory()) {
            File[] prefsFiles = prefsDir.listFiles();
            if (prefsFiles != null) {
                for (File prefsFile : prefsFiles) {
                    if (prefsFile.isFile() && prefsFile.getName().endsWith(".xml")) {
                        addToZip(zos, "shared_prefs/" + prefsFile.getName(), prefsFile);
                    }
                }
            }
        }
    }

    private static void backupAccountData(DxApplication app, ZipOutputStream zos) throws IOException {
        // You might want to serialize account data to XML/JSON
        // For now, we'll create a placeholder
        String accountData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<account>\n" +
                "  <timestamp>" + System.currentTimeMillis() + "</timestamp>\n" +
                "  <version>1.0</version>\n" +
                "</account>";

        zos.putNextEntry(new ZipEntry("account_data.xml"));
        zos.write(accountData.getBytes());
        zos.closeEntry();
    }

    // ==================== RESTORE METHODS ====================

    private static void restoreDatabaseEntry(DxApplication app, ZipEntry entry, ZipInputStream zis) throws IOException {
        String entryName = entry.getName();
        String dbName = entryName.substring("databases/".length());

        File dataDir = new File(app.getApplicationInfo().dataDir);
        File dbDir = new File(dataDir, "databases");

        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        File dbFile = new File(dbDir, dbName);

        try (FileOutputStream fos = new FileOutputStream(dbFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }

        Log.d(TAG, "Restored database file: " + dbName);
    }

    private static void restoreFileEntry(DxApplication app, ZipEntry entry, ZipInputStream zis) throws IOException {
        String entryName = entry.getName();
        String relativePath = entryName.substring("files/".length());

        File targetFile = new File(app.getFilesDir(), relativePath);
        File parentDir = targetFile.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }

        Log.d(TAG, "Restored file: " + relativePath);
    }

    private static void restoreSharedPrefsEntry(DxApplication app, ZipEntry entry, ZipInputStream zis) throws IOException {
        String entryName = entry.getName();
        String prefsName = entryName.substring("shared_prefs/".length());

        File dataDir = new File(app.getApplicationInfo().dataDir);
        File prefsDir = new File(dataDir, "shared_prefs");

        if (!prefsDir.exists()) {
            prefsDir.mkdirs();
        }

        File prefsFile = new File(prefsDir, prefsName);

        try (FileOutputStream fos = new FileOutputStream(prefsFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }

        Log.d(TAG, "Restored shared preferences: " + prefsName);
    }

    private static void restoreTorEntry(DxApplication app, ZipEntry entry, ZipInputStream zis) throws IOException {
        String entryName = entry.getName();

        // Determine target directory based on entry name pattern
        File targetDir;
        if (entryName.startsWith("tor/app_tor/")) {
            targetDir = new File(app.getApplicationInfo().dataDir, "app_tor");
        } else if (entryName.startsWith("tor/app_data/")) {
            targetDir = new File(app.getApplicationInfo().dataDir, "app_data");
        } else if (entryName.startsWith("tor/files_tor/")) {
            targetDir = new File(app.getFilesDir(), "tor");
        } else {
            targetDir = new File(app.getApplicationInfo().dataDir, "tor");
        }

        String relativePath;
        if (entryName.startsWith("tor/app_tor/")) {
            relativePath = entryName.substring("tor/app_tor/".length());
        } else if (entryName.startsWith("tor/app_data/")) {
            relativePath = entryName.substring("tor/app_data/".length());
        } else if (entryName.startsWith("tor/files_tor/")) {
            relativePath = entryName.substring("tor/files_tor/".length());
        } else {
            relativePath = entryName.substring("tor/".length());
        }

        File targetFile = new File(targetDir, relativePath);
        File parentDir = targetFile.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }

        Log.d(TAG, "Restored Tor file: " + relativePath);
    }

    private static void restoreAccountData(DxApplication app, ZipInputStream zis) throws IOException {
        // Read and parse account data
        // For now, just consume the stream
        byte[] buffer = new byte[BUFFER_SIZE];
        while (zis.read(buffer) > 0) {
            // Read and discard for now
        }
        Log.d(TAG, "Account data restored");
    }

    // ==================== ENCRYPTION/DECRYPTION ====================

    private static void encryptBackupFile(File inputFile, File outputFile, byte[] encryptionKey) throws Exception {
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            byte[] fileData = new byte[(int) inputFile.length()];
            fis.read(fileData);

            // Use your existing encryption method
            byte[] encryptedData = FileHelper.encrypt(encryptionKey, fileData);

            fos.write(encryptedData);
        }
    }

    private static void decryptBackupFile(File inputFile, File outputFile, byte[] encryptionKey) throws Exception {
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            byte[] encryptedData = new byte[(int) inputFile.length()];
            fis.read(encryptedData);

            // Use your existing decryption method
            byte[] decryptedData;
            try {
                decryptedData = FileHelper.decrypt(encryptionKey, encryptedData);
            } catch (InvalidKeyException e) {
                throw new Exception("Invalid encryption key for backup", e);
            }

            fos.write(decryptedData);
        }
    }

    // ==================== HELPER METHODS ====================

    private static void addDirectoryToZip(ZipOutputStream zos, String basePath, File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                addDirectoryToZip(zos, basePath + file.getName() + "/", file);
            } else {
                addToZip(zos, basePath + file.getName(), file);
            }
        }
    }

    private static void addToZip(ZipOutputStream zos, String entryName, File file) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entryName);
        zipEntry.setTime(file.lastModified());
        zos.putNextEntry(zipEntry);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }

        zos.closeEntry();
    }

    /**
     * Lists all existing backups in the specified directory
     * @param backupDir Directory containing backup files
     * @return Array of backup file information
     */
    public static File[] listBackups(File backupDir) {
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return new File[0];
        }

        return backupDir.listFiles((dir, name) ->
                name.startsWith("AnonymousMessenger_Backup_") && name.endsWith(".abk"));
    }

    /**
     * Deletes a specific backup file
     * @param backupFilePath Path to the backup file
     * @return true if deletion succeeded
     */
    public static boolean deleteBackup(String backupFilePath) {
        File backupFile = new File(backupFilePath);
        if (backupFile.exists()) {
            return backupFile.delete();
        }
        return false;
    }

    /**
     * Gets backup file information
     * @param backupFilePath Path to the backup file
     * @return Backup info or null if file doesn't exist
     */
    public static BackupInfo getBackupInfo(String backupFilePath) {
        File backupFile = new File(backupFilePath);
        if (!backupFile.exists()) {
            return null;
        }

        BackupInfo info = new BackupInfo();
        info.filePath = backupFilePath;
        info.fileSize = backupFile.length();
        info.lastModified = new Date(backupFile.lastModified());
        info.fileName = backupFile.getName();

        // Parse timestamp from filename
        try {
            String timestampStr = backupFile.getName()
                    .replace("AnonymousMessenger_Backup_", "")
                    .replace(".abk", "");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            info.timestamp = sdf.parse(timestampStr);
        } catch (Exception e) {
            info.timestamp = info.lastModified;
        }

        return info;
    }

    /**
     * Backup information container
     */
    public static class BackupInfo {
        public String filePath;
        public String fileName;
        public long fileSize;
        public Date timestamp;
        public Date lastModified;

        public String getFormattedSize() {
            if (fileSize < 1024) return fileSize + " B";
            if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }

        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            return sdf.format(timestamp);
        }
    }

    /**
     * Verifies a backup file can be decrypted with the given key
     * @param backupFilePath Path to backup file
     * @param encryptionKey Encryption key to test
     * @return true if key is valid for this backup
     */
    public static boolean verifyBackupKey(String backupFilePath, byte[] encryptionKey) {
        if (backupFilePath == null || encryptionKey == null) {
            return false;
        }

        File backupFile = new File(backupFilePath);
        if (!backupFile.exists()) {
            return false;
        }

        try (FileInputStream fis = new FileInputStream(backupFile)) {
            // Read just enough to attempt decryption (IV + minimal data)
            byte[] testData = new byte[FileHelper.IV_LENGTH + 16];
            int bytesRead = fis.read(testData);

            if (bytesRead < FileHelper.IV_LENGTH + 1) {
                return false;
            }

            // Try to decrypt a small portion
            FileHelper.decrypt(encryptionKey, testData);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}