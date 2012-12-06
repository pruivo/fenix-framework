package pt.ist.fenixframework;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.dml.CodeGenerator;
import pt.ist.fenixframework.dml.CompilerArgs;
import pt.ist.fenixframework.dml.DmlCompilerException;
import pt.ist.fenixframework.dml.DmlLexer;
import pt.ist.fenixframework.dml.DmlParser;
import pt.ist.fenixframework.dml.DmlTree;
import pt.ist.fenixframework.dml.DomainModel;

public class DmlCompiler {
    private static final Logger logger = LoggerFactory.getLogger(DmlCompiler.class);

    /**
     * Runs the DML compiler
     * 
     * This is the main entry point for running the DML compiler, from the
     * command line. This method will create the {@link CompilerArgs} from the
     * command line parameters, and then invoke {@link DmlCompiler#compile}.
     * 
     * @param args
     *            All the compiler's parameters
     * 
     * @see CompilerArgs
     */
    public static void main(String[] args) throws DmlCompilerException {
	CompilerArgs compArgs = new CompilerArgs(args);
	compile(compArgs);
    }

    /**
     * Runs the DML compiler
     * 
     * This is the main entry point for, programmatically, running the DML
     * compiler. The compiler will first create the domain model, and then run
     * the code generator.
     * 
     * @param compArgs
     *            All the compiler's parameters
     * @return The {@link DomainModel}
     * 
     * @see CompilerArgs
     */
    public static DomainModel compile(CompilerArgs compArgs) throws DmlCompilerException {
	try {
	    DomainModel model = getDomainModel(compArgs);
	    CodeGenerator generator = compArgs.getCodeGenerator().getConstructor(CompilerArgs.class, DomainModel.class)
		    .newInstance(compArgs, model);
	    generator.generateCode();
	    return model;
	} catch (Exception e) {
	    throw new DmlCompilerException(e);
	}
    }

    public static DomainModel getDomainModel(CompilerArgs compArgs) throws DmlCompilerException {
	// IMPORTANT: external specs must be first. The order is important for
	// the DmlCompiler
	List<URL> dmlSpecs = new ArrayList<URL>(compArgs.getExternalDomainSpecs());
	dmlSpecs.addAll(compArgs.getLocalDomainSpecs());
	return getDomainModel(dmlSpecs, false);
    }

    public static DomainModel getDomainModel(List<URL> dmlFilesURLs) throws DmlCompilerException {
	return getDomainModel(dmlFilesURLs, false);
    }

    public static DomainModel getDomainModel(List<URL> dmlFilesURLs, boolean checkForMissingExternals)
	    throws DmlCompilerException {
	if (logger.isTraceEnabled()) {
	    StringBuilder message = new StringBuilder();
	    for (URL url : dmlFilesURLs) {
		message.append(url + "  ***  ");
	    }

	    logger.trace("dmlFilesUrls = " + message.toString());
	}

	DomainModel model = new DomainModel();

	for (URL dmlFileURL : dmlFilesURLs) {
	    InputStream urlStream = null;
	    try {
		urlStream = dmlFileURL.openStream();

		DmlLexer lexer = new DmlLexer(new ANTLRInputStream());
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		DmlParser parser = new DmlParser(tokens);
		CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(parser.compilationUnit().getTree());
		DmlTree walker = new DmlTree(nodeStream);
		walker.compilationUnit(model, dmlFileURL);
	    } catch (IOException ioe) {
		System.err.println("Cannot read " + dmlFileURL + ".  Ignoring it...");
		// System.exit(3);
	    } catch (RecognitionException e) {
		throw new DmlCompilerException(e);
	    } finally {
		if (urlStream != null) {
		    try {
			urlStream.close();
		    } catch (IOException ioe) {
		    }
		}
	    }
	}

	model.finalizeDomain(checkForMissingExternals);
	return model;
    }
}
