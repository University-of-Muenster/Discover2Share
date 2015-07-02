package de.wwu.d2s.jpa;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class User implements java.io.Serializable {
	private static final long serialVersionUID = -7399509240684000311L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	private String username;
	private String password;
	private String authToken;
	private String authRole;

	public User() {
	}
	
	public void hashOwnPassword(){
		password = hashPassword(password);
	}

	public String hashPassword(String password) {
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
			messageDigest.update(password.getBytes());
			return new String(messageDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			return "";
		}
	}
	
	public boolean comparePassword(String pw) {
		String hashed = hashPassword(pw);
		if(password.equals(hashed))
			return true;
		return false;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	public String getAuthRole() {
		return authRole;
	}

	public void setAuthRole(String authRole) {
		this.authRole = authRole;
	}
}
