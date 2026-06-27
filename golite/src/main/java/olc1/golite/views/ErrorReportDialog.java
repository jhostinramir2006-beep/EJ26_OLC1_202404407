package olc1.golite.views;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import olc1.golite.reports.GoliteError;

public class ErrorReportDialog extends JDialog {

    public ErrorReportDialog(JFrame parent,
                             List<GoliteError> lexical,
                             List<GoliteError> syntactic,
                             List<GoliteError> semantic) {

        super(parent, "Reporte de errores", true);

        String[] columns = {"No.", "Descripción", "Línea", "Columna", "Tipo"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int count = 1;

        for (GoliteError e : lexical) {
            model.addRow(new Object[]{
                count++,
                e.getDescription(),
                e.getLine(),
                e.getColumn(),
                "léxico"
            });
        }

        for (GoliteError e : syntactic) {
            model.addRow(new Object[]{
                count++,
                e.getDescription(),
                e.getLine(),
                e.getColumn(),
                "sintáctico"
            });
        }

        for (GoliteError e : semantic) {
            model.addRow(new Object[]{
                count++,
                e.getDescription(),
                e.getLine(),
                e.getColumn(),
                "semántico"
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.setFont(new Font("Arial", Font.PLAIN, 13));

        JScrollPane scrollPane = new JScrollPane(table);

        add(scrollPane, BorderLayout.CENTER);

        setSize(850, 500);
        setLocationRelativeTo(parent);
    }
}