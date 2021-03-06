
import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

public class Main implements JmmParser {
	public JmmParserResult parse(String jmmCode) {
		try {
			Reports.clear();
			JMM jmm = new JMM(new StringReader(jmmCode));
    		SimpleNode root = jmm.Program(); // returns reference to root node

    		return new JmmParserResult(root, Reports.getReports());
		} catch (ParseException ex) {
			Reports.store(new Report(ReportType.ERROR, Stage.SYNTATIC, ex.currentToken.beginLine, ex.getMessage()));
			return new JmmParserResult(null, Reports.getReports());
		}
	}

	public JmmSemanticsResult analyse(JmmParserResult parserResult) {
		try {
			AnalysisStage analysisStage = new AnalysisStage();
			return analysisStage.semanticAnalysis(parserResult);
		} catch (Exception e) {
			e.printStackTrace();
			return new JmmSemanticsResult((JmmNode) null, null, Reports.getReports());
		}
	}

	public OllirResult generateOllir(JmmSemanticsResult semanticsResult, CommandLineArgs args) {
		try {
			OptimizationStage optimizationStage = new OptimizationStage();
			optimizationStage.args = args;
			return optimizationStage.toOllir(semanticsResult);
		} catch (Exception e) {
			e.printStackTrace();
			return new OllirResult(semanticsResult, null, Reports.getReports());
		}
	}

	public JasminResult generateJasmin(OllirResult ollirResult) {
		try {
			BackendStage backendStage = new BackendStage();
			return backendStage.toJasmin(ollirResult);
		}
		catch (Exception e) {
			e.printStackTrace();
			return new JasminResult(ollirResult, null, Reports.getReports());
		}
	}

	private static CommandLineArgs parseCommandLineArgs(String[] args) throws IllegalArgumentException {
		boolean optimize = false;
		String path = null;
		Integer maxRegisters = null;

		for (String arg : args) {
			if (arg.equals("-o")) {
				optimize = true;
			}
			else if (arg.startsWith("-r=")) {
				try {
					maxRegisters = Integer.parseInt(arg.substring(3));
				}
				catch (NumberFormatException ex) {
					throw new IllegalArgumentException("Number of registers must be an integer");
				}

				if (maxRegisters <= 0) {
					throw new IllegalArgumentException("Number of registers must be positive");
				}
			}
			else if (path == null) {
				path = arg;
			}
			else {
				throw new IllegalArgumentException("Invalid argument: " + arg);
			}
		}

		if (path == null) {
			throw new IllegalArgumentException("A path to a JMM file to compile must be provided");
		}

		return new CommandLineArgs(path, optimize, maxRegisters);
	}

	private static void printReports(List<Report> reports) {
		for (Report report : reports) {
			System.out.println(report);
		}
	}

	private static List<Report> getErrorReports(List<Report> reports) {
		return reports.stream().filter(report -> report.getType() == ReportType.ERROR).collect(Collectors.toList());
	}

    public static void main(String[] args) throws IOException {
		Main main = new Main();

		String folder = ".";

		CommandLineArgs parsedArgs;
		try {
			parsedArgs = parseCommandLineArgs(args);
		}
		catch (Exception ex) {
			System.out.println(ex.getMessage());
			return;
		}

		String jmmCode = new String((new FileInputStream(parsedArgs.path)).readAllBytes());
		JmmParserResult parserResult = main.parse(jmmCode);

		JmmSemanticsResult semanticsResult;
		if (getErrorReports(parserResult.getReports()).isEmpty()) {
			semanticsResult = main.analyse(parserResult);

			// ClassName.json
			File astJsonFile = new File(folder + File.separator + semanticsResult.getSymbolTable().getClassName()
					+ ".json");
			astJsonFile.createNewFile();
			FileWriter writer = new FileWriter(astJsonFile);
			writer.write(semanticsResult.getRootNode().toJson());
			writer.close();

			// ClassName.symbols.txt
			File symbolsFile = new File(folder + File.separator + semanticsResult.getSymbolTable().getClassName()
					+ ".symbols.txt");
			symbolsFile.createNewFile();
			writer = new FileWriter(symbolsFile);
			writer.write(semanticsResult.getSymbolTable().print());
			writer.close();
		}
		else {
			printReports(parserResult.getReports());
			return;
		}

		OllirResult ollirResult;
		if (getErrorReports(semanticsResult.getReports()).isEmpty()) {
			ollirResult = main.generateOllir(semanticsResult, parsedArgs);

			// ClassName.ollir
			File ollirCodeFile = new File(folder + File.separator + ollirResult.getOllirClass().getClassName()
					+ ".ollir");
			ollirCodeFile.createNewFile();
			FileWriter writer = new FileWriter(ollirCodeFile);
			writer.write(ollirResult.getOllirCode());
			writer.close();
		}
		else {
			printReports(semanticsResult.getReports());
			return;
		}

		JasminResult jasminResult;
		if (getErrorReports(ollirResult.getReports()).isEmpty()) {
			jasminResult = main.generateJasmin(ollirResult);

			// ClassName.j
			File jasminCodeFile = new File(folder + File.separator + jasminResult.getClassName() + ".j");
			jasminCodeFile.createNewFile();
			FileWriter writer = new FileWriter(jasminCodeFile);
			writer.write(jasminResult.getJasminCode());
			writer.close();

			// ClassName.class
			jasminResult.compile(new File(folder));
		}
		else {
			printReports(ollirResult.getReports());
		}
    }
}