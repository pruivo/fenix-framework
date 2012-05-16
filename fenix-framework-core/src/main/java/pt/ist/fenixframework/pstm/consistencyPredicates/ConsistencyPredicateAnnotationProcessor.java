package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;


/**
 * Checks that all consistency predicates receive no parameters, return a
 * primitive boolean value, and are public, protected, or private. Otherwise,
 * throws an <code>Error</code> during the compilation.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes({ "pt.ist.fenixframework.pstm.consistencyPredicates.ConsistencyPredicate",
	"jvstm.cps.ConsistencyPredicate" })
public class ConsistencyPredicateAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
	final Set<MethodSymbol> elements = (Set<MethodSymbol>) roundEnv.getElementsAnnotatedWith(ConsistencyPredicate.class);
	elements.addAll((Set<MethodSymbol>) roundEnv.getElementsAnnotatedWith(jvstm.cps.ConsistencyPredicate.class));

	for (MethodSymbol method : elements) {
	    ClassSymbol classSymbol = (ClassSymbol) method.getEnclosingElement();
	    method.getModifiers();
	    if (!method.getParameters().isEmpty()) {
		throw new Error("Consistency Predicates cannot have parameters - " + classSymbol.getQualifiedName() + "."
			+ method.getQualifiedName() + "()");
	    }
	    if (!method.getReturnType().getKind().equals(TypeKind.BOOLEAN)) {
		throw new Error("Consistency Predicates must return a primitive boolean value - "
			+ classSymbol.getQualifiedName()
			+ "."
			+ method.getQualifiedName() + "()");
	    }
	    if (!method.getModifiers().contains(Modifier.PUBLIC) && !method.getModifiers().contains(Modifier.PRIVATE)
		    && !method.getModifiers().contains(Modifier.PROTECTED)) {
		throw new Error("Consistency Predicates must be private, protected or public - " + classSymbol.getQualifiedName()
			+ "." + method.getQualifiedName() + "()");
	    }
	}

	return true;
    }

}
