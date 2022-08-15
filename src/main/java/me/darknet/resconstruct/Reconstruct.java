package me.darknet.resconstruct;

import me.coley.analysis.SimAnalyzer;
import me.coley.analysis.SimInterpreter;
import me.coley.analysis.TypeChecker;
import me.coley.analysis.TypeResolver;
import me.coley.analysis.exception.ResolvableExceptionFactory;
import me.coley.analysis.util.InheritanceGraph;
import me.coley.analysis.util.TypeUtil;
import me.darknet.resconstruct.util.InheritanceUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Reconstruct {
	private final ClassHierarchy hierarchy = new ClassHierarchy();
	private final Map<String, ClassReader> inputs = new HashMap<>();
	private final InheritanceGraph graph;

	public ClassHierarchy getHierarchy() {
		return hierarchy;
	}

	public InheritanceGraph getGraph() {
		return graph;
	}

	public Reconstruct() {
		graph = InheritanceUtils.getClasspathGraph().copy();
	}

	public void add(InputStream classFileStream) throws IOException {
		add(classFileStream.readAllBytes());
	}

	public void add(byte[] classFile) {
		ClassReader cr = new ClassReader(classFile);
		inputs.put(cr.getClassName(), cr);
		graph.addClass(classFile);
	}

	public void run() {
		for (ClassReader cr : inputs.values()) {
			PhantomVisitor visitor = new PhantomVisitor(Opcodes.ASM9, null, this);
			cr.accept(visitor, 0);
			ClassNode classNode = new ClassNode();
			cr.accept(classNode, 0);
			InstructionsSolver solver = new InstructionsSolver(this);
			solver.solve(hierarchy, classNode);
		}
	}

	public Map<String, byte[]> build() {
		return hierarchy.phantoms.entrySet().stream()
				.filter(e -> !e.getValue().isCp && !inputs.containsKey(e.getKey().getInternalName()))
				.collect(Collectors.toMap(
						e -> e.getKey().getInternalName(),
						e -> e.getValue().generate(Opcodes.V1_8)
				));
	}

	public SimAnalyzer newAnalyzer() {
		SimInterpreter interpreter = new SimInterpreter();
		SimAnalyzer analyzer = new SimAnalyzer(interpreter) {
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
		return analyzer;
	}
}
