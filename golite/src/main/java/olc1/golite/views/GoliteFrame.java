package olc1.golite.views;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import java.io.BufferedReader;
import java.io.StringReader;

import olc1.golite.Lexer;
import olc1.golite.parser;
import olc1.golite.ast.ASTNode;
import olc1.golite.reports.GoliteError;
import olc1.golite.visitor.interpreter.InterpreterVisitor;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.awt.Desktop;
import olc1.golite.reports.AstGraphGenerator;

public class GoliteFrame extends JFrame {
    private final EditorPanel editorPanel;
    private final JTextArea consoleTextArea;
    private InterpreterVisitor interpreter;
    private Lexer lexer;
    private parser parser;

    public GoliteFrame() {
        setTitle("Golite");
        setMinimumSize(new Dimension(600, 400));
        setSize(new Dimension(1200, 675));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        editorPanel = new EditorPanel();
        consoleTextArea = new JTextArea();
        cleanConsole();

        GoliteMenuBar menuBar = new GoliteMenuBar();
        setJMenuBar(menuBar);
        add(new MainPanel(editorPanel, consoleTextArea));

        wireActions(menuBar);

        setVisible(true);
        editorPanel.getTextArea().requestFocus();
    }

    private void wireActions(GoliteMenuBar menuBar) {
        menuBar.onLoad(e -> loadFile());
        menuBar.onRun(e -> run());
        menuBar.onClean(e -> cleanConsole());
        menuBar.onNew(e -> editorPanel.setText(""));
        menuBar.onExit(e -> System.exit(0));
        menuBar.onTokens(e -> {
            /* TODO: reporte de tokens */ });
        menuBar.onErrors(e -> { errors(); });
        menuBar.onAst(e -> astReport());
        menuBar.onAbout(e -> JOptionPane.showMessageDialog(
                this,
                "GolLite\nVersión 1.0.0\nLaboratorio OLC1",
                "Acerca de",
                JOptionPane.INFORMATION_MESSAGE));
    }

    private void run() {
        cleanConsole();

        lexer = null;
        parser = null;
        interpreter = null;

        try {
            lexer = new Lexer(new BufferedReader(new StringReader(editorPanel.getText())));
            parser = new parser(lexer);

            Object result = parser.parse().value;

            if (!(result instanceof ASTNode ast)) {
                consoleTextArea.append("No se pudo ejecutar porque no se generó AST recuperable.\n");
                return;
            }

            interpreter = new InterpreterVisitor();

            try {
                interpreter.Visit(ast);
            } catch (Exception e) {
                interpreter.semanticErrors.add(
                    new GoliteError("Semantico", e.getMessage(), -1, -1)
                );
            }

            consoleTextArea.append(interpreter.output);

            if (!lexer.errors.isEmpty() || !parser.errors.isEmpty()
                    || !interpreter.semanticErrors.isEmpty()) {
                consoleTextArea.append("\nEjecución finalizada con errores. Revise el reporte de errores.\n");
            }

        } catch (Exception e) {
            consoleTextArea.append("No se pudo ejecutar completamente. Revise el reporte de errores.\n");
        }

        consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
        editorPanel.getTextArea().requestFocus();
    }
    private void errors() {
        if (lexer == null || parser == null) {
            JOptionPane.showMessageDialog(
                this,
                "Aún no se ha ejecutado nada.",
                "Reporte de errores",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        java.util.List<GoliteError> semantic =
                interpreter != null ? interpreter.semanticErrors : new java.util.ArrayList<>();

        ErrorReportDialog dialog = new ErrorReportDialog(
                this,
                lexer.errors,
                parser.errors,
                semantic
        );

        dialog.setVisible(true);
    }
    private void cleanConsole() {
        consoleTextArea.setText("CONSOLA  -  LABORATORIO DE ORGANIZACION DE LENGUAJES Y COMPILADORES 1\n\n");
    }

    public EditorPanel getEditorPanel() {
        return editorPanel;
    }

    public JTextArea getConsoleTextArea() {
        return consoleTextArea;
    }
    private void loadFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Cargar archivo GoLite");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Archivos GoLite (*.glt, *.go, *.txt)",
                "glt", "go", "txt"
        ));

        int result = chooser.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

            editorPanel.setText(content);
            cleanConsole();
            consoleTextArea.append("Archivo cargado: " + file.getName() + "\n");

        } catch (Exception e) {
            cleanConsole();
            consoleTextArea.append("Error al cargar archivo: " + e.getMessage() + "\n");
        }

        editorPanel.getTextArea().requestFocus();
    }
    private void astReport() {
        try {
            lexer = new Lexer(new BufferedReader(new StringReader(editorPanel.getText())));
            parser = new parser(lexer);

            Object result = parser.parse().value;

            if (!(result instanceof ASTNode ast)) {
                JOptionPane.showMessageDialog(
                        this,
                        "No se pudo generar el AST.",
                        "Reporte AST",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            AstGraphGenerator generator = new AstGraphGenerator();
            String dot = generator.generate(ast);

            File dotFile = File.createTempFile("Reporte_AST_", ".dot");
            File pngFile = new File(
                    dotFile.getParentFile(),
                    dotFile.getName().replace(".dot", ".png")
            );

            Files.writeString(dotFile.toPath(), dot, StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder(
                    "dot",
                    "-Tpng",
                    dotFile.getAbsolutePath(),
                    "-o",
                    pngFile.getAbsolutePath()
            );

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0 || !pngFile.exists()) {
                JOptionPane.showMessageDialog(
                        this,
                        "No se pudo generar el PNG. Verifica que Graphviz esté instalado y que el comando dot funcione.",
                        "Reporte AST",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            Desktop.getDesktop().browse(pngFile.toURI());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Error generando AST: " + e.getMessage(),
                    "Reporte AST",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
