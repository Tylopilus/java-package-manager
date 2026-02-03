package jpm.cli;

import jpm.utils.FileUtils;
import picocli.CommandLine.Command;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

@Command(name = "clean", description = "Clean build artifacts")
public class CleanCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        try {
            File targetDir = new File("target");
            
            if (targetDir.exists()) {
                System.out.println("Cleaning target/ directory...");
                FileUtils.deleteDirectory(targetDir);
                System.out.println("Clean complete");
            } else {
                System.out.println("Nothing to clean (no target/ directory found)");
            }
            
            return 0;
            
        } catch (IOException e) {
            System.err.println("Error cleaning: " + e.getMessage());
            return 1;
        }
    }
}
