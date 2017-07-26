package com.taobao.diamond.domain;

/**
 * 
 * @author water.lyl
 *
 */
public class ConfigInfoBaseEx extends ConfigInfoBase {

	private static final long serialVersionUID = -1L;
	private int status;
	private String message;

	public ConfigInfoBaseEx() {
		super();
	}

	public ConfigInfoBaseEx(String dataId, String group, String content) {
		super(dataId, group, content);
	}

	public ConfigInfoBaseEx(String dataId, String group, String content,
			int status, String message) {
		super(dataId, group, content);
		this.status = status;
		this.message = message;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "ConfigInfoBaseEx [status=" + status + ", message=" + message
				+ ", dataId=" + getDataId() + ", group()=" + getGroup()
				+ ", content()=" + getContent() + "]";
	}

}