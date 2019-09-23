It's a console chat.

Without web. Two module for client and server. 
Client using IO. Server using NIO.

On the server side:
One thread accept tcp-connections. Then other thread read and write in these all connections. Other one set pair client-agent for
private conversation.
Message format: <START_"payloadLength">"payload"<END>
