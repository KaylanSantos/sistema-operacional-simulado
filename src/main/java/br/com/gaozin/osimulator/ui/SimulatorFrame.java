package br.com.gaozin.osimulator.ui;

import br.com.gaozin.osimulator.events.SimulationEvent;
import br.com.gaozin.osimulator.process.ProcessState;
import br.com.gaozin.osimulator.process.SimProcess;
import br.com.gaozin.osimulator.scheduler.SchedulingAlgorithm;
import br.com.gaozin.osimulator.simulation.SimulationEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public final class SimulatorFrame extends JFrame {
    private final Color bg = new Color(15, 23, 42);
    private final Color panel = new Color(30, 41, 59);
    private final Color muted = new Color(148, 163, 184);
    private final Color blue = new Color(59, 130, 246);
    private final Color green = new Color(16, 185, 129);
    private final Color amber = new Color(245, 158, 11);
    private final SimulationEngine engine = new SimulationEngine();

    private final JLabel tickValue = new JLabel("0");
    private final JLabel cpuValue = new JLabel("0%");
    private final JLabel ramValue = new JLabel("0 / 1024");
    private final JLabel cacheValue = new JLabel("0%");
    private final JLabel faultsValue = new JLabel("0");
    private final JLabel runningValue = new JLabel("Nenhum");
    private final JLabel statusValue = new JLabel("PAUSADO");
    private final JComboBox<SchedulingAlgorithm> algorithmBox = new JComboBox<>(SchedulingAlgorithm.values());
    private final JSpinner quantumSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
    private final JSpinner speedSpinner = new JSpinner(new SpinnerNumberModel(500, 100, 2000, 100));
    private final DefaultTableModel processModel = new DefaultTableModel(
            new Object[]{"PID", "Processo", "Estado", "Chegada", "CPU inicial", "Restante", "Prioridade", "RAM", "Espera", "Retorno"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final DefaultTableModel frameModel = new DefaultTableModel(new Object[]{"Quadro", "Proprietário", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final DefaultTableModel cacheModel = new DefaultTableModel(new Object[]{"Endereço físico simulado"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTextArea eventArea = new JTextArea();
    private final JProgressBar ramBar = new JProgressBar(0, 100);
    private final Timer timer;
    private boolean started;

    public SimulatorFrame() {
        super("OS Simulator — Sistema Operacional Simulado");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setSize(1450, 900);
        setLocationRelativeTo(null);
        getContentPane().setBackground(bg);
        setLayout(new BorderLayout(12, 12));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMain(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        styleTable(new JTable(processModel));
        timer = new Timer((Integer) speedSpinner.getValue(), e -> {
            engine.advanceIfRunning();
            refreshAll();
        });
        speedSpinner.addChangeListener(e -> timer.setDelay((Integer) speedSpinner.getValue()));
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                timer.stop();
                dispose();
            }
        });
        refreshAll();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setBackground(bg);
        header.setBorder(new EmptyBorder(12, 16, 0, 16));
        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("OS SIMULATOR");
        title.setForeground(Color.WHITE); title.setFont(new Font("SansSerif", Font.BOLD, 25));
        JLabel subtitle = new JLabel("Escalonamento • RAM • Paginação • Cache");
        subtitle.setForeground(muted); subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        titleBox.add(title); titleBox.add(Box.createVerticalStrut(3)); titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        controls.setOpaque(false);
        controls.add(label("Algoritmo"));
        algorithmBox.setSelectedItem(engine.getAlgorithm());
        controls.add(algorithmBox);
        controls.add(label("Quantum"));
        controls.add(quantumSpinner);
        controls.add(label("Velocidade ms"));
        controls.add(speedSpinner);
        JButton start = button("▶ Iniciar", green);
        JButton pause = button("Ⅱ Pausar", amber);
        JButton step = button("⏭ 1 ciclo", blue);
        JButton reset = button("↻ Reiniciar", new Color(100, 116, 139));
        JButton add = button("+ Processo", new Color(124, 58, 237));
        controls.add(start); controls.add(pause); controls.add(step); controls.add(reset); controls.add(add);
        header.add(controls, BorderLayout.SOUTH);

        start.addActionListener(e -> {
            try {
                if (engine.getTick() == 0) {
                    engine.setAlgorithm((SchedulingAlgorithm) algorithmBox.getSelectedItem());
                    engine.setQuantum((Integer) quantumSpinner.getValue());
                } else {
                    algorithmBox.setSelectedItem(engine.getAlgorithm());
                }
                engine.start();
                started = true;
                timer.start();
                refreshAll();
            } catch (RuntimeException ex) { showError(ex.getMessage()); }
        });
        pause.addActionListener(e -> { engine.pause(); timer.stop(); refreshAll(); });
        step.addActionListener(e -> { timer.stop(); engine.step(); refreshAll(); });
        reset.addActionListener(e -> {
            timer.stop();
            engine.reset();
            started = false;
            algorithmBox.setSelectedItem(engine.getAlgorithm());
            refreshAll();
        });
        add.addActionListener(e -> addProcessDialog());
        return header;
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(0, 16, 0, 16));

        JPanel cards = new JPanel(new GridLayout(1, 5, 10, 0));
        cards.setOpaque(false);
        cards.add(metricCard("CICLO LÓGICO", tickValue, blue));
        cards.add(metricCard("CPU UTILIZADA", cpuValue, green));
        cards.add(metricCard("RAM OCUPADA", ramValue, amber));
        cards.add(metricCard("CACHE HIT RATE", cacheValue, green));
        cards.add(metricCard("FALTAS DE PÁGINA", faultsValue, amber));

        JPanel upper = new JPanel(new BorderLayout(10, 10));
        upper.setOpaque(false);
        upper.add(cards, BorderLayout.NORTH);
        JPanel runCard = new JPanel(new BorderLayout(8, 8));
        runCard.setBackground(panel);
        runCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel runTitle = sectionTitle("CPU / PROCESSO EM EXECUÇÃO");
        runningValue.setForeground(Color.WHITE);
        runningValue.setFont(new Font("SansSerif", Font.BOLD, 18));
        statusValue.setForeground(amber);
        JPanel runTop = new JPanel(new BorderLayout());
        runTop.setOpaque(false);
        runTop.add(runTitle, BorderLayout.WEST);
        runTop.add(statusValue, BorderLayout.EAST);
        runCard.add(runTop, BorderLayout.NORTH);
        runCard.add(runningValue, BorderLayout.CENTER);
        JLabel help = new JLabel("O tempo é lógico; a velocidade controla apenas a atualização visual.");
        help.setForeground(muted); help.setFont(new Font("SansSerif", Font.PLAIN, 12));
        runCard.add(help, BorderLayout.SOUTH);
        upper.add(runCard, BorderLayout.CENTER);
        main.add(upper, BorderLayout.NORTH);

        JTable processTable = new JTable(processModel);
        styleTable(processTable);
        JScrollPane processScroll = scroll(processTable);
        processScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)), "PROCESSOS"));
        processScroll.setPreferredSize(new Dimension(800, 260));

        JPanel memoryPanel = new JPanel(new BorderLayout(6, 6));
        memoryPanel.setBackground(panel);
        memoryPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        memoryPanel.add(sectionTitle("RAM — QUADROS FÍSICOS"), BorderLayout.NORTH);
        JTable frameTable = new JTable(frameModel);
        styleTable(frameTable);
        memoryPanel.add(scroll(frameTable), BorderLayout.CENTER);
        ramBar.setStringPainted(true);
        ramBar.setForeground(blue);
        ramBar.setBackground(new Color(51, 65, 85));
        memoryPanel.add(ramBar, BorderLayout.SOUTH);

        JPanel cachePanel = new JPanel(new BorderLayout(6, 6));
        cachePanel.setBackground(panel);
        cachePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        cachePanel.add(sectionTitle("CACHE LRU"), BorderLayout.NORTH);
        JTable cacheTable = new JTable(cacheModel);
        styleTable(cacheTable);
        cachePanel.add(scroll(cacheTable), BorderLayout.CENTER);

        JPanel right = new JPanel(new GridLayout(2, 1, 0, 10));
        right.setOpaque(false);
        right.add(memoryPanel); right.add(cachePanel);
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.add(processScroll, BorderLayout.CENTER);
        center.add(right, BorderLayout.EAST);
        right.setPreferredSize(new Dimension(330, 500));
        main.add(center, BorderLayout.CENTER);
        return main;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 8));
        footer.setBackground(bg);
        footer.setBorder(new EmptyBorder(0, 16, 12, 16));
        JLabel title = sectionTitle("LOG DE EVENTOS");
        eventArea.setEditable(false);
        eventArea.setBackground(new Color(2, 6, 23));
        eventArea.setForeground(new Color(203, 213, 225));
        eventArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        eventArea.setRows(7);
        JScrollPane logScroll = scroll(eventArea);
        JPanel logPanel = new JPanel(new BorderLayout(6, 6));
        logPanel.setBackground(panel);
        logPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        logPanel.add(title, BorderLayout.NORTH);
        logPanel.add(logScroll, BorderLayout.CENTER);
        footer.add(logPanel, BorderLayout.CENTER);
        JLabel note = new JLabel("Simulação educacional • Java • Sem acesso ao hardware real");
        note.setForeground(muted);
        note.setBorder(new EmptyBorder(6, 2, 0, 0));
        footer.add(note, BorderLayout.SOUTH);
        return footer;
    }

    private JPanel metricCard(String title, JLabel value, Color accent) {
        JPanel p = new JPanel(new BorderLayout(4, 8));
        p.setBackground(panel);
        p.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel t = new JLabel(title);
        t.setForeground(muted); t.setFont(new Font("SansSerif", Font.BOLD, 10));
        value.setForeground(accent); value.setFont(new Font("SansSerif", Font.BOLD, 22));
        p.add(t, BorderLayout.NORTH); p.add(value, BorderLayout.CENTER);
        return p;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(226, 232, 240));
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        return l;
    }
    private JLabel label(String text) { JLabel l = new JLabel(text); l.setForeground(muted); return l; }
    private JButton button(String text, Color color) {
        JButton b = new JButton(text);
        b.setForeground(Color.WHITE); b.setBackground(color); b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(7, 10, 7, 10));
        return b;
    }
    private JScrollPane scroll(Component c) {
        JScrollPane s = new JScrollPane(c);
        s.getViewport().setBackground(panel);
        s.setBorder(BorderFactory.createEmptyBorder());
        return s;
    }
    private void styleTable(JTable table) {
        table.setBackground(panel);
        table.setForeground(new Color(226, 232, 240));
        table.setGridColor(new Color(51, 65, 85));
        table.setSelectionBackground(new Color(37, 99, 235));
        table.setSelectionForeground(Color.WHITE);
        table.setRowHeight(25);
        table.getTableHeader().setBackground(new Color(15, 23, 42));
        table.getTableHeader().setForeground(new Color(203, 213, 225));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        table.setFillsViewportHeight(true);
    }

    private void addProcessDialog() {
        JTextField name = new JTextField("NovoProcesso");
        JSpinner arrival = new JSpinner(new SpinnerNumberModel(engine.getTick(), 0, 100000, 1));
        JSpinner burst = new JSpinner(new SpinnerNumberModel(4, 1, 100000, 1));
        JSpinner priority = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        JSpinner memory = new JSpinner(new SpinnerNumberModel(128, 1, 1024, 64));
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Nome")); form.add(name);
        form.add(new JLabel("Instante de chegada")); form.add(arrival);
        form.add(new JLabel("Duração CPU (ciclos)")); form.add(burst);
        form.add(new JLabel("Prioridade (1 = maior)")); form.add(priority);
        form.add(new JLabel("Memória solicitada (unidades)")); form.add(memory);
        int result = JOptionPane.showConfirmDialog(this, form, "Criar processo", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                engine.addProcess(name.getText(), (Integer) arrival.getValue(), (Integer) burst.getValue(),
                        (Integer) priority.getValue(), (Integer) memory.getValue());
                refreshAll();
            } catch (RuntimeException ex) { showError(ex.getMessage()); }
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message == null ? "Ocorreu um erro." : message, "Não foi possível executar", JOptionPane.WARNING_MESSAGE);
    }

    private void refreshAll() {
        tickValue.setText(Integer.toString(engine.getTick()));
        cpuValue.setText(String.format("%.1f%%", engine.getCpuUtilization()));
        ramValue.setText(engine.getMemory().getAllocatedUnits() + " / " + engine.getMemory().getCapacity());
        cacheValue.setText(String.format("%.1f%%", engine.getCache().getHitRate()));
        faultsValue.setText(Long.toString(engine.getPaging().getPageFaults()));
        SimProcess running = engine.getRunning();
        runningValue.setText(running == null ? "CPU ociosa" :
                "P" + running.getPid() + " — " + running.getName() + " | restante: " + running.getRemainingTime() + " ciclos");
        statusValue.setText(engine.isFinished() ? "CONCLUÍDO" : engine.isPaused() ? "PAUSADO" : "EXECUTANDO");
        statusValue.setForeground(engine.isFinished() ? green : engine.isPaused() ? amber : green);

        processModel.setRowCount(0);
        for (SimProcess p : engine.getProcesses()) {
            processModel.addRow(new Object[]{
                    "P" + p.getPid(), p.getName(), stateLabel(p.getState()), p.getArrivalTime(),
                    p.getFirstRunTime() < 0 ? "—" : p.getFirstRunTime(), p.getRemainingTime(),
                    p.getPriority(), p.getMemoryUnits(),
                    p.waitingTime() < 0 ? "—" : p.waitingTime(),
                    p.turnaroundTime() < 0 ? "—" : p.turnaroundTime()
            });
        }

        frameModel.setRowCount(0);
        for (int i = 0; i < engine.getMemory().getFrameCount(); i++) {
            Integer owner = engine.getMemory().getFrameOwner(i);
            frameModel.addRow(new Object[]{i, owner == null ? "—" : "P" + owner, owner == null ? "LIVRE" : "OCUPADO"});
        }
        int pct = (int) Math.round(100.0 * engine.getMemory().getAllocatedUnits() / engine.getMemory().getCapacity());
        ramBar.setValue(pct);
        ramBar.setString(pct + "% utilizado");

        cacheModel.setRowCount(0);
        for (Long address : engine.getCache().getCachedAddresses()) cacheModel.addRow(new Object[]{address});
        eventArea.setText("");
        List<SimulationEvent> events = engine.getEvents();
        int start = Math.max(0, events.size() - 100);
        for (int i = start; i < events.size(); i++) eventArea.append(events.get(i).toString() + "\n");
        eventArea.setCaretPosition(eventArea.getDocument().getLength());

        if (engine.isFinished()) timer.stop();
    }

    private String stateLabel(ProcessState state) {
        return switch (state) {
            case NEW -> "NOVO";
            case READY -> "PRONTO";
            case RUNNING -> "EXECUTANDO";
            case BLOCKED -> "BLOQUEADO";
            case TERMINATED -> "CONCLUÍDO";
        };
    }
}
