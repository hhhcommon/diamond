package com.taobao.diamond.client.impl;

import com.taobao.diamond.client.DiamondPublisher;
import com.taobao.diamond.client.DiamondSubscriber;


@Deprecated
public class DiamondClientFactory {

    public synchronized static DiamondSubscriber getSingletonDiamondSubscriber() {
        return DefaultDiamondSubscriber.singleton;
    }

    public synchronized static DiamondSubscriber getSingletonBasestoneSubscriber() {
        return DefaultDiamondSubscriber.singleton;
    }

    public synchronized static DiamondPublisher getSingletonBasestonePublisher() {
        return DefaultDiamondPublisher.singleton;
    }
}
