package me.darknet.resconstruct;

import me.coley.analysis.SimAnalyzer;
import me.coley.analysis.SimInterpreter;
import me.coley.analysis.TypeChecker;
import me.coley.analysis.TypeResolver;
import me.coley.analysis.exception.ResolvableExceptionFactory;
import me.coley.analysis.util.InheritanceGraph;
import me.coley.analysis.util.TypeUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Reconstruct {
	private static final InheritanceGraph GRAPH_BASE = new InheritanceGraph();
	private final ClassHierarchy hierarchy = new ClassHierarchy();
	private final List<byte[]> inputs = new ArrayList<>();
	private final InheritanceGraph graph;
	private final SimAnalyzer analyzer;

	static {
		// TODO: BG thread
		try {
			GRAPH_BASE.addClasspath();
		} catch (IOException ex) {
			throw new IllegalStateException("xdark fix this later", ex);
		}
	}

	public ClassHierarchy getHierarchy() {
		return hierarchy;
	}

	public InheritanceGraph getGraph() {
		return graph;
	}

	public SimAnalyzer getAnalyzer() {
		return analyzer;
	}

	public Reconstruct() {
		graph = GRAPH_BASE.copy();
		this.analyzer = new SimAnalyzer(new SimInterpreter()) {
			@Override
			protected ResolvableExceptionFactory createExceptionFactory() {
				// TODO: Do we need this, or can we use the silent option?
				return super.createExceptionFactory();
			}

			@Override
			public TypeChecker createTypeChecker() {
				return (parent, child) -> graph.getAllParents(child.getInternalName())
						.contains(parent.getInternalName());
			}

			@Override
			public TypeResolver createTypeResolver() {
				return new TypeResolver() {
					@Override
					public Type common(Type type1, Type type2) {
						String common = graph.getCommon(type1.getInternalName(), type2.getInternalName());
						if (common != null)
							return Type.getObjectType(common);
						return TypeUtil.OBJECT_TYPE;
					}

					@Override
					public Type commonException(Type type1, Type type2) {
						String common = graph.getCommon(type1.getInternalName(), type2.getInternalName());
						if (common != null)
							return Type.getObjectType(common);
						return TypeUtil.EXCEPTION_TYPE;
					}
				};
			}
		};
        analyzer.setThrowUnresolvedAnalyzerErrors(false);
	}

	public void add(InputStream classFileStream) throws IOException {
		add(classFileStream.readAllBytes());
	}

	public void add(byte[] classFile) {
		inputs.add(classFile);
		graph.addClass(classFile);
	}

	public void run() {
		for (byte[] classFile : inputs) {
			ClassReader cr = new ClassReader(classFile);
			PhantomVisitor visitor = new PhantomVisitor(Opcodes.ASM9, null, this);
			cr.accept(visitor, 0);
			ClassNode classNode = new ClassNode();
			cr.accept(classNode, 0);
			classNode.methods.forEach(m -> {
				// Hack to force ASM to cache 
			    if (m.instructions.size() > 0) {
			        m.instructions.get(0);
			    }
			});
			InstructionsSolver solver = new InstructionsSolver(this);
			solver.solve(hierarchy, classNode);
		}
	}

	public void build() {
		// TODO: Output to a Map<String, byte[]>
		hierarchy.phantoms.forEach(((type, phantomClass) -> {
			byte[] bytes = phantomClass.generate(Opcodes.V1_8);
			try {
				File file = new File("test", type.getInternalName() + ".class");
				file.getParentFile().mkdirs();
				Files.write(Paths.get("test", type.getInternalName() + ".class"), bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}));
	}

	// TODO: Move to better spot
	public static void main(String[] args) throws IOException {
		Reconstruct re = new Reconstruct();
		re.add(Objects.requireNonNull(Reconstruct.class.getResourceAsStream("/test.class")));
		re.run();
		re.build();
	}
}
