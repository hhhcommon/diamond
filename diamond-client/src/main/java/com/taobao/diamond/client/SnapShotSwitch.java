package com.taobao.diamond.client;

import com.taobao.diamond.client.impl.LocalConfigInfoProcessor;

public class SnapShotSwitch {

	private static Boolean isSnapShot = true;

	public static Boolean getIsSnapShot() {
		return isSnapShot;
	}

	public static void setIsSnapShot(Boolean isSnapShot) {
		SnapShotSwitch.isSnapShot = isSnapShot;
		LocalConfigInfoProcessor.cleanAllSnapshot();
	}
	
}
