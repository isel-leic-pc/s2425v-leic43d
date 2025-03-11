public class HelloThreads {

    public static void printNumbers() {
        while (true) {
            for (int i = 0; i <= 9999; i++) {
                System.out.printf("%04d%n", i);
                try {
                    Thread.sleep(10); // Slow down output
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static void printLetters() {
        while (true) {
            for (char c = 'A'; c <= 'Z'; c++) {
                System.out.printf("%c%c%c%c%n", c, c, c, c);
                try {
                    Thread.sleep(10); // Slow down output
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static void printAlternating() {
        while (true) {
            System.out.println("----");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            System.out.println("****");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public static void main(String[] args) {
        Thread t1 = new Thread(HelloThreads::printNumbers);
        Thread t2 = new Thread(HelloThreads::printLetters);
        Thread t3 = new Thread(HelloThreads::printAlternating);

        t1.start();
        t2.start();
        t3.start();
    }
}
