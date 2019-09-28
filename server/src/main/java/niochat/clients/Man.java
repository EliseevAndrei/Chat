package niochat.clients;

import niochat.msghandling.MessageHandler;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class Man {
    //Don't use modifier "public" for class fields if they are not final, use getters and setters instead
    public byte[] name;
    public SocketChannel companion;
    public SocketChannel myChannel; //Never used, delete
    public MessageHandler handler;
    public Man() { } //Never used, delete
    public Man(byte[] name){
        this.name = name;
    }
    public void setCompanion(SocketChannel companion){ //Never used, delete
        this.companion = companion;
    }
    // Don't think tou really need Agent and Client classes.
    // You can just add "final boolean isAgent" and you it in exactly the same way

    public boolean write(Selector selector) {
        return handler.handleWrite(companion.keyFor(selector), name);
    }


}
