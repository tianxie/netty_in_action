package chapter1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

/**
 * EchoServer based on NIO.2
 */
public class PlainNio2EchoServer {
    public void serve(int port) throws IOException {
        System.out.println("Listening for connections on port " + port);
        final AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open();
        final InetSocketAddress address = new InetSocketAddress(port);
        serverChannel.bind(address);
        final CountDownLatch latch = new CountDownLatch(1);
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            public void completed(AsynchronousSocketChannel channel, Object attachment) {
                serverChannel.accept(null, this);
                final ByteBuffer buffer = ByteBuffer.allocate(100);
                channel.read(buffer, buffer, new EchoCompleteHandler(channel));
            }

            public void failed(Throwable throwable, Object attachment) {
                try {
                    serverChannel.close();
                } catch (IOException e) {
                    // ignore on close
                } finally {
                    latch.countDown();
                }
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    private final class EchoCompleteHandler implements CompletionHandler<Integer, ByteBuffer> {
        private AsynchronousSocketChannel channel;

        public EchoCompleteHandler(AsynchronousSocketChannel channel) {
            this.channel = channel;
        }

        public void completed(Integer result, ByteBuffer buffer) {
            buffer.flip();
            channel.write(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                public void completed(Integer result, ByteBuffer buffer) {
                    if (buffer.hasRemaining()) {
                        channel.write(buffer,buffer, this);
                    } else {
                        buffer.compact();
                        channel.read(buffer, buffer, EchoCompleteHandler.this);
                    }
                }

                public void failed(Throwable exc, ByteBuffer attachment) {
                    try {
                        channel.close();
                    } catch (IOException e) {
                        // ignore on close
                    }
                }
            });
        }

        public void failed(Throwable exc, ByteBuffer attachment) {
            try {
                channel.close();
            } catch (IOException e) {
                // ignore on close
            }
        }
    }
}
