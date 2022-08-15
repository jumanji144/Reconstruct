package me.darknet.resconstruct;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ReconstructTest {

	@Test
	public void test() {
		try {
			Reconstruct re = new Reconstruct();
			re.add(Objects.requireNonNull(Reconstruct.class.getResourceAsStream("/test.class")));
			re.run();
			Map<String, byte[]> build = re.build();
			Map<String, ClassNode> builtNodes = build.values().stream()
					.map(ClassReader::new)
					.collect(Collectors.toMap(ClassReader::getClassName, cr -> {
						ClassNode node = new ClassNode();
						cr.accept(node, ClassReader.SKIP_CODE);
						return node;
					}));
			assertEquals(4, build.size());
		} catch (IOException ex) {
			fail(ex);
		}
	}

}