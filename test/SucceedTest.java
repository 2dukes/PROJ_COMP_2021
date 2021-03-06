import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;
import java.io.StringReader;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

public class SucceedTest {
    public void testSucceed(String path, CommandLineArgs args) {
        String jmmCode = SpecsIo.getResource(path);
        JmmParserResult result = TestUtils.parse(jmmCode);
        TestUtils.noErrors(result.getReports());

        AnalysisStage analysisStage = new AnalysisStage();
        JmmSemanticsResult semanticsResult = analysisStage.semanticAnalysis(result);

        System.out.println("Semantic reports: " + semanticsResult.getReports());
        TestUtils.noErrors(semanticsResult.getReports());

        OptimizationStage optimizationStage = new OptimizationStage();
        optimizationStage.args = args;
        OllirResult ollirResult = optimizationStage.toOllir(semanticsResult);

        BackendStage backendStage = new BackendStage();
        JasminResult jasminResult = backendStage.toJasmin(ollirResult);

        System.out.println(jasminResult.getJasminCode());
        jasminResult.run();
    }

    public void testSucceed(String path) {
        testSucceed(path, new CommandLineArgs(null, false, null));
    }

    @Test
    public void testHelloWorld() {
        testSucceed("fixtures/public/HelloWorld.jmm");
    }

    @Test
    public void testSimple() {
        testSucceed("fixtures/public/Simple.jmm");
    }

    @Test
    public void testQuickSort() {
        testSucceed("fixtures/public/QuickSort.jmm");
    }

    @Test
    public void testLazySort() {
        testSucceed("fixtures/public/Lazysort.jmm");
    }

    @Test
    public void testFindMaximum() {
        testSucceed("fixtures/public/FindMaximum.jmm");
    }

    // The following tests take user input so they must be compiled and executed manually
    /*
    @Test
    public void testLife() {
        testSucceed("fixtures/public/Life.jmm");
    }

    @Test
    public void testMonteCarloPi() {
        testSucceed("fixtures/public/MonteCarloPi.jmm");
    }

    @Test
    public void testTicTacToe() {
        testSucceed("fixtures/public/TicTacToe.jmm");
    }
    */

    @Test
    public void testWhileAndIf() {
        testSucceed("fixtures/public/WhileAndIF.jmm");
    }

    @Test
    public void testMaxOverloading() {
        testSucceed("fixtures/public/MaxOverloading.jmm");
    }

    @Test
    public void testFibonacciAndFactorial() {
        testSucceed("fixtures/public/FibonacciAndFactorial.jmm");
    }

    @Test
    public void testConstantPropagation() {
        testSucceed("fixtures/public/ConstantPropagation.jmm", new CommandLineArgs(null, true, null));
    }
}
