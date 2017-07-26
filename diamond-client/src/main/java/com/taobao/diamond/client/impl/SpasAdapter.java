package com.taobao.diamond.client.impl;

import java.util.ArrayList;
import java.util.List;

import com.taobao.spas.sdk.client.SpasSdkClientFacade;
import com.taobao.spas.sdk.common.sign.SpasSigner;
public class SpasAdapter {

	public static List<String> getSignHeaders(String group) {
		List<String> header = new ArrayList<String>();
		String timeStamp = String.valueOf(System.currentTimeMillis());
		header.add("timeStamp");
		header.add(timeStamp);
		String secretKey = SpasSdkClientFacade.getCredential().getSecretKey();
		if (secretKey != null) {
			header.add("Spas-Signature");
			String signature = SpasSigner.sign(group + "+" + timeStamp,
					secretKey);
			header.add(signature);
		}
		return header;
	}
	
	public static String getAk() {
		return SpasSdkClientFacade.getCredential().getAccessKey();
	}
}
