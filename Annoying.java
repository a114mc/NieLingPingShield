public class Annoying {
    public static void main(String[] args) {
        while (true) {
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {

            }
            try {
                ProcessBuilder pb = new ProcessBuilder("./solver.py", args[0]);
                pb.inheritIO();
                Process pro = pb.start();
                pro.waitFor();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
