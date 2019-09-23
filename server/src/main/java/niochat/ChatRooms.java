package niochat;

import niochat.clients.Agent;
import niochat.clients.Client;
import niochat.clients.Man;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatRooms extends Thread {
    private Selector selector2;
    private BlockingQueue<Agent> agents = new LinkedBlockingQueue<>();
    private BlockingQueue<Client> clients = new LinkedBlockingQueue<>();
    private Queue<Man> manForRegistration = new ArrayDeque<>();

    public ChatRooms() throws IOException {
        new Thread(() -> {
            while (true) {
                try {
                    Agent agent;
                    Client client;
                    agent = agents.take();
                    client = clients.take();
                    agent.companion = client.myChannel;
                    client.companion = agent.myChannel;
                    manForRegistration.add(agent);
                    manForRegistration.add(client);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        this.selector2 = Selector.open();
    }

    @Override
    public void run() {
        Iterator<SelectionKey> iter;
        SelectionKey key;
        while (true) {
            try {
                register();
                selector2.selectNow();
                if (selector2.selectedKeys().size() != 0) {
                    iter = selector2.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        key = iter.next();
                        iter.remove();
                        if (key.isValid())
                            readAndWrite(key);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void register() {
        Man man;
        while ((man = manForRegistration.poll()) != null) {
            man.myChannel.keyFor(selector2).interestOps(SelectionKey.OP_READ);
        }
    }

    public void addToQueue(Man obj) {
        try {
            if (obj instanceof Client) {
                ((Client) obj).myChannel.register(selector2, 0, obj);
                this.clients.add((Client) obj);
            } else {
                obj.myChannel.register(selector2, 0, obj);
                this.agents.add((Agent) obj);
            }
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
    }

    public void readAndWrite(SelectionKey key) {
        Man man = (Man) key.attachment();
        try {
            if (man.companion != null && man.handler.handleRead(key)) {
                String str;
                if ((str = man.handler.isCommand()) != null) {
                    System.out.println(str);
                    switch (str) {
                        case "/leave":
                            if (!man.write(selector2)) {
                                closeRoom(man.companion.keyFor(selector2),key , man);
                            }
                        case "/finished":
                            man.companion = null;
                            key.interestOps(0);
                            if (man instanceof Client) {
                                clients.add((Client) man);
                            } else agents.add((Agent) man);
                            break;
                        case "/exit":
                            if (!man.write(selector2)) {
                                closeRoom(man.companion.keyFor(selector2),key , man);
                            }
                            man.companion = null;
                            man.myChannel.write(ByteBuffer.wrap("/eof\n".getBytes()));
                            key.channel().close();
                            key.cancel();
                            break;
                    }
                } else if (!man.write(selector2)) {
                    closeRoom(man.companion.keyFor(selector2),key , man);
                }
            } else {
                man.companion.write(ByteBuffer.wrap("/exit".getBytes()));
                key.channel().close();
                key.cancel();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeRoom(SelectionKey keyForClose,    SelectionKey key, Man man) {
        try {
            keyForClose.channel().close();
            keyForClose.cancel();
            man.companion = null;
            key.interestOps(0);
            if (man instanceof Client) {
                clients.add((Client) man);
            } else agents.add((Agent) man);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
