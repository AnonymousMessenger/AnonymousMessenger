package net.sf.msopentech.thali.java.toronionproxy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class OnionProxyContext {
  public final Context ctx;
  protected final static String HIDDENSERVICE_DIRECTORY_NAME = "hiddenservice";
  protected final static String DIR = "Data/";
  protected final static String GEO_IP_NAME = "geoip.txt";
  protected final static String GEO_IPV_6_NAME = "geoip6.txt";
  protected final static String TORRC_NAME = "torrc.txt";
  protected final File workingDirectory;
  protected final File geoIpFile;
  protected final File geoIpv6File;
  protected final File torrcFile;
  protected final File torExecutableFile;
  protected final File cookieFile;
  protected final File hostnameFile;

  public OnionProxyContext(Context context, String workingSubDirectoryName) {
    this.workingDirectory = context.getDir(workingSubDirectoryName, MODE_PRIVATE);
    this.ctx = context;
    geoIpFile = new File(getWorkingDirectory(), GEO_IP_NAME);
    geoIpv6File = new File(getWorkingDirectory(), GEO_IPV_6_NAME);
    torrcFile = new File(getWorkingDirectory(), TORRC_NAME);
    torExecutableFile = new File(ctx.getApplicationInfo().nativeLibraryDir, getTorExecutableFileName());
    cookieFile = new File(getWorkingDirectory(), ".tor/control_auth_cookie");
    hostnameFile = new File(getWorkingDirectory(), "/" + HIDDENSERVICE_DIRECTORY_NAME + "/hostname");
  }

  public String getProcessId() {
    return String.valueOf(android.os.Process.myPid());
  }

  public String getPathToTorExecutable() {
    return ctx.getApplicationInfo().nativeLibraryDir;
  }

  @SuppressLint("DefaultLocale")
  public String getTorExecutableFileName() {
    String arch = Objects.requireNonNull(System.getProperty("os.arch")).toLowerCase();
    Log.d("ANONYMOUSMESSENGER",arch);

    //in case the apk has another ABI also compatible with the device
    for (File file : Objects.requireNonNull(new File(ctx.getApplicationInfo().nativeLibraryDir).listFiles())) {
//      System.out.println(file.getName());
//      Log.d("ANONYMOUSMESSENGER",file.getName());
      if (file.getName().startsWith("libtor")) {
        return file.getName();
      }
    }

    String exec = "libtor.";
    if (arch.contains("64")) {
      if (arch.contains("arm") || arch.contains("aar"))
        return exec + "arm64"+".so";
//      else if (arch.contains("mips"))
//        return exec + Arch.MIPS64.name().toLowerCase();
      else if (arch.contains("86") || arch.contains("amd"))
        return exec + "x86_64"+".so";
    } else {
      if (arch.contains("arm") || arch.contains("aar"))
        return exec + "arm"+".so";
//      else if (arch.contains("mips"))
//        return exec + Arch.MIPS.name().toLowerCase();
      else if (arch.contains("86") || arch.contains("amd"))
        return exec + "x86"+".so";
    }
    throw new RuntimeException("We don't support Tor on this OS");
  }

  public void installFiles() throws IOException, InterruptedException {
    Thread.sleep(1000, 0);

    if (!workingDirectory.exists() && !workingDirectory.mkdirs()) {
      throw new RuntimeException("Could not create root directory!");
    }

    FileUtilities.cleanInstallOneFile(this.ctx.getAssets().open(DIR+GEO_IP_NAME), geoIpFile);
    FileUtilities.cleanInstallOneFile(this.ctx.getAssets().open(DIR+GEO_IPV_6_NAME), geoIpv6File);
    FileUtilities.cleanInstallOneFile(this.ctx.getAssets().open(DIR+TORRC_NAME), torrcFile);
  }

  /**
   * Sets environment variables and working directory needed for Tor
   *
   * @param processBuilder we will call start on this to run Tor
   */
  public void setEnvironmentArgsAndWorkingDirectoryForStart(ProcessBuilder processBuilder) {
    processBuilder.directory(getWorkingDirectory());
    Map<String, String> environment = processBuilder.environment();
    environment.put("HOME", getWorkingDirectory().getAbsolutePath());
    environment.put("LD_LIBRARY_PATH", getWorkingDirectory().getAbsolutePath());
  }

  public String[] getEnvironmentArgsForExec() {
    List<String> envArgs = new ArrayList<>();
    envArgs.add("HOME=" + getWorkingDirectory().getAbsolutePath());
    envArgs.add("LD_LIBRARY_PATH=" + getWorkingDirectory().getAbsolutePath());
    return envArgs.toArray(new String[envArgs.size()]);
  }

  public File getGeoIpFile() {
    return geoIpFile;
  }

  public File getGeoIpv6File() {
    return geoIpv6File;
  }

  public File getTorrcFile() {
    return torrcFile;
  }

  public File getCookieFile() {
    return cookieFile;
  }

  public File getHostNameFile() {
    return hostnameFile;
  }

  public File getTorExecutableFile() {
    return torExecutableFile;
  }

  public File getWorkingDirectory() {
    return workingDirectory;
  }

  public void deleteAllFilesButHiddenServices() throws InterruptedException {
    // It can take a little bit for the Tor OP to detect the connection is dead and kill itself
    Thread.sleep(1000, 0);
    for (File file : Objects.requireNonNull(getWorkingDirectory().listFiles())) {
      if (file.isDirectory()) {
        if (file.getName().compareTo(HIDDENSERVICE_DIRECTORY_NAME) != 0) {
          FileUtilities.recursiveFileDelete(file);
        }
      } else {
        if (!file.delete()) {
          throw new RuntimeException("Could not delete file " + file.getAbsolutePath());
        }
      }
    }
  }
}