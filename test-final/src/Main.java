import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Using Guava's Lists
        List<String> names = Lists.newArrayList("Alice", "Bob", "Charlie");
        System.out.println("Names: " + names);
        
        // Using Guava's ImmutableMap
        Map<String, Integer> ages = ImmutableMap.of(
            "Alice", 30,
            "Bob", 25,
            "Charlie", 35
        );
        System.out.println("Ages: " + ages);
        
        System.out.println("Guava works! IDE autocompletion should recognize these imports.");
    }
}
