/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package org.whispersystems.curve25519;

public class JavaCurve25519Provider extends BaseJavaCurve25519Provider {

  protected JavaCurve25519Provider() {
    super(new JCESha512Provider(), new JCESecureRandomProvider());
  }

  @Override
  public boolean isNative() {
    return false;
  }

}
