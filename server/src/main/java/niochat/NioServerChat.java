package niochat;


import niochat.clients.Agent;
import niochat.clients.Client;
import niochat.clients.Man;
import niochat.msghandling.MessageHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class NioServerChat implements Runnable {
    private ServerSocketChannel ssc;
    private Selector selector;
    private static ChatRooms chatRooms;
    public static ByteBuffer buf = ByteBuffer.allocate(1024*1024);

    public static void main(String[] args) throws IOException {
        new Thread(new NioServerChat()).start();
        chatRooms = new ChatRooms();
        chatRooms.start();
    }

    public NioServerChat() throws IOException {
        this.ssc = ServerSocketChannel.open();
        this.ssc.socket().bind(new InetSocketAddress(9999));
        this.ssc.configureBlocking(false);
        this.selector = Selector.open();
        this.ssc.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        Iterator<SelectionKey> iter;
        SelectionKey key;
        while(this.ssc.isOpen()) {
            try {
                this.selector.select();
                iter = this.selector.selectedKeys().iterator();
                while(iter.hasNext()) {
                    key = iter.next();
                    iter.remove();
                    if (key.isAcceptable()) {
                        this.handleAccept(key);
                    }
                    if (key.isReadable()) {
                        if (key.attachment() == null)
                            this.handleRegistration(key);
                        else {
                            Object obj = key.attachment();
                            chatRooms.addToQueue((Man) obj);
                            key.cancel();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRegistration(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        MessageHandler handler = new MessageHandler();
        try {
            if (handler.handleRead(key)) {
                Man man = handler.findRegistration();
                if (man != null) {
                    if (man instanceof Agent) {
                        man.myChannel = (SocketChannel) key.channel();
                        man.handler = handler;
                        key.cancel();
                        chatRooms.addToQueue(man);
                    } else if (man instanceof Client) {
                        man.handler = handler;
                        man.myChannel = (SocketChannel) key.channel();
                        key.attach(man);
                    }
                } else {
                    channel.write(ByteBuffer.wrap("You must register!\n".getBytes()));
                }
            } else {
                key.channel().close();
                key.cancel();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAccept(SelectionKey key) {
        try {
            SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

