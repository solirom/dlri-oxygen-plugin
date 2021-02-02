package ro.dlri.oxygen.plugin;

public class Username {
	private String userid;
	private String username;

	public Username(String userCredentials) {
		this.userid = userCredentials.substring(0, userCredentials.indexOf("\t"));
		this.username = userCredentials.substring(userCredentials.indexOf("\t") + 1);
	}

	public String toString() {
		return getUsername();
	}

	public String getUsername() {
		return username;
	}

	public String getUserid() {
		return userid;
	}
}
