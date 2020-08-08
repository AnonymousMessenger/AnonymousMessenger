
package net.sf.msopentech.thali.java.toronionproxy.android;

import android.annotation.SuppressLint;
import android.content.Context;

import net.sf.msopentech.thali.java.toronionproxy.FileUtilities;
import net.sf.msopentech.thali.java.toronionproxy.OnionProxyContext;
import net.sf.msopentech.thali.java.toronionproxy.WriteObserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static android.content.Context.MODE_PRIVATE;

public class AndroidOnionProxyContext extends OnionProxyContext {
  private final Context context;

  private enum Arch {
    ARM64,
    MIPS64,
    AMD64,
    ARM,
    MIPS,
    X86
  };

  public AndroidOnionProxyContext(Context context, String workingSubDirectoryName) {
    super(context,context.getDir(workingSubDirectoryName, MODE_PRIVATE));
    this.context = context;
  }

  @Override
  public WriteObserver generateWriteObserver(File file) {
    return new AndroidWriteObserver(file);
  }

  @Override
  protected InputStream getAssetOrResourceByName(String fileName) throws IOException {
    try {
      return context.getResources().getAssets().open(fileName);
    } catch (IOException e) {
      return getClass().getResourceAsStream("/" + fileName);
    }
  }

  @Override
  public String getProcessId() {
    return String.valueOf(android.os.Process.myPid());
  }

  @Override
  public void installFiles() throws IOException, InterruptedException {
    super.installFiles();
//    FileUtilities.cleanInstallOneFile(getAssetOrResourceByName(getPathToTorExecutable() + getTorExecutableFileName()),
//            torExecutableFile);
  }

  @Override
  public String getPathToTorExecutable() {
    return "";

  }

  @SuppressLint("DefaultLocale")
  @Override
  protected String getTorExecutableFileName() {
    String arch = System.getProperty("os.arch").toLowerCase();
    System.out.println(arch);
    String exec = "tor.";
    if (arch.contains("64")) {
      if (arch.contains("arm") || arch.contains("aar"))
        return exec + Arch.ARM64.name().toLowerCase();
      else if (arch.contains("mips"))
        return exec + Arch.MIPS64.name().toLowerCase();
      else if (arch.contains("86") || arch.contains("amd"))
        return exec + Arch.AMD64.name().toLowerCase();
    } else {
      if (arch.contains("arm") || arch.contains("aar"))
        return exec + Arch.ARM.name().toLowerCase();
      else if (arch.contains("mips"))
        return exec + Arch.MIPS.name().toLowerCase();
      else if (arch.contains("86"))
        return exec + Arch.X86.name().toLowerCase();
    }
    throw new RuntimeException("We don't support Tor on this OS");
  }
}