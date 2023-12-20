import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CzytanieOdczytywanie {
    private static final String pilkPath = "file.txt";
    private static Lock lock = new ReentrantLock();
    private static Condition piszacyCondition = lock.newCondition();
    private static Condition czytającyCondition = lock.newCondition();

    private static boolean czyKolejPisarza = true;
    private static boolean czyKolejCzytającego = false;
    private static String linijkaDoWpisania = null;
    private static String ostatniaLinijka = null;

    public static void main(String[] args) {
        Thread pisarzThread = new Thread(() -> {
            try {
                wpiszDoPliku();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Thread czytajacyThread = new Thread(() -> {
            try {
                odczytajZpliku();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        pisarzThread.start();
        czytajacyThread.start();
    }

    private static void wpiszDoPliku() throws IOException {
        Scanner scanner = new Scanner(System.in);
        while(true){
            lock.lock();
            try {
                while (!czyKolejPisarza) {
                    piszacyCondition.await();
                }
                System.out.print("Enter a line of text: ");
                String line = scanner.nextLine();
                linijkaDoWpisania = line;
                czyKolejPisarza = false;
                czyKolejCzytającego = true;
                czytającyCondition.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    private static void odczytajZpliku() throws IOException {
        while (true) {
            lock.lock();
            try {
                while (!czyKolejCzytającego) {
                    czytającyCondition.await();
                }

                Path filePath = Path.of(pilkPath);
                Files.writeString(filePath, linijkaDoWpisania + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                ostatniaLinijka = linijkaDoWpisania;
                linijkaDoWpisania = null;
                czyKolejCzytającego = false;
                czyKolejPisarza = true;
                piszacyCondition.signal();

                if (ostatniaLinijka != null) {
                    System.out.println("Last line: " + ostatniaLinijka);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }
}