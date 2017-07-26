package com.taobao.diamond.domain;

public class ConfigInfo extends ConfigInfoBase {
	static final long serialVersionUID = -1L;

	private String appName;

	public ConfigInfo() {

	}

	public ConfigInfo(String dataId, String group, String content) {
		super(dataId, group, content);
	}
	
	public ConfigInfo(String dataId, String group, String appName, String content) {
		super(dataId, group, content);
		this.appName = appName;
	}
	
	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	@Override
	public String toString() {
		return "ConfigInfo{" + "id=" + getId() + ", dataId='" + getDataId()
				+ '\'' + ", group='" + getGroup() + '\'' + ", appName='"
				+ appName + '\'' + ", content='" + getContent() + '\''
				+ ", md5='" + getMd5() + '\'' + '}';
	}
}
