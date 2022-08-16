package me.darknet.resconstruct;

import me.darknet.resconstruct.util.AccessUtils;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ReconstructTest {
	@Test
	public void testSimulation() throws IOException {
		test("/simulation/Application.classx", (reconstruct, builtNodes) -> {
			// Should have generated: Cat, Person, Creature, Living, Named, World
			assertEquals(6, builtNodes.size());
			// No reason to suggest class, can use interface
			//  - World isn't written as one, but from 'Application' alone that cannot be determined.
			assertTrue(AccessUtils.isInterface(builtNodes.get("simulation/Living").access));
			assertTrue(AccessUtils.isInterface(builtNodes.get("simulation/Named").access));
			assertTrue(AccessUtils.isInterface(builtNodes.get("simulation/World").access));
			// Constructors called, must be classes
			assertFalse(AccessUtils.isInterface(builtNodes.get("simulation/Cat").access));
			assertFalse(AccessUtils.isInterface(builtNodes.get("simulation/Person").access));
			assertFalse(AccessUtils.isInterface(builtNodes.get("simulation/Creature").access));
			// TODO: assertions for model inheritance order
		});
	}

	@Test
	public void testSimple() {
		test("/simple/A.classx", (reconstruct, builtNodes) -> {
			// Should have generated: B, C, D classes
			assertEquals(3, builtNodes.size());
			assertNotNull(builtNodes.get("B"), "Missing 'B' phantom");
			assertNotNull(builtNodes.get("C"), "Missing 'C' phantom");
			assertNotNull(builtNodes.get("D"), "Missing 'D' phantom");
		});
	}

	private void test(String path, BiConsumer<Reconstruct, Map<String, ClassNode>> test) {
		try {
			InputStream is = Objects.requireNonNull(Reconstruct.class.getResourceAsStream(path));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int read = 0;
			while ((read = is.read(buffer)) > 0) {
				baos.write(buffer, 0, read);
			}
			byte[] classFile = baos.toByteArray();

			Reconstruct re = new Reconstruct();
			re.add(classFile);
			re.run();
			System.out.println(re.getHierarchy());
			Map<String, ClassNode> builtNodes = re.build().values().stream()
					.map(ClassReader::new)
					.collect(Collectors.toMap(ClassReader::getClassName, cr -> {
						ClassNode node = new ClassNode();
						cr.accept(node, ClassReader.SKIP_CODE);
						return node;
					}));
			test.accept(re, builtNodes);
		} catch (IOException ex) {
			fail(ex);
		}
	}

}