package es.satec.rundeck.helper;

public class UserSession {

	private String username;
	private String password;
	private String hostname;
	private String nodename;
	private int port;

	public UserSession(String username, String password, String hostname, int port) {
		this.username = username;
		this.password = password;
		this.hostname = hostname;
		this.port = port;
	}

	public UserSession(String username, String password, String hostname, String nodename, int port) {
		this.username = username;
		this.password = password;
		this.hostname = hostname;
		this.port = port;
		this.nodename = nodename;

	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getNodename() {
		return nodename;
	}

	public void setNodename(String nodename) {
		this.nodename = nodename;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}