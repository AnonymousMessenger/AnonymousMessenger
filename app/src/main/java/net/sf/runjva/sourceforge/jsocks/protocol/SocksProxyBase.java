package net.sf.runjva.sourceforge.jsocks.protocol;

/**
 * Abstract class Proxy, base for classes Socks4Proxy and Socks5Proxy. Defines
 * methods for specifying default proxy, to be used by all classes of this
 * package.
 */

public abstract class SocksProxyBase {

	// Constructors
	// ====================
	SocksProxyBase() {
	}

	// Private methods
	// ===============

	// Constants

	public static final int SOCKS_SUCCESS = 0;
	public static final int SOCKS_FAILURE = 1;
	public static final int SOCKS_BADCONNECT = 2;
	public static final int SOCKS_BADNETWORK = 3;
	public static final int SOCKS_HOST_UNREACHABLE = 4;
	public static final int SOCKS_CONNECTION_REFUSED = 5;
	public static final int SOCKS_TTL_EXPIRE = 6;
	public static final int SOCKS_CMD_NOT_SUPPORTED = 7;
	public static final int SOCKS_ADDR_NOT_SUPPORTED = 8;

	public static final int SOCKS_NO_PROXY = 1 << 16;
	public static final int SOCKS_PROXY_NO_CONNECT = 2 << 16;
	public static final int SOCKS_PROXY_IO_ERROR = 3 << 16;
	public static final int SOCKS_AUTH_NOT_SUPPORTED = 4 << 16;
	public static final int SOCKS_AUTH_FAILURE = 5 << 16;
	public static final int SOCKS_JUST_ERROR = 6 << 16;

	public static final int SOCKS_DIRECT_FAILED = 7 << 16;
	public static final int SOCKS_METHOD_NOTSUPPORTED = 8 << 16;

	static final int SOCKS_CMD_CONNECT = 0x1;
	static final int SOCKS_CMD_BIND = 0x2;
	static final int SOCKS_CMD_UDP_ASSOCIATE = 0x3;

}
