package ro.dlri.oxygen.plugin;

public class Username {
	private String userid;
	private String username;
	private String userEmail;

	public Username(String userCredentials) {
		String[] parts = userCredentials.split("\t");		
		this.userid = parts[0];
		this.username = parts[1];
		this.userEmail = parts[2];
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
	public String getUserEmail() {
		return userEmail;
	}	
}
