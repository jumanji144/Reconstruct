import me.darknet.reconstruct.error.Result;
import me.darknet.reconstruct.model.ClassHierarchy;
import me.darknet.reconstruct.solvers.phantom.PhantomSolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ReconstructTest {

    private static final Map<String, byte[]> JAVA_BASE = new HashMap<>(128);

    @BeforeAll
    public static void init() throws IOException {
        JAVA_BASE.put("java/lang/Object", ClassLoader.getSystemResourceAsStream("java/lang/Object.class").readAllBytes());
    }

    private static <K, V> void assertElement(Map<K, V> map, K key, Consumer<V> consumer) {
        Assertions.assertTrue(map.containsKey(key));
        consumer.accept(map.get(key));
    }

    @Test
    public void testApplication() {
        test("simulation/Application.classx", hierarchy -> {
            assertElement(hierarchy.primary(), "simulation/Named", named -> {
                Assertions.assertNotNull(named.getMethod("getName", "()Ljava/lang/String;"));
            });
        });
    }

    private void test(String path, Consumer<ClassHierarchy> test) {
        try (InputStream is = Objects.requireNonNull(
                ReconstructTest.class.getClassLoader().getResourceAsStream(path))) {
            byte[] data = is.readAllBytes();
            ClassHierarchy hierarchy = ClassHierarchy.of(
                    Map.of(path.substring(0, path.lastIndexOf('.')), data), JAVA_BASE);

            PhantomSolver solver = new PhantomSolver();
            Result<ClassHierarchy> result = solver.solve(hierarchy);

            result.ifOk(test)
                    .ifErr(errors -> {
                       errors.forEach(System.err::println);
                       Assertions.fail();
                    });
        } catch (IOException e) {
            Assertions.fail(e);
        }

    }

}
