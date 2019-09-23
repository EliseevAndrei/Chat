import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class SmallClientSocket {
    private static volatile boolean isClosed = false;
    private static volatile  boolean waiting = true;
    public static void main(String[] args) {
        ExecutorService service = Executors.newCachedThreadPool();
        try (
                Socket socket = new Socket(InetAddress.getLocalHost(), 9999);//8030
                PrintStream ps = new PrintStream(socket.getOutputStream());
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))
        ) {
            Scanner scan = new Scanner(System.in);
            service.execute(new Thread(() -> {
                String str = "";
                while (!isClosed) {
                    try {
                        while ((str = br.readLine()) != null) {
                            waiting = false;
                            if (Pattern.matches(".*: /leave", str) || Pattern.matches(".*: /exit", str)) {
                                System.out.println("finished conversation");
                                waiting = true;
                                ps.println("<START_9>/finished<END>");
                            } else if (Pattern.matches(".*: /eof", str)) {
                                isClosed = true;
                                service.shutdownNow();
                                ps.close();
                                scan.close();
                                return;
                            } else System.out.println(str);
                        }
                    } catch (SocketException e) {
                        isClosed = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }));
            String str;
            while (!isClosed) {
                str = scan.nextLine();
                if (str.equals("/exit"))
                    isClosed = true;
                if (str.equals("/leave")){
                    waiting = true;
                }
                str = "<START_" + str.length() + ">" + str + "<END>";
                ps.println(str);
                if (isClosed && waiting) {
                    service.shutdownNow();
                    ps.close();
                    socket.close();
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }   catch (IOException e) {
            e.printStackTrace();
        }
    }




}
