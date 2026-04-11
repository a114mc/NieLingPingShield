public class Annoying {
    public static void main(String[] args) {
        System.out.println("Victim: " + args[0]);
        while (true) {
            try {
                ProcessBuilder pb = new ProcessBuilder("./solver.py", args[0]);
                pb.inheritIO();
                Process pro = pb.start();
                pro.waitFor();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                Thread.sleep(15 * 1000L);
            } catch (InterruptedException e) {

            }
        }
    }
}
