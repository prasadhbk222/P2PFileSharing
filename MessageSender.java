import java.io.OutputStream;


public class MessageSender implements Runnable {
    private final OutputStream outputStream;
    private final byte[] message;

    public MessageSender(OutputStream outputStream, byte[] message) {
        this.outputStream = outputStream;
        this.message = message;
    }

    @Override
    public void run() {
        try {
            if (Thread.currentThread().isInterrupted())
                return;
            synchronized(outputStream) {
                outputStream.write(this.message);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
