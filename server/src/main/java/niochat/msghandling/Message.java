package niochat.msghandling;

public class Message {
    public byte[] value = null;
    int curPosition = 0;
    int remaining = 0;
    public byte[] metaData = null;
    public Message() {  }

    public Message(byte[] value, int curPosition, int remaining){
        this.value = value;
        this.curPosition = curPosition;
        this.remaining = remaining;
    }
    public Message(byte[] value, int curPosition, int remaining, byte[] metaData){
        this.value = value;
        this.curPosition = curPosition;
        this.remaining = remaining;
        this.metaData = metaData;
    }


}
