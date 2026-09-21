import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExitPlan extends JFrame {
    private final JSpinner returnTime = new JSpinner(new SpinnerDateModel());
    private final JSpinner outbound = new JSpinner(new SpinnerNumberModel(20,0,300,5));
    private final JSpinner inbound = new JSpinner(new SpinnerNumberModel(20,0,300,5));
    private final JSpinner parking = new JSpinner(new SpinnerNumberModel(5,0,120,5));
    private final JSpinner buffer = new JSpinner(new SpinnerNumberModel(15,0,180,5));
    private final DefaultTableModel activities = new DefaultTableModel(new Object[]{"Activity","Minutes"},0);
    private final DefaultTableModel timeline = new DefaultTableModel(new Object[]{"Step","Start","End","Duration"},0);
    private final JLabel leaveBy = new JLabel("--");
    private final JLabel totalTime = new JLabel("--");
    private final JLabel status = new JLabel("Add activities, then calculate your plan.");

    public ExitPlan() {
        super("ExitPlan — Reverse Planner");
        setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(920,680); setLocationRelativeTo(null);
        returnTime.setEditor(new JSpinner.DateEditor(returnTime,"MM/dd/yyyy hh:mm a"));
        activities.addRow(new Object[]{"Dinner",60});
        JPanel root=new JPanel(new BorderLayout(14,14)); root.setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
        JLabel title=new JLabel("ExitPlan"); title.setFont(title.getFont().deriveFont(Font.BOLD,28f));
        JLabel subtitle=new JLabel("Plan backward from the time you absolutely need to be back.");
        JPanel head=new JPanel(new GridLayout(2,1));head.add(title);head.add(subtitle);root.add(head,BorderLayout.NORTH);

        JPanel settings=new JPanel(new GridLayout(5,2,8,8));settings.setBorder(BorderFactory.createTitledBorder("Plan settings"));
        addField(settings,"Must be back by",returnTime);addField(settings,"Outbound travel (min)",outbound);addField(settings,"Return travel (min)",inbound);addField(settings,"Parking / walking each way",parking);addField(settings,"Safety buffer",buffer);

        JTable activityTable=new JTable(activities); JPanel activityPanel=new JPanel(new BorderLayout(6,6));activityPanel.setBorder(BorderFactory.createTitledBorder("Activities"));activityPanel.add(new JScrollPane(activityTable),BorderLayout.CENTER);
        JButton add=new JButton("+ Add activity"); JButton remove=new JButton("Remove selected"); JPanel activityButtons=new JPanel(new FlowLayout(FlowLayout.LEFT));activityButtons.add(add);activityButtons.add(remove);activityPanel.add(activityButtons,BorderLayout.SOUTH);
        add.addActionListener(e->activities.addRow(new Object[]{"New activity",30}));
        remove.addActionListener(e->{int r=activityTable.getSelectedRow();if(r>=0&&activities.getRowCount()>1)activities.removeRow(r);});

        JPanel centerTop=new JPanel(new GridLayout(1,2,12,12));centerTop.add(settings);centerTop.add(activityPanel);
        JTable timelineTable=new JTable(timeline); JPanel center=new JPanel(new BorderLayout(10,10));center.add(centerTop,BorderLayout.NORTH);center.add(new JScrollPane(timelineTable),BorderLayout.CENTER);root.add(center,BorderLayout.CENTER);

        JButton calculate=new JButton("Calculate reverse plan");
        JPanel metrics=new JPanel(new GridLayout(1,2,10,10));metrics.add(metric("LEAVE BY",leaveBy));metrics.add(metric("TOTAL OUTING",totalTime));
        JPanel bottom=new JPanel(new BorderLayout(8,8));bottom.add(metrics,BorderLayout.NORTH);bottom.add(status,BorderLayout.CENTER);bottom.add(calculate,BorderLayout.SOUTH);root.add(bottom,BorderLayout.SOUTH);
        calculate.addActionListener(e->calculate()); setContentPane(root);
    }
    private void addField(JPanel p,String name,JComponent c){p.add(new JLabel(name));p.add(c);}
    private JPanel metric(String name,JLabel value){JPanel p=new JPanel(new GridLayout(2,1));p.setBorder(BorderFactory.createEtchedBorder());JLabel n=new JLabel(name,SwingConstants.CENTER);value.setHorizontalAlignment(SwingConstants.CENTER);value.setFont(value.getFont().deriveFont(Font.BOLD,20f));p.add(n);p.add(value);return p;}
    private void calculate(){
        try{
            java.util.Date d=(java.util.Date)returnTime.getValue(); LocalDateTime deadline=LocalDateTime.ofInstant(d.toInstant(),ZoneId.systemDefault());
            int out=(Integer)outbound.getValue(), back=(Integer)inbound.getValue(), park=(Integer)parking.getValue(), safe=(Integer)buffer.getValue(), act=0;
            List<Activity> list=new ArrayList<>();
            for(int i=0;i<activities.getRowCount();i++){String name=String.valueOf(activities.getValueAt(i,0));int mins=Integer.parseInt(String.valueOf(activities.getValueAt(i,1)));if(mins<0)throw new IllegalArgumentException();list.add(new Activity(name,mins));act+=mins;}
            int total=out+back+park*2+safe+act; LocalDateTime leave=deadline.minusMinutes(total); DateTimeFormatter fmt=DateTimeFormatter.ofPattern("hh:mm a");
            leaveBy.setText(leave.format(fmt));totalTime.setText((total/60)+"h "+(total%60)+"m");timeline.setRowCount(0);
            LocalDateTime cursor=leave;timeline.addRow(new Object[]{"Leave",cursor.format(fmt),"—",out+" min travel"});cursor=cursor.plusMinutes(out+park);
            for(Activity a:list){LocalDateTime end=cursor.plusMinutes(a.minutes());timeline.addRow(new Object[]{a.name(),cursor.format(fmt),end.format(fmt),a.minutes()+" min"});cursor=end;}
            timeline.addRow(new Object[]{"Head home",cursor.format(fmt),deadline.format(fmt),(park+back+safe)+" min incl. buffer"});
            status.setText(leave.isBefore(LocalDateTime.now())?"⚠ This plan requires leaving now or earlier.":"✓ Plan is feasible based on the times entered.");
        }catch(Exception ex){JOptionPane.showMessageDialog(this,"Check activity names and minutes.","Invalid input",JOptionPane.ERROR_MESSAGE);}
    }
    public static void main(String[] args){SwingUtilities.invokeLater(()->new ExitPlan().setVisible(true));}
    record Activity(String name,int minutes){}
}
