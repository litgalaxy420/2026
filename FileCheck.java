import java.io.File;
import java.util.Scanner;

public class FileCheck {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the file path to check: ");
        String filePath = scanner.nextLine();

        File file = new File(filePath);

        // Check if file exists, is a file, and is readable
        if (file.exists() && file.isFile() && file.canRead()) {
            System.out.println("File is safe to read: " + file.getAbsolutePath());
            // Proceed with file operations
        } else {
            System.out.println("File is not safe, does not exist, or is a directory.");
        }
        
        scanner.close();
    }
}
