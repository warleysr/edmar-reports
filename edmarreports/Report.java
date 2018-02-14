package edmarreports;

import java.util.Date;

public class Report {
	
	private int id = -1;
	private String sender;
	private String reported;
	private String reason;
	private Date date;
	
	public Report(String sender, String reported, String reason, Date date) {
		this.sender = sender;
		this.reported = reported;
		this.reason = reason;
		this.date = date;
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public String getSender() {
		return sender;
	}

	public String getReported() {
		return reported;
	}

	public String getReason() {
		return reason;
	}

	public Date getDate() {
		return date;
	}

}
