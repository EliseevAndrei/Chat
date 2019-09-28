package niochat.msghandling;

import niochat.NioServerChat;
import niochat.clients.Agent;
import niochat.clients.Client;
import niochat.clients.Man;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class MessageHandler {//Class never used, delete it

    private static final byte[] startMessage = new byte[] {'<','S','T','A','R','T','_'}; //Use "<START_".getBytes() instead
    private static final byte[] endMessage = new byte[] {'<','E','N','D','>'};
    private static final byte[] REGISTRATION = new byte[] {'/','r','e','g','i','s','t','e','r' ,' '};
    private static final byte[] FINISHED = new byte[] {'/','f','i','n','i','s','h','e','d'};
    private static final byte[] LEAVE = new byte[] {'/','l','e','a','v','e'};
    private static final byte[] EXIT = new byte[] {'/','e','x','i','t'};
    private static final byte[] AGENT = new byte[] {'a','g','e','n','t',' '};
    private static final byte[] CLIENT = new byte[] {'c','l','i','e','n','t',' '};


    private List<Message> completedMessages = new ArrayList<>();
    private Message notCompletedMsg = null;
    private int metaDataLength = 0;
    private int endMeta = 0;
    private byte[] metaData = null;
    private int limit = 0;
    private int sendingMessages = 0;

    // This class is definitely a God object, it's doing too much different things
    // You should split it on 3 (maybe even more) separate classes:
    // 1) Low level channel processing. This class should read bytes from chanel and return strings; make byte arrays from strings and write them to channel
    // 2) Message processing. This class should found in messages words like /leave, /exit, etc. What "isCommand" doing now
    // 3) Other things: method "findRegistration", witch doing high level logic. It should be in another class, for example factory one


    public String isCommand() {
        int i = 0;
        String str = null;
        for (; i < completedMessages.size(); i++) {
            if (findWord(completedMessages.get(i).value, FINISHED, 0)) {
                str = "/finished";
                break;
            }  else if (findWord(completedMessages.get(i).value, LEAVE, 0)) {
                str = "/leave";
                break;
            } else if (findWord(completedMessages.get(i).value, EXIT, 0)) {
                str = "/exit";
                break;
            }
        }
        if (str != null) {
            sendingMessages = i + 1;
            if (str.equals("/finished")) {
                sendingMessages--;
                completedMessages.remove(i);
            }
            return str;
        }
        return null;
    }

    public boolean handleWrite(SelectionKey key, byte[] name) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            int barrier;
            if (sendingMessages != 0) {
                barrier = sendingMessages;
            } else barrier = completedMessages.size();

            for (int i = 0; i < barrier; barrier--) {
                ByteBuffer buf1 = ByteBuffer.wrap(completedMessages.get(i).value);
                channel.write(ByteBuffer.wrap(name));
                channel.write(ByteBuffer.wrap(": ".getBytes()));
                channel.write(buf1);
                buf1.clear();
                buf1.put((byte)'\n');
                buf1.flip();
                channel.write(buf1);
                System.out.println("written" + new String(completedMessages.get(i).value));
                completedMessages.remove(i);
            }
            sendingMessages = 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public Man findRegistration() {
        Man man = null;
        byte[] name;
        int i = 0;
        for (; i < completedMessages.size(); i++) {
            if(findWord(completedMessages.get(i).value, REGISTRATION, 0)) {
                if (findWord(completedMessages.get(i).value, CLIENT, REGISTRATION.length)) {
                    if ((name = findName(completedMessages.get(i).value, REGISTRATION.length + CLIENT.length)) != null){
                        man = new Client(name);
                        break;
                    }
                } else if (findWord(completedMessages.get(i).value, AGENT, REGISTRATION.length)) {
                    if ((name = findName(completedMessages.get(i).value, REGISTRATION.length + AGENT.length)) != null){
                        man = new Agent(name);
                        break;
                    }
                }
            }
        }
        if (man != null) {
            for (; i >= 0; i--) {
                completedMessages.remove(i);
            }
            return man;
        }
        return null;
    }

    private byte[] findName(byte[] message, int offset) {
        int i = offset;
        while (i < message.length && message[i] != ' ') {
            i++;
        }
        if (i == offset) {
            return null;
        }
        byte[] name = new byte[i - offset];
        System.arraycopy(message, offset, name, 0, name.length);
        return name;
    }

    private boolean findWord(byte[] message, byte[] word, int offset) {
        int j = offset, i = 0;
        for (; i < word.length && j < message.length; j++){
            if (message[j] != word[i]){
                return false;
            }
            i++;
        }
        if (i >= word.length && j >= message.length){
            return true;
        }
        return true;
    }



    public boolean handleRead(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        return this.read(channel);
    }

    private boolean read(SocketChannel channel) {
        ByteBuffer buf = NioServerChat.buf;
        buf.clear();
        int byteAmount;
        try {
            while ((byteAmount = channel.read(buf)) > 0) {
                parsing(buf);
                buf.clear();
            }
            if (byteAmount == -1) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
            return true;
    }


    //<start_"length">.....<end>
    public void parsing(ByteBuffer buf) {
        byte[] value;
        byte[] valueLengthBytes = new byte[buf.remaining()];
        buf.flip();

        //Put big branches into separate methods
        while (buf.hasRemaining()) {
            if (notCompletedMsg == null) {

                int startIndex = 0;
                if (limit == 0) {
                    startIndex = metaDataLength;


                    //find start of message
                    int i;
                    for (i = startIndex; i < startMessage.length && buf.hasRemaining();) {
                        if (startMessage[i] == buf.get()) {
                            i++;
                            while (buf.hasRemaining() && i < startMessage.length && startMessage[i] == buf.get()) {
                                i++;
                            }
                            if (i < startMessage.length && buf.hasRemaining()) {
                                buf.position(buf.position() + i + 1);
                                i = 0;
                            }
                            if (i <= startMessage.length && !buf.hasRemaining()) {
                                metaDataLength = i;
                                return;
                            }
                        }
                    }
                    if (!buf.hasRemaining() && i == 0) {
                        return;
                    }
                }

                //find value length
                int offset = -1;
                if (limit == 0) {
                    metaData = new byte[256];
                }

                 if (buf.position() != 0) {
                    offset = buf.position() - 1;
                }

                do {
                    if (!buf.hasRemaining()) {
                        buf.position(offset + 1);
                        byte[] tmpMas = new byte[buf.remaining()];
                        int curPos = buf.remaining();
                        buf.get(tmpMas);
                        System.arraycopy(tmpMas, 0, metaData, limit, curPos);
                        limit += curPos;
                        metaDataLength += curPos;
                        return;
                    }
                } while (buf.get() != '>');

                int end = 0;
                if (buf.position() != 1) {
                    end = buf.position() - 2;
                    byte[] tmpMas = new byte[end - offset];
                    buf.position(offset + 1);
                    buf.get(tmpMas, 0, end - offset);
                    System.arraycopy(tmpMas, 0, metaData, limit, end - offset);
                    limit += end - offset;
                    buf.get();
                }
                int valueLength = Integer.parseInt(new String(metaData, 0,limit));
                limit = 0;
                metaData = null;
                metaDataLength = 0;




                //read value
                value = new byte[valueLength];
                if (buf.remaining() < valueLength + endMessage.length) {
                    int length;
                    int remain = 0;
                    if (buf.remaining() >= valueLength) {
                        length = valueLength;
                        remain = 0;
                    } else {
                       length = buf.remaining();
                       remain = valueLength - length;
                    }
                    int curPos = buf.remaining();
                    buf.get(value, 0, length);
                    notCompletedMsg = new Message(value, length, remain);
                } else {
                    buf.get(value, 0, valueLength);
                }
                if (!buf.hasRemaining()) {
                    return;
                }
                //find end of message
                for (int j = 0; j < endMessage.length && buf.hasRemaining(); j++) {
                    if (endMessage[j] != buf.get()) {
                        notCompletedMsg = null;
                        return;
                    }
                    endMeta++;
                }
                endMeta = 0;
                completedMessages.add(new Message(value, 0, 0));

            } else {
                if (notCompletedMsg.remaining == 0) {
                    //find end of message
                    for (int i = endMeta; i < endMessage.length && buf.hasRemaining(); i++) {
                        if (endMessage[i] != buf.get()) {
                            notCompletedMsg = null;
                            return;
                        }
                    }
                    endMeta = 0;
                    completedMessages.add(notCompletedMsg);
                    notCompletedMsg = null;
                } else {
                    if (buf.remaining() >= notCompletedMsg.remaining + endMessage.length) {
                        byte[] tmpMas = new byte[notCompletedMsg.remaining];
                        buf.get(tmpMas, 0, notCompletedMsg.remaining);
                        int curPos = buf.remaining();
                        System.arraycopy(tmpMas, 0, notCompletedMsg.value, notCompletedMsg.curPosition, tmpMas.length);

                        for (int i = 0; i < endMessage.length && buf.hasRemaining(); i++) {
                            if (endMessage[i] != buf.get()) {
                                notCompletedMsg = null;
                                return;
                            }
                        }
                        endMeta = 0;
                        completedMessages.add(notCompletedMsg);
                        notCompletedMsg = null;
                        limit = 0;
                        metaDataLength = 0;
                        metaData = null;
                        return;
                    } else {
                        int length;
                        int remain;
                        if (buf.remaining() >= notCompletedMsg.remaining) {
                            length = notCompletedMsg.remaining;
                            remain = 0;
                        } else {
                            length = buf.remaining();
                            remain = notCompletedMsg.remaining - length;
                        }
                        byte[] tmpMas = new byte[length];
                        buf.get(tmpMas, 0, length);
                        System.arraycopy(tmpMas, 0, notCompletedMsg.value, notCompletedMsg.curPosition, tmpMas.length);
                        notCompletedMsg.remaining = remain;
                        notCompletedMsg.curPosition += tmpMas.length;
                    }
                }
            }
        }
    }

}
