package com.taobao.diamond.client;


public interface DiamondClientSub {


    public void setDiamondConfigure(DiamondConfigure diamondConfigure);



    public DiamondConfigure getDiamondConfigure();


 
    public void start();


    public void close();
}
