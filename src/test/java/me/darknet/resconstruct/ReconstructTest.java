package me.darknet.resconstruct;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ReconstructTest {

	@Test
	public void test() {
		try {
			Reconstruct re = new Reconstruct();
			re.add(Objects.requireNonNull(Reconstruct.class.getResourceAsStream("/A.classx")));
			re.run();
			Map<String, ClassNode> builtNodes = re.build().values().stream()
					.map(ClassReader::new)
					.collect(Collectors.toMap(ClassReader::getClassName, cr -> {
						ClassNode node = new ClassNode();
						cr.accept(node, ClassReader.SKIP_CODE);
						return node;
					}));
			// Should have generated, B, C, D classes
			assertEquals(3, builtNodes.size());
			assertNotNull(builtNodes.get("B"), "Missing 'B' phantom");
			assertNotNull(builtNodes.get("C"), "Missing 'C' phantom");
			assertNotNull(builtNodes.get("D"), "Missing 'D' phantom");
		} catch (IOException ex) {
			fail(ex);
		}
	}

}