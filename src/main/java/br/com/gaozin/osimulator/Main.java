package br.com.gaozin.osimulator;

import br.com.gaozin.osimulator.ui.SimulatorFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() {}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // O visual personalizado funciona mesmo se o tema do sistema não estiver disponível.
            }
            new SimulatorFrame().setVisible(true);
        });
    }
}
