import niochat.msghandling.MessageHandler;
import org.junit.Test;

import java.nio.ByteBuffer;



public class MessageHandlingTest {


    @Test
    public void testRegistration() {
        MessageHandler handler = new MessageHandler();
    }

    @Test
    public void testParsing() {
        MessageHandler handler = new MessageHandler();

        byte[] b = "hello, world".getBytes();
        ByteBuffer buf = ByteBuffer.allocate(1024 * 1024);
        byte[] bf = new byte[1024*1024];
        buf.put(bf);
        buf.flip();
        byte[] bytd = Integer.toString(b.length).getBytes();

        //test 1
        buf.put("<START_1".getBytes());
        //buf.put(bytd[0]);
        buf.flip();
        handler.parsing(buf);

        buf.clear();

        //buf.put(bytd[1]);
        buf.put("2>hello, world<END>".getBytes());
        buf.flip();
        handler.parsing(buf);
        buf.clear();
        /*handler.completedMessages.forEach(e -> System.out.println(new String(e.value)));
        handler.completedMessages.remove(0);*/

        //test 2
        buf.put("<START_".getBytes());
        buf.flip();
        handler.parsing(buf);
        buf.clear();
        buf.put(bytd[0]);
        buf.put(bytd[1]);
        buf.put(">hello, world<END>".getBytes());
        buf.flip();
        handler.parsing(buf);

        buf.clear();
        /*handler.completedMessages.forEach(e -> System.out.println(new String(e.value)));
        handler.completedMessages.remove(0);*/

        //test 3
        buf.put("<".getBytes());
        buf.put("START_".getBytes());
        buf.put(bytd[0]);
        buf.put(bytd[1]);
        buf.flip();
        handler.parsing(buf);
        buf.clear();
        buf.put(">hello, world<END>".getBytes());
        buf.flip();
        handler.parsing(buf);

        buf.clear();
        /*handler.completedMessages.forEach(e -> System.out.println(new String(e.value)));
        handler.completedMessages.remove(0);*/


        //test 4
        buf.put("<START".getBytes());
        buf.put("_".getBytes());
        buf.put(bytd[0]);
        buf.put(bytd[1]);
        buf.flip();
        handler.parsing(buf);
        buf.clear();
        buf.put(">hello, world<END>".getBytes());
        buf.flip();
        handler.parsing(buf);

        buf.clear();
        /*handler.completedMessages.forEach(e -> System.out.println(new String(e.value)));
        handler.completedMessages.remove(0);*/

        //test 5
        buf.put("<START".getBytes());
        buf.put("_".getBytes());
        buf.put(bytd[0]);
        buf.put(bytd[1]);
        buf.put(">hello, ".getBytes());
        buf.flip();
        handler.parsing(buf);
        buf.clear();
        buf.put("world<END>".getBytes());
        buf.flip();
        handler.parsing(buf);

        buf.clear();
        /*handler.completedMessages.forEach(e -> System.out.println(new String(e.value)));
        handler.completedMessages.remove(0);*/


        //test 6
        buf.put("<START".getBytes());
        buf.put("_".getBytes());
        buf.put(bytd[0]);
        buf.put(bytd[1]);
        buf.put(">hello, ".getBytes());
        buf.flip();
        handler.parsing(buf);
        buf.clear();
        buf.put("wo".getBytes());
        buf.flip();
        handler.parsing(buf);

        buf.clear();
        buf.put("rld<END>".getBytes());
        buf.flip();
        handler.parsing(buf);


        buf.clear();
        /*handler.completedMessages.forEach(e -> System.out.println(new String(e.value)));
        handler.completedMessages.remove(0);*/



        //test 7
        buf.put("jhjkhllkjhkljhkjlhlkj<START".getBytes());
        buf.put("_".getBytes());
        buf.put(bytd[0]);
        buf.put(bytd[1]);
        buf.put(">hello, ".getBytes());
        buf.put("wo".getBytes());
        buf.put("rld<E".getBytes());
        buf.flip();
        handler.parsing(buf);
        buf.clear();
        buf.put("ND>".getBytes());
        buf.flip();
        handler.parsing(buf);

        buf.clear();
        /*handler.completedMessages.forEach(e -> System.out.println(new String(e.value)));
        handler.completedMessages.remove(0);*/
    }
}
