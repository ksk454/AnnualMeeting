import util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author John Olheiser
 */
public class DoorPrizePanel extends PrizeTab {

    private JLabel title = new JLabel("Door Prize Drawing");
    private JTextField prizeField = new JTextField();
    private JTextField prizeDescriptionField = new JTextField();
    private JTextField winnerField = new JTextField();
    private JButton drawNameButton = new JButton("Draw Name");
    private JLabel presentLabel = new JLabel("Is this person present?");
    private JLabel drawLabel = new JLabel("Drawing For");
    private JLabel prizeDescriptionLabel = new JLabel("Description: ");
    private JButton drawPrizeButton = new JButton("Draw Prize");
    private WinnersTableModel winnersTableModel;
    private JTable winnersTable = new JTable(winnersTableModel);
    private static final int MIN_VALUE = 1;
    private JButton yesButton = new JButton("Yes");
    private JButton noButton = new JButton("No");
    private int prizeID;
    private String prize;
    private String prizeDescription;
    private int winnerID;
    private String winner;

    JPanel buttonPanel = new JPanel(new FlowLayout());

    public DoorPrizePanel() {
        setLayout(new BorderLayout());
        setProperties();

        //layout stuff
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(drawPrizeButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        mainPanel.add(drawLabel);
        mainPanel.add(prizeField);
        mainPanel.add(prizeDescriptionLabel);
        mainPanel.add(prizeDescriptionField);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        mainPanel.add(drawNameButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        mainPanel.add(winnerField);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        mainPanel.add(presentLabel);
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createVerticalGlue());

        add(title,BorderLayout.NORTH);
        add(mainPanel,BorderLayout.CENTER);
        add(new JScrollPane(winnersTable), BorderLayout.WEST);

        //button listeners
        drawPrizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                drawRandomPrize();
            }
        });
        drawNameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                drawRandomName();
            }
        });
        yesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setWinner();
            }
        });
        noButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                drawAnotherName();
            }
        });
    }

    private void drawRandomPrize(){
        //queries the DB for prizes that have not been won yet
        ResultSet rs = DatabaseManager.getInstance().doQuery("SELECT * FROM CEC.DOOR_PRIZES WHERE WON_BY IS NULL " +
                                                             "OR WON_BY = 0");

        //get the size of the resultset of the prizes
        int size = 0;
        try {
            rs.last();
            size = rs.getRow();
            rs.beforeFirst();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        if (size <= 0){
            JOptionPane.showMessageDialog(null, "All of the available prizes have been won by someone already.\n" +
                                           "Please add more prizes.", "No Prizes Available", JOptionPane.ERROR_MESSAGE);
            loadTab();
            return;
        }
        //now that we have the size we can get a random number in that range
        int randomNumber = MIN_VALUE + (int)(Math.random() * ((size - MIN_VALUE) + 1));

        //get the row in that random number and populate what we need
        try {
            rs.absolute(randomNumber);
            prizeID = rs.getInt("PRIZE_ID");
            prize = rs.getString("PRIZE");
            prizeDescription = rs.getString("DESCRIPTION");
            prizeField.setText(prize);
            prizeDescriptionField.setText(prizeDescription);

        } catch (SQLException e) {
            new PrizeExceptionHandler().caughtException(e);
        }
        drawNameButton.setEnabled(true);
        drawPrizeButton.setEnabled(false);
    }

    /**
     * This method fetches all the registered members from the database who have not won yet and are NOT employees
     * It then selects a random member and sets the winning information of that member
     */
    private void drawRandomName(){
        //queries the DB for prizes that have not been won yet
        ResultSet rs = DatabaseManager.getInstance().doQuery("SELECT REG.ACCOUNT, REG.NAME, REG.ADDRESS " +
                "FROM CEC.REGISTERED_MEMBER REG " +
                " LEFT OUTER JOIN CEC.CEC_EMPLOYEE EMP " +
                " ON REG.ACCOUNT = EMP.ACCOUNT " +
                " WHERE EMP.ACCOUNT IS NULL AND (REG.HAS_WON = 0 OR REG.HAS_WON IS NULL)");

        //get the size of the resultset of the prizes
        int size = 0;
        try {
            rs.last();
            size = rs.getRow();
            rs.beforeFirst();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }

        //now that we have the size we can get a random number in that range
        int randomNumber = MIN_VALUE + (int)(Math.random() * ((size - MIN_VALUE) + 1));

        //get the row in that random number and populate what we need
        try {
            rs.absolute(randomNumber);
            winnerID = rs.getInt("ACCOUNT");
            winner = rs.getString("NAME");
            winnerField.setText(winner);
        } catch (SQLException e) {
            new PrizeExceptionHandler().caughtException(e);
        }
        yesButton.setEnabled(true);
        noButton.setEnabled(true);
        drawNameButton.setEnabled(false);
    }

    private void setWinner(){
        //check that there is something in winner and winnerID
        if (winner.isEmpty() || winner == null || winnerID <= 0){
            JOptionPane.showMessageDialog(null, "The winner cannot be empty.", "Winner is empty", JOptionPane.ERROR_MESSAGE);
            loadTab();
            return;
        }

        //check to see if there is something in prize and prizeID
        if (prize.isEmpty() || prize == null || prizeID <= 0){
            JOptionPane.showMessageDialog(null, "The prize cannot be empty.", "Prize is empty", JOptionPane.ERROR_MESSAGE);
            loadTab();
            return;
        }
        try {
            String updateDoorPrizesQuery = "UPDATE CEC.DOOR_PRIZES SET WON_BY = " + winnerID + " WHERE PRIZE_ID = " + prizeID;
            String updateMembersQuery = "UPDATE CEC.REGISTERED_MEMBER SET HAS_WON = 1, IS_PRESENT = 1 WHERE ACCOUNT = " + winnerID;
            DatabaseManager.getInstance().doNoResultsQuery(updateDoorPrizesQuery);
            DatabaseManager.getInstance().doNoResultsQuery(updateMembersQuery);
            JOptionPane.showMessageDialog(null, winner + "\nCongratulations!", "The winner of " + prize + " is:",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception e){
            new PrizeExceptionHandler().caughtException(e);
        }
        loadTab();
    }

    private void drawAnotherName(){
        String updateMembersQuery = "UPDATE CEC.REGISTERED_MEMBER SET HAS_WON = 1, IS_PRESENT = 0 WHERE ACCOUNT = " + winnerID;
        DatabaseManager.getInstance().doNoResultsQuery(updateMembersQuery);
        JOptionPane.showMessageDialog(null, "Please draw another name", "No Winner", JOptionPane.INFORMATION_MESSAGE);
        winner = "";
        winnerID = 0;
        winnerField.setText(winner);
        drawNameButton.setEnabled(true);
        yesButton.setEnabled(false);
        noButton.setEnabled(false);
    }

    public void loadTab(){
        //updates the table and prize/winner fields and variables
        winnersTableModel = new WinnersTableModel();
        winnersTable.setModel(winnersTableModel);
        drawPrizeButton.setEnabled(true);
        winner = "";
        winnerID = 0;
        winnerField.setText("");
        prize = "";
        prizeID = 0;
        prizeField.setText("");
        prizeDescription = "";
        prizeDescriptionField.setText("");
        drawNameButton.setEnabled(false);
        yesButton.setEnabled(false);
        noButton.setEnabled(false);
    }

    private void setProperties() {
        title.setFont(PrizeUtil.getTitleFont());

        winnerField.setEditable(false);
        prizeField.setEditable(false);
        prizeDescriptionField.setEditable(false);

        winnerField.setMaximumSize(new Dimension(200, 30));
        prizeField.setMaximumSize(new Dimension(200, 30));
        prizeDescriptionField.setMaximumSize(new Dimension(200, 30));

        drawNameButton.setEnabled(false);
        yesButton.setEnabled(false);
        noButton.setEnabled(false);
        alignXs();
    }

    private void alignXs() {
        drawPrizeButton.setAlignmentX(CENTER_ALIGNMENT);
        drawLabel.setAlignmentX(CENTER_ALIGNMENT);
        prizeField.setAlignmentX(CENTER_ALIGNMENT);
        prizeDescriptionLabel.setAlignmentX(CENTER_ALIGNMENT);
        prizeDescriptionField.setAlignmentX(CENTER_ALIGNMENT);
        drawNameButton.setAlignmentX(CENTER_ALIGNMENT);
        winnerField.setAlignmentX(CENTER_ALIGNMENT);
        presentLabel.setAlignmentX(CENTER_ALIGNMENT);
        buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
    }

    protected class WinnersTableModel extends PrizeTableModel {
        public WinnersTableModel() {
            super("SELECT " +
                    "PRIZE, DESCRIPTION, WON_BY, NAME " +
                    "FROM CEC.DOOR_PRIZES dp " +
                    "JOIN CEC.CEC_MEMBER cm " +
                    "ON dp.WON_BY = cm.ACCOUNT " +
                    "WHERE dp.WON_BY IS NOT NULL",
                    new String[]{"Prize", "Description", "Winner Name"},
                    new String[]{"PRIZE", "DESCRIPTION", "NAME"});
        }
    }
}
