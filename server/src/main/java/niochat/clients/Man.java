package niochat.clients;

import niochat.msghandling.MessageHandler;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class Man {
    public byte[] name;
    public SocketChannel companion;
    public SocketChannel myChannel;
    public MessageHandler handler;
    public Man() { }
    public Man(byte[] name){
        this.name = name;
    }
    public void setCompanion(SocketChannel companion){
        this.companion = companion;
    }

    public boolean write(Selector selector) {
        return handler.handleWrite(companion.keyFor(selector), name);
    }


}
