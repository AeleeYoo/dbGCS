import java.awt.Rectangle;
import java.awt.event.*;
import javax.swing.border.*;
import com.teamdev.jxmaps.*;
import com.teamdev.jxmaps.Icon;
import com.teamdev.jxmaps.MouseEvent;
import com.teamdev.jxmaps.swing.MapView;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.awt.*;
import java.awt.Point;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
/*
 * Created by JFormDesigner on Mon Oct 08 16:19:12 KST 2018
 */

/**
 * @author User #3
 */
public class
MainFrame extends MapView {
    //    static SecondWindow secondWindow = new SecondWindow();

    private OptionsWindow optionsWindowCommand;
    private OptionsWindow optionsWindowStatus;
    private OptionsWindow optionsWindowMeanVelocity;
    double X, Y, Z;
    static Marker marker;
    Polyline eventPolyline;
    static Map map;
    Icon icon = new Icon();
    int textFieldIndex = 0;
    double wpArray[][] = new double[20][3];
    static String port;
    static OutputStream out;
    static String strCT;
    Marker[][] markers = new Marker[3][15];
    Polyline[][] drawLine = new Polyline[3][14];
    public InfoWindow[][] infoWindow = new InfoWindow[3][15];

    Icon icon1 = new Icon();
    Icon icon2 = new Icon();
    Icon icon3 = new Icon();

    public MainFrame() {

        setOnMapReadyHandler(new MapReadyHandler() {
            @Override
            public void onMapReady(MapStatus status) {
                map = getMap();
                map.setCenter(new LatLng(37.138730,	127.138730)); // 초기 위치를 건대로 설정
                map.setZoom(10.0); // Setting initial zoom value
                MapOptions options = new MapOptions();
                options.setZoomControl(false);
                options.setMapTypeControl(false);
                options.setStreetViewControl(false);
                getMap().setOptions(options);
                marker = new Marker(map);
            }
        });
        initComponents();
        icon1.loadFromStream(this.getClass().getResourceAsStream("mm_20_green.png"), "png");
        icon2.loadFromStream(this.getClass().getResourceAsStream("mm_20_blue.png"), "png");
        icon3.loadFromStream(this.getClass().getResourceAsStream("mm_20_purple.png"), "png");
    }

    @Override
    public void addNotify() {
        super.addNotify();

        optionsWindowCommand = new OptionsWindow(this, new Dimension(225,220)) {
            @Override
            public void initContent(JWindow contentWindow) {
                JPanel content2 = new JPanel(new BorderLayout());
                content2.setBackground(Color.white);
                content2.setSize(225, 220);

                LatLng pathData[] = new LatLng[2];
                content2.add(mainPanelCommand, BorderLayout.CENTER);
                contentWindow.getContentPane().add(content2);
            }

            @Override
            protected void updatePosition() {
                if (parentFrame.isVisible()) {
                    Point newLocation = parentFrame.getContentPane().getLocationOnScreen();
                    newLocation.translate(0, 0); // status 화면 오른쪽에 붙이기
                    contentWindow.setLocation(newLocation);
                    contentWindow.setSize(225, 220);
                }
            }
        };

        optionsWindowStatus = new OptionsWindow(this, new Dimension(357,495)) {
            @Override
            public void initContent(JWindow contentWindow) {
                JPanel content2 = new JPanel(new BorderLayout());
                content2.setBackground(Color.white);
                content2.setSize(357, 495);

                LatLng pathData[] = new LatLng[2];
                content2.add(mainInformation, BorderLayout.CENTER);
                contentWindow.getContentPane().add(content2);
            }

            @Override
            protected void updatePosition() {
                if (parentFrame.isVisible()) {
                    Point newLocation = parentFrame.getContentPane().getLocationOnScreen();
                    newLocation.translate(1550, 0); // status 화면 오른쪽에 붙이기
                    contentWindow.setLocation(newLocation);
                    contentWindow.setSize(352, 495);
                }
            }
        };

        optionsWindowMeanVelocity = new OptionsWindow(this, new Dimension(357,159)) {
            @Override
            public void initContent(JWindow contentWindow) {
                JPanel content2 = new JPanel(new BorderLayout());
                content2.setBackground(Color.white);
                content2.setSize(357, 159);

                LatLng pathData[] = new LatLng[2];
                content2.add(mainVelocity, BorderLayout.CENTER);
                contentWindow.getContentPane().add(content2);
            }

            @Override
            protected void updatePosition() {
                if (parentFrame.isVisible()) {
                    Point newLocation = parentFrame.getContentPane().getLocationOnScreen();
                    newLocation.translate(1550, 495); // status 화면 오른쪽에 붙이기
                    contentWindow.setLocation(newLocation);
                    contentWindow.setSize(352, 159);
                }
            }
        };
    }

    public static void main(String[] args) {

        final MainFrame mapView = new MainFrame();

        JFrame frame = new JFrame("군집 무인기 상태창");

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(mapView, BorderLayout.CENTER);
        frame.setSize(1920, 1080);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static public Polyline polyline(String color){
        Polyline pl = new Polyline(map);
        PolylineOptions option = new PolylineOptions();
        option.setGeodesic(true);
        option.setStrokeColor(color);
        option.setStrokeOpacity(1.0);
        option.setStrokeWeight(2.0);
        pl.setOptions(option);
        return pl;
    }
    int num = 0;
    int[] okButtonActionPerformed(ActionEvent e) {
        int query[] = new int[3];
        query[0] = Integer.parseInt(String.valueOf(testCombo.getSelectedItem()));
        query[1] = Integer.parseInt(String.valueOf(missionCombo.getSelectedItem()));
        if(uav1.isSelected()) query[2] = 1;
        else if(uav2.isSelected()) query[2] = 2;
        else if(uav3.isSelected()) query[2] = 3;
        else if(uavAll.isSelected()) query[2] = 4;
        System.out.println(query[0] +" " + query[1] + " " + query[2]);
        if(markers[0][0]!=null ) removeMap(0);
        if(markers[1][0]!=null) removeMap(1);
        if(markers[2][0]!=null) removeMap(2);
        setOption(query[2]);
        num++;
        return query;
    }

    void removeMap(int j){
        for(int i = 0; i<15; i++){
            markers[j][i].remove();
            infoWindow[j][i].close();
            if(i<14) drawLine[j][i].remove();
        }
    }

    void setOption(int index){
        switch (index){
            case 1 : route(1, icon1, "test22");
                    break;
            case 2 : route(2, icon2, "test33");
                    break;
            case 3 : route(3, icon3, "test123");
                    break;
            case 4 : route(1, icon1, "test22");  route(2, icon2, "test33");   route(3, icon3, "test123");
                break;

        }
    }

    void route(int index, Icon setIcon, String textName){

        readFile(textName);
        LatLng[] path = new LatLng[2];
        for(int i =1; i<15; i++){
            path[0] = new LatLng(reaArray[i-1][0], reaArray[i-1][1]);
            path[1] = new LatLng(reaArray[i][0], reaArray[i][1]);
            drawLine[index-1][i-1] = polyline("#22741C");
            drawLine[index-1][i-1].setPath(path);
            markers[index-1][i-1] = new Marker(map);
            markers[index-1][i-1].setIcon(setIcon);
            markers[index-1][i-1].setPosition(path[0]);
            infoWindow[index-1][i-1] = new InfoWindow(map);
            infoWindow[index-1][i-1].setContent(String.valueOf(i));
            infoWindow[index-1][i-1].open(map, markers[index-1][i-1]);
            final int num = i;
            markers[index-1][num-1].addEventListener("click", new MapMouseEvent() {
                @Override
                public void onEvent(MouseEvent mouseEvent) {
                    setText(index, num-1);
                }
            });
        }
        markers[index-1][14] = new Marker(map);
        markers[index-1][14].setIcon(setIcon);
        markers[index-1][14].setPosition(path[1]);
        infoWindow[index-1][14] = new InfoWindow(map);
        infoWindow[index-1][14].setContent(String.valueOf(15));
        infoWindow[index-1][14].open(map, markers[index-1][14]);
        markers[index-1][14].addEventListener("click", new MapMouseEvent() {
            @Override
            public void onEvent(MouseEvent mouseEvent) {
                setText(index, 14);
            }
        });
    }


    void setText(int index, int num){
        itextArea18.setText(String.valueOf(infoWindow[index-1][num].getContent()));
        itextArea4.setText(String.valueOf(reaArray[num][0]));
        itextArea5.setText(String.valueOf(reaArray[num][1]));
    }

    static double reaArray[][] = new double[15][2];
    static void readFile(String textName){
        File file = new File("data\\" +textName + ".txt");
        String s[];
        int size = 0;
        int i =0;
        try {
            BufferedReader BR = new BufferedReader(new FileReader(file));
            String input;
            while((input = BR.readLine()) != null){
                s = input.split("\t");
                reaArray[i][0] = Double.parseDouble(s[0]); reaArray[i][1] = Double.parseDouble(s[1]);
                i++;
            }
        } catch(IOException a){
            a.printStackTrace();
        }
    }
    private void initComponents () {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        mainPanelCommand = new JPanel();
        panelCommand = new JPanel();
        label34 = new JLabel();
        panel1 = new JPanel();
        label1 = new JLabel();
        testCombo = new JComboBox<>();
        panel2 = new JPanel();
        label2 = new JLabel();
        missionCombo = new JComboBox<>();
        panel3 = new JPanel();
        uav1 = new JRadioButton();
        uav2 = new JRadioButton();
        uav3 = new JRadioButton();
        uavAll = new JRadioButton();
        panel4 = new JPanel();
        hSpacer1 = new JPanel(null);
        okButton = new JButton();
        hSpacer2 = new JPanel(null);
        mainInformation = new JPanel();
        gPanel21 = new JPanel();
        label41 = new JLabel();
        itextArea1 = new JTextArea();
        label58 = new JLabel();
        itextArea18 = new JTextArea();
        label42 = new JLabel();
        itextArea2 = new JTextArea();
        label43 = new JLabel();
        itextArea3 = new JTextArea();
        label44 = new JLabel();
        itextArea4 = new JTextArea();
        label45 = new JLabel();
        itextArea5 = new JTextArea();
        label46 = new JLabel();
        itextArea6 = new JTextArea();
        label47 = new JLabel();
        itextArea7 = new JTextArea();
        label48 = new JLabel();
        itextArea8 = new JTextArea();
        label52 = new JLabel();
        itextArea9 = new JTextArea();
        label53 = new JLabel();
        itextArea10 = new JTextArea();
        label54 = new JLabel();
        itextArea11 = new JTextArea();
        label55 = new JLabel();
        itextArea12 = new JTextArea();
        label56 = new JLabel();
        itextArea13 = new JTextArea();
        label57 = new JLabel();
        itextArea14 = new JTextArea();
        mainVelocity = new JPanel();
        gPanel22 = new JPanel();
        label49 = new JLabel();
        itextArea15 = new JTextArea();
        label50 = new JLabel();
        itextArea16 = new JTextArea();
        label51 = new JLabel();
        itextArea17 = new JTextArea();
        label3 = new JLabel();

        //======== mainPanelCommand ========
        {
            mainPanelCommand.setBorder(LineBorder.createBlackLineBorder());
            mainPanelCommand.setBackground(Color.white);
            mainPanelCommand.setLayout(new GridBagLayout());
            ((GridBagLayout)mainPanelCommand.getLayout()).columnWidths = new int[] {223, 0};
            ((GridBagLayout)mainPanelCommand.getLayout()).rowHeights = new int[] {60, 40, 40, 40, 0, 0};
            ((GridBagLayout)mainPanelCommand.getLayout()).columnWeights = new double[] {0.0, 1.0E-4};
            ((GridBagLayout)mainPanelCommand.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

            //======== panelCommand ========
            {
                panelCommand.setBackground(Color.darkGray);
                panelCommand.setLayout(null);

                //---- label34 ----
                label34.setText("Query");
                label34.setHorizontalAlignment(SwingConstants.CENTER);
                label34.setFont(label34.getFont().deriveFont(label34.getFont().getStyle() | Font.BOLD, label34.getFont().getSize() + 1f));
                label34.setForeground(Color.white);
                panelCommand.add(label34);
                label34.setBounds(55, 10, 110, 45);

                { // compute preferred size
                    Dimension preferredSize = new Dimension();
                    for(int i = 0; i < panelCommand.getComponentCount(); i++) {
                        Rectangle bounds = panelCommand.getComponent(i).getBounds();
                        preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                        preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                    }
                    Insets insets = panelCommand.getInsets();
                    preferredSize.width += insets.right;
                    preferredSize.height += insets.bottom;
                    panelCommand.setMinimumSize(preferredSize);
                    panelCommand.setPreferredSize(preferredSize);
                }
            }
            mainPanelCommand.add(panelCommand, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //======== panel1 ========
            {
                panel1.setBackground(Color.white);
                panel1.setLayout(new GridBagLayout());
                ((GridBagLayout)panel1.getLayout()).columnWidths = new int[] {76, 146, 0};
                ((GridBagLayout)panel1.getLayout()).rowHeights = new int[] {0, 0};
                ((GridBagLayout)panel1.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                ((GridBagLayout)panel1.getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

                //---- label1 ----
                label1.setText("Test");
                label1.setBackground(Color.white);
                label1.setFont(new Font("\ub9d1\uc740 \uace0\ub515", Font.BOLD, 14));
                panel1.add(label1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));

                //---- testCombo ----
                testCombo.setBackground(Color.white);
                testCombo.setModel(new DefaultComboBoxModel<>(new String[] {
                    "1",
                    "2",
                    "3"
                }));
                panel1.add(testCombo, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            mainPanelCommand.add(panel1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //======== panel2 ========
            {
                panel2.setBackground(Color.white);
                panel2.setLayout(new GridBagLayout());
                ((GridBagLayout)panel2.getLayout()).columnWidths = new int[] {76, 146, 0};
                ((GridBagLayout)panel2.getLayout()).rowHeights = new int[] {0, 0};
                ((GridBagLayout)panel2.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                ((GridBagLayout)panel2.getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

                //---- label2 ----
                label2.setText("Mission");
                label2.setBackground(Color.white);
                label2.setFont(new Font("\ub9d1\uc740 \uace0\ub515", Font.BOLD, 14));
                panel2.add(label2, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));

                //---- missionCombo ----
                missionCombo.setBackground(Color.white);
                missionCombo.setModel(new DefaultComboBoxModel<>(new String[] {
                    "1",
                    "2",
                    "3"
                }));
                panel2.add(missionCombo, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            mainPanelCommand.add(panel2, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //======== panel3 ========
            {
                panel3.setBackground(Color.white);
                panel3.setLayout(new GridLayout(1, 4));

                //---- uav1 ----
                uav1.setText("UAV1");
                uav1.setBackground(Color.white);
                uav1.setHorizontalAlignment(SwingConstants.CENTER);
                uav1.setFont(new Font("\ub9d1\uc740 \uace0\ub515", Font.BOLD, 11));
                uav1.setSelected(true);
                panel3.add(uav1);

                //---- uav2 ----
                uav2.setText("UAV2");
                uav2.setBackground(Color.white);
                uav2.setHorizontalAlignment(SwingConstants.CENTER);
                uav2.setFont(new Font("\ub9d1\uc740 \uace0\ub515", Font.BOLD, 11));
                panel3.add(uav2);

                //---- uav3 ----
                uav3.setText("UAV3");
                uav3.setBackground(Color.white);
                uav3.setHorizontalAlignment(SwingConstants.CENTER);
                uav3.setFont(new Font("\ub9d1\uc740 \uace0\ub515", Font.BOLD, 11));
                panel3.add(uav3);

                //---- uavAll ----
                uavAll.setText("ALL");
                uavAll.setBackground(Color.white);
                uavAll.setHorizontalAlignment(SwingConstants.CENTER);
                uavAll.setFont(new Font("\ub9d1\uc740 \uace0\ub515", Font.BOLD, 11));
                panel3.add(uavAll);
            }
            mainPanelCommand.add(panel3, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //======== panel4 ========
            {
                panel4.setBackground(Color.white);
                panel4.setLayout(new GridLayout(1, 3));

                //---- hSpacer1 ----
                hSpacer1.setBackground(Color.white);
                panel4.add(hSpacer1);

                //---- okButton ----
                okButton.setText("OK");
                okButton.setBackground(Color.white);
                okButton.addActionListener(e -> okButtonActionPerformed(e));
                panel4.add(okButton);

                //---- hSpacer2 ----
                hSpacer2.setBackground(Color.white);
                panel4.add(hSpacer2);
            }
            mainPanelCommand.add(panel4, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }

        //======== mainInformation ========
        {
            mainInformation.setBackground(Color.white);
            mainInformation.setBorder(LineBorder.createBlackLineBorder());
            mainInformation.setLayout(null);

            //======== gPanel21 ========
            {
                gPanel21.setBackground(Color.white);
                gPanel21.setBorder(null);
                gPanel21.setLayout(new GridBagLayout());
                ((GridBagLayout)gPanel21.getLayout()).columnWidths = new int[] {137, 196, 0};
                ((GridBagLayout)gPanel21.getLayout()).rowHeights = new int[] {31, 34, 34, 34, 34, 34, 31, 32, 32, 32, 32, 32, 32, 32, 26, 0};
                ((GridBagLayout)gPanel21.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                ((GridBagLayout)gPanel21.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                //---- label41 ----
                label41.setText("UAV ID");
                label41.setFont(label41.getFont().deriveFont(label41.getFont().getSize() + 3f));
                label41.setForeground(Color.darkGray);
                label41.setHorizontalAlignment(SwingConstants.CENTER);
                label41.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label41, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea1 ----
                itextArea1.setFont(new Font("\ub9d1\uc740 \uace0\ub515", itextArea1.getFont().getStyle(), itextArea1.getFont().getSize()));
                itextArea1.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea1, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label58 ----
                label58.setText("Index");
                label58.setFont(label58.getFont().deriveFont(label58.getFont().getSize() + 3f));
                label58.setForeground(Color.darkGray);
                label58.setHorizontalAlignment(SwingConstants.CENTER);
                label58.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label58, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea18 ----
                itextArea18.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea18, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label42 ----
                label42.setText("Test Time");
                label42.setFont(label42.getFont().deriveFont(label42.getFont().getSize() + 3f));
                label42.setForeground(Color.darkGray);
                label42.setHorizontalAlignment(SwingConstants.CENTER);
                label42.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label42, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea2 ----
                itextArea2.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea2, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label43 ----
                label43.setText("Battery");
                label43.setFont(label43.getFont().deriveFont(label43.getFont().getSize() + 3f));
                label43.setForeground(Color.darkGray);
                label43.setHorizontalAlignment(SwingConstants.CENTER);
                label43.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label43, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea3 ----
                itextArea3.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea3, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label44 ----
                label44.setText("Latitude");
                label44.setFont(label44.getFont().deriveFont(label44.getFont().getSize() + 3f));
                label44.setForeground(Color.darkGray);
                label44.setHorizontalAlignment(SwingConstants.CENTER);
                label44.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label44, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea4 ----
                itextArea4.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea4, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label45 ----
                label45.setText("Longitude");
                label45.setFont(label45.getFont().deriveFont(label45.getFont().getSize() + 3f));
                label45.setForeground(Color.darkGray);
                label45.setHorizontalAlignment(SwingConstants.CENTER);
                label45.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label45, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea5 ----
                itextArea5.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea5, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label46 ----
                label46.setText("Altitude");
                label46.setFont(label46.getFont().deriveFont(label46.getFont().getSize() + 3f));
                label46.setForeground(Color.darkGray);
                label46.setHorizontalAlignment(SwingConstants.CENTER);
                label46.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label46, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea6 ----
                itextArea6.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea6, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label47 ----
                label47.setFont(label47.getFont().deriveFont(label47.getFont().getSize() + 3f));
                label47.setForeground(Color.darkGray);
                label47.setHorizontalAlignment(SwingConstants.CENTER);
                label47.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                label47.setText("Sat num");
                gPanel21.add(label47, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea7 ----
                itextArea7.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea7, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label48 ----
                label48.setText("Dop");
                label48.setFont(label48.getFont().deriveFont(label48.getFont().getSize() + 3f));
                label48.setForeground(Color.darkGray);
                label48.setHorizontalAlignment(SwingConstants.CENTER);
                label48.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label48, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea8 ----
                itextArea8.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea8, new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label52 ----
                label52.setText("Roll");
                label52.setFont(label52.getFont().deriveFont(label52.getFont().getSize() + 3f));
                label52.setForeground(Color.darkGray);
                label52.setHorizontalAlignment(SwingConstants.CENTER);
                label52.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label52, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea9 ----
                itextArea9.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea9, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label53 ----
                label53.setText("Pitch");
                label53.setFont(label53.getFont().deriveFont(label53.getFont().getSize() + 3f));
                label53.setForeground(Color.darkGray);
                label53.setHorizontalAlignment(SwingConstants.CENTER);
                label53.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label53, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea10 ----
                itextArea10.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea10, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label54 ----
                label54.setText("Yaw");
                label54.setFont(label54.getFont().deriveFont(label54.getFont().getSize() + 3f));
                label54.setForeground(Color.darkGray);
                label54.setHorizontalAlignment(SwingConstants.CENTER);
                label54.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label54, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea11 ----
                itextArea11.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea11, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label55 ----
                label55.setText("Velocity - North");
                label55.setFont(label55.getFont().deriveFont(label55.getFont().getSize() + 3f));
                label55.setForeground(Color.darkGray);
                label55.setHorizontalAlignment(SwingConstants.CENTER);
                label55.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label55, new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea12 ----
                itextArea12.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea12, new GridBagConstraints(1, 12, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label56 ----
                label56.setText("Velocity - East");
                label56.setFont(label56.getFont().deriveFont(label56.getFont().getSize() + 3f));
                label56.setForeground(Color.darkGray);
                label56.setHorizontalAlignment(SwingConstants.CENTER);
                label56.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label56, new GridBagConstraints(0, 13, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea13 ----
                itextArea13.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea13, new GridBagConstraints(1, 13, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label57 ----
                label57.setText("Velocity - Down");
                label57.setFont(label57.getFont().deriveFont(label57.getFont().getSize() + 3f));
                label57.setForeground(Color.darkGray);
                label57.setHorizontalAlignment(SwingConstants.CENTER);
                label57.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(label57, new GridBagConstraints(0, 14, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 4), 0, 0));

                //---- itextArea14 ----
                itextArea14.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel21.add(itextArea14, new GridBagConstraints(1, 14, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            mainInformation.add(gPanel21);
            gPanel21.setBounds(10, 10, 345, 480);

            { // compute preferred size
                Dimension preferredSize = new Dimension();
                for(int i = 0; i < mainInformation.getComponentCount(); i++) {
                    Rectangle bounds = mainInformation.getComponent(i).getBounds();
                    preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                    preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                }
                Insets insets = mainInformation.getInsets();
                preferredSize.width += insets.right;
                preferredSize.height += insets.bottom;
                mainInformation.setMinimumSize(preferredSize);
                mainInformation.setPreferredSize(preferredSize);
            }
        }

        //======== mainVelocity ========
        {
            mainVelocity.setBackground(Color.white);
            mainVelocity.setBorder(LineBorder.createBlackLineBorder());
            mainVelocity.setLayout(null);

            //======== gPanel22 ========
            {
                gPanel22.setBackground(Color.white);
                gPanel22.setBorder(null);
                gPanel22.setLayout(new GridBagLayout());
                ((GridBagLayout)gPanel22.getLayout()).columnWidths = new int[] {137, 197, 0};
                ((GridBagLayout)gPanel22.getLayout()).rowHeights = new int[] {31, 34, 28, 0};
                ((GridBagLayout)gPanel22.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                ((GridBagLayout)gPanel22.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                //---- label49 ----
                label49.setText("UAV 1");
                label49.setFont(label49.getFont().deriveFont(label49.getFont().getSize() + 3f));
                label49.setForeground(Color.darkGray);
                label49.setHorizontalAlignment(SwingConstants.CENTER);
                label49.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel22.add(label49, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea15 ----
                itextArea15.setFont(new Font("\ub9d1\uc740 \uace0\ub515", itextArea15.getFont().getStyle(), itextArea15.getFont().getSize()));
                itextArea15.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel22.add(itextArea15, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label50 ----
                label50.setText("UAV 2");
                label50.setFont(label50.getFont().deriveFont(label50.getFont().getSize() + 3f));
                label50.setForeground(Color.darkGray);
                label50.setHorizontalAlignment(SwingConstants.CENTER);
                label50.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel22.add(label50, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 4), 0, 0));

                //---- itextArea16 ----
                itextArea16.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel22.add(itextArea16, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 6, 0), 0, 0));

                //---- label51 ----
                label51.setText("UAV 3");
                label51.setFont(label51.getFont().deriveFont(label51.getFont().getSize() + 3f));
                label51.setForeground(Color.darkGray);
                label51.setHorizontalAlignment(SwingConstants.CENTER);
                label51.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel22.add(label51, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 4), 0, 0));

                //---- itextArea17 ----
                itextArea17.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
                gPanel22.add(itextArea17, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            mainVelocity.add(gPanel22);
            gPanel22.setBounds(10, 46, 350, 100);

            //---- label3 ----
            label3.setText("\ud3c9\uade0 \uc18d\ub3c4");
            label3.setFont(new Font("\ub9d1\uc740 \uace0\ub515", Font.BOLD, 20));
            mainVelocity.add(label3);
            label3.setBounds(new Rectangle(new Point(145, 10), label3.getPreferredSize()));

            { // compute preferred size
                Dimension preferredSize = new Dimension();
                for(int i = 0; i < mainVelocity.getComponentCount(); i++) {
                    Rectangle bounds = mainVelocity.getComponent(i).getBounds();
                    preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                    preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                }
                Insets insets = mainVelocity.getInsets();
                preferredSize.width += insets.right;
                preferredSize.height += insets.bottom;
                mainVelocity.setMinimumSize(preferredSize);
                mainVelocity.setPreferredSize(preferredSize);
            }
        }

        //---- buttonGroup1 ----
        ButtonGroup buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(uav1);
        buttonGroup1.add(uav2);
        buttonGroup1.add(uav3);
        buttonGroup1.add(uavAll);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }



    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel mainPanelCommand;
    private JPanel panelCommand;
    private JLabel label34;
    private JPanel panel1;
    private JLabel label1;
    private JComboBox<String> testCombo;
    private JPanel panel2;
    private JLabel label2;
    private JComboBox<String> missionCombo;
    private JPanel panel3;
    private JRadioButton uav1;
    private JRadioButton uav2;
    private JRadioButton uav3;
    private JRadioButton uavAll;
    private JPanel panel4;
    private JPanel hSpacer1;
    private JButton okButton;
    private JPanel hSpacer2;
    private JPanel mainInformation;
    private JPanel gPanel21;
    private JLabel label41;
    private JTextArea itextArea1;
    private JLabel label58;
    private JTextArea itextArea18;
    private JLabel label42;
    private JTextArea itextArea2;
    private JLabel label43;
    private JTextArea itextArea3;
    private JLabel label44;
    private JTextArea itextArea4;
    private JLabel label45;
    private JTextArea itextArea5;
    private JLabel label46;
    private JTextArea itextArea6;
    private JLabel label47;
    private JTextArea itextArea7;
    private JLabel label48;
    private JTextArea itextArea8;
    private JLabel label52;
    private JTextArea itextArea9;
    private JLabel label53;
    private JTextArea itextArea10;
    private JLabel label54;
    private JTextArea itextArea11;
    private JLabel label55;
    private JTextArea itextArea12;
    private JLabel label56;
    private JTextArea itextArea13;
    private JLabel label57;
    private JTextArea itextArea14;
    private JPanel mainVelocity;
    private JPanel gPanel22;
    private JLabel label49;
    private JTextArea itextArea15;
    private JLabel label50;
    private JTextArea itextArea16;
    private JLabel label51;
    private JTextArea itextArea17;
    private JLabel label3;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
