import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class ViewPlaying extends ViewGame{
    final int buttonH=super.getButtonHeight();//hauteur d un bouton
    final JLabel errorName=new JLabel("Please enter a valid name :");
    final Level level;//niveau en cours
    private PanelBlackBoard blackBoard;//patron + visualisation du resultat du code
    private PanelDragDropBoard dragDrop;//fusion de WhiteBoard et CommandBoard
    private JPanel features=new JPanel(new GridBagLayout());//panel avec tous les boutons sous BlackBoard
    private JPanel topFeatures=new JPanel(new GridLayout());//panel avec tous les boutons au-dessus de BlackBoard
    private CustomJButton run, stop, reset;
    private JButton submit=new JButton("Submit");
    private boolean canSubmit=false;
    private Timer timer=new Timer(30, null);//vitesse par defaut
    private JSlider slider=new JSlider();//regulation de la vitesse
    private PanelDragDropBoard.Command runC;//commande a executer -> permettre passage au suivant dans lambda expression
    private JProgressBar limite;
    private HashMap<String, Integer> variables=new HashMap<String, Integer>();//nom, valeur des variables globales
    private JPanel varPanel=new JPanel(new WrapLayout(WrapLayout.CENTER, 20, 10));
    
    ViewPlaying(Control control, Player player, boolean isCreating) throws IOException{
        super(control, player);
        this.level=player.getLevel();
        errorName.setForeground(Color.RED);
        addBoard();//ajout des tableaux, avec des marges de 20 (haut, bas et entre tableaux)
        addFeatures(isCreating, player.username.equals("GM"));//ajout des fonctionnalites
        addTopFeatures();//ajout des boutons en rapport avec blackBoard
        if(level.functions!=null) dragDrop.loadCode(level.functions, false);
        if(level.mainCode!=null){
            dragDrop.loadCode(level.mainCode, true);
            limite.setValue(dragDrop.getNumberFromHead());
        }
        changeButtonColor(this, control.darkModeOn());
        this.getRootPane().setDefaultButton(run);
    }
    
    void addBoard() throws IOException{
        blackBoard=new PanelBlackBoard();
        this.add(blackBoard);//taille fixee
        dragDrop=new PanelDragDropBoard();
        this.add(dragDrop);//taille relative a l ecran
    }
    
    void addTopFeatures() throws IOException{
        topFeatures.setBounds(20, 20+buttonH, blackBoard.getWidth(),buttonH);
        this.add(topFeatures);
        
        JButton seeGrid=new JButton("Hide grid");//voir la grille
        seeGrid.addActionListener((event)->{
            blackBoard.gridApparent=!blackBoard.gridApparent;
            seeGrid.setText(blackBoard.gridApparent?"Hide grid":"Show grid");
            blackBoard.repaint();
        });
        
        CustomJButton sizeS=new CustomJButton("", ImageIO.read(new File("images/sizeS.png")));
        CustomJButton sizeM=new CustomJButton("", ImageIO.read(new File("images/sizeM.png")));
        CustomJButton sizeL=new CustomJButton("", ImageIO.read(new File("images/sizeL.png")));
        sizeS.setPreferredSize(new Dimension(buttonH, buttonH));
        sizeM.setPreferredSize(new Dimension(buttonH, buttonH));
        sizeL.setPreferredSize(new Dimension(buttonH, buttonH));
        
        sizeS.setEnabled(false);
        sizeS.addActionListener((event)->sizeSM(400, sizeS, sizeM, sizeL));
        sizeM.addActionListener((event)->sizeSM(600, sizeS, sizeM, sizeL));
        sizeL.addActionListener((event)->{
            dragDrop.setVisible(false);
            int size=Math.min(widthFS-features.getWidth(), heightFS-buttonH*2-40);
            blackBoard.setBounds(20, 20+buttonH*2, size, size);
            topFeatures.setBounds(20, 20+buttonH, size, buttonH);
            features.setBounds(size+60, blackBoard.getY(), widthFS-size-100, features.getHeight());
            varPanel.setBounds(size+60, blackBoard.getY()+features.getHeight()+40,
                features.getWidth(), heightFS-blackBoard.getY()-features.getHeight()-60);
            varPanel.setVisible(varPanel.getHeight()>50);
            sizeS.setEnabled(true);
            sizeM.setEnabled(true);
            sizeL.setEnabled(false);
            SwingUtilities.updateComponentTreeUI(this);
        });
        
        JPanel left=new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));//hide/show grid
        left.add(seeGrid);
        topFeatures.add(left);
        
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.add((sizeS));
        if(860+dragDrop.width/2<widthFS && 660+buttonH*2+features.getHeight()<heightFS)
            right.add((sizeM));//CommandBoard manipulable et features visible entierement
        right.add((sizeL));
        topFeatures.add(right);
    }
    
    void sizeSM(int n, JButton sizeS, JButton sizeM, JButton sizeL){
        dragDrop.setVisible(true);
        dragDrop.setBounds(n+40, dragDrop.getY(), widthFS-n-60, dragDrop.getHeight());
        blackBoard.setBounds(20, 20+buttonH*2, n, n);
        topFeatures.setBounds(20, 20+buttonH, n, buttonH);
        features.setBounds(20, n+40+buttonH*2, n, features.getHeight());
        varPanel.setBounds(20, n+60+buttonH*2+features.getHeight(),
            n, heightFS-n-80-buttonH*2-features.getHeight());
        varPanel.setVisible(varPanel.getHeight()>50);
        sizeS.setEnabled(n!=400);
        sizeM.setEnabled(n!=600);
        sizeL.setEnabled(true);
        SwingUtilities.updateComponentTreeUI(this);
    }
    
    void addFeatures(boolean isCreating, boolean isGM) throws IOException{
        int featuresH=50;//hauteur de features
        GridBagConstraints c=new GridBagConstraints();
        
        //1ere ligne avec slider de vitesse + boutons run/stop/reset
        Hashtable<Integer, JLabel> labels=new Hashtable<>();
        JLabel g=new JLabel("Slower");
        g.setForeground(Color.WHITE);
        labels.put(0, g);
        JLabel d=new JLabel("Faster");
        d.setForeground(Color.WHITE);
        labels.put(100, d);
        slider.setBackground(new Color(0, 0, 128));
        slider.setLabelTable(labels);
        slider.setPaintLabels(true);
        slider.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e){
                JSlider source=(JSlider)e.getSource();
                if(!source.getValueIsAdjusting()){
                    int speed=(int)source.getValue()+1;
                    if(speed==100) ViewPlaying.this.timer.setDelay(0);//ne saute pas vraiment l'animation...
                    else if(speed>50)//pour qu'on puisse voir un peu plus la difference
                        ViewPlaying.this.timer.setDelay(1000/speed);
                    else ViewPlaying.this.timer.setDelay(1500/speed);
                }
            }
        });
        run=new CustomJButton("",ImageIO.read(new File("images/run.png")));
        stop=new CustomJButton("", ImageIO.read(new File("images/stop.png")));
        reset=new CustomJButton("", ImageIO.read(new File("images/reset.png")));
        run.setPreferredSize(new Dimension(41, 41));
        stop.setPreferredSize(new Dimension(41, 41));
        reset.setPreferredSize(new Dimension(41, 41));
        run.addActionListener((event)->run());
        stop.addActionListener((event)->stop());
        reset.addActionListener((event)->reset());
        stop.setVisible(false);//apparait apres avoir clique sur run
        reset.setVisible(false);//idem, pour stop
        
        c.insets=new Insets(0, 20, 15, 20);
        features.add(slider, c);
        features.add(run, c);
        features.add(stop, c);
        features.add(reset, c);
        featuresH+=Math.max(slider.getPreferredSize().height, 41);
        
        //2e ligne avec soit progressBar, soit submit (ou checkBox)
        c.gridwidth=2;
        c.insets=new Insets(15, 10, 0, 10);
        if(isCreating){//creer un niveau -> que pour la page Create
            JCheckBox saveCode=new JCheckBox("Save main code");
            JCheckBox saveFun=new JCheckBox("Save functions");
            submit.addActionListener((event)->{
                if(level.getPlayerDraw().isEmpty()) return;//on ne sauvegarde pas de dessin vide
                String name=JOptionPane.showInputDialog(null, "Level's name ?", "Submit level", JOptionPane.QUESTION_MESSAGE);
                while(name!=null && (name.equals("") || !name.matches("^[a-zA-Z0-9]*$")))
                    name=JOptionPane.showInputDialog(null, errorName, "Submit level", JOptionPane.QUESTION_MESSAGE);
                if(name!=null){//choix de la destination dans l arborescence
                    String dest="challenge/";
                    if(isGM){
                        File[] arrayLevels=nombreNiveau("levels/training/");
                        JComboBox choice=new JComboBox();
                        for(int i=0; i<arrayLevels.length; i++)
                            choice.addItem("training/"+arrayLevels[i].getName().substring(0, arrayLevels[i].getName().length())+"/");
                        choice.addItem("challenge/");
                        JOptionPane.showOptionDialog(null, choice, "Destination of level", JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null, null, null);
                        dest=choice.getItemAt(choice.getSelectedIndex()).toString();
                    }
                    control.submit(name, !dest.equals("challenge/"), level, saveCode.isSelected()?dragDrop.convertStart():null,
                        saveFun.isSelected()?dragDrop.convertFunctions():null, dest, blackBoard.getHeight());
                }
            });
            c.gridy=2;
            features.add(submit, c);
            submit.setEnabled(false);
            featuresH+=30+buttonH;
            if(isGM){
                saveCode.setBackground(new Color(0, 0, 128));
                saveCode.setForeground(Color.WHITE);
                saveFun.setBackground(new Color(0, 0, 128));
                saveFun.setForeground(Color.WHITE);
                c.gridwidth=1;
                c.gridy=1;
                c.anchor=GridBagConstraints.LINE_START;
                features.add(saveCode, c);
                features.add(saveFun, c);
                featuresH+=15+saveCode.getPreferredSize().height;
            }
        }
        else{//limite des commandes si on est dans un niveau
            limite=new JProgressBar(0, level.numberOfCommands){
                public String getString(){//presentation apparente
                    return getValue()+"/"+level.numberOfCommands;
                }
            };
            limite.setStringPainted(true);
            limite.setForeground(Color.DARK_GRAY);
            c.gridy=1;
            features.add(limite, c);
            featuresH+=30+limite.getPreferredSize().height;
        }
        
        features.setBounds(20, 440+buttonH*2, 400, featuresH);
        features.setBackground(new Color(0, 0, 128));
        this.add(features);
        
        varPanel.setBounds(20, 460+buttonH*2+featuresH, 400, heightFS-480-buttonH*2-featuresH);
        varPanel.setBorder(BorderFactory.createLineBorder(Color.MAGENTA.darker(), 3));
        varPanel.setVisible(varPanel.getHeight()>50);
        this.add(varPanel);
    }
    
    void run(){
        run.setVisible(false);
        stop.setVisible(true);
        this.getRootPane().setDefaultButton(stop);
        runC=dragDrop.commands.getFirst();
        runC=runC.canExecute(variables)?runC.next:null;
        canSubmit=true;
        ActionListener taskPerformed=new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(runC!=null) runC=runC.execute(variables);
                else{
                    stop();//arret automatique
                    victoryMessage();
                }
            }
        };
        timer.addActionListener(taskPerformed);
        timer.start();
    }
    
    void stop(){
        timer.stop();
        stop.setVisible(false);
        reset.setVisible(true);
        this.getRootPane().setDefaultButton(reset);
        submit.setEnabled(canSubmit);
        for(ActionListener action : timer.getActionListeners()) timer.removeActionListener(action);
    }
    
    void reset(){
        level.initializePlayerDraw();//vide le dessin du joueur
        blackBoard.brush.resetBrush();//remet le pinceau a l'emplacement initial
        if(!variables.isEmpty()){
            for(String key : variables.keySet()) variables.replace(key, 0);
        }
        for(PanelDragDropBoard.Command c : dragDrop.commands) c.reset();
        repaint();
        reset.setVisible(false);
        run.setVisible(true);
        this.getRootPane().setDefaultButton(run);
    }
            
    void updateVariableDisplay(){
        for(Component c : varPanel.getComponents()){
            if(c instanceof VariablePanel)
                ((VariablePanel)c).setValue(variables.get(((VariablePanel)c).getVarName()));
        }
        SwingUtilities.updateComponentTreeUI(features);
    }
    
    void victoryMessage(){
    	if(!level.compare()) return;
        Clip clip=null;
        boolean musicOn=control.musicIsActive();
        try{
            if(musicOn) control.musicChangeState();
            AudioInputStream audio=AudioSystem.getAudioInputStream(new File("sounds/tuturu.wav"));
            clip=AudioSystem.getClip();
            clip.open(audio);
            clip.start();
        }
        catch(Exception e){}
        control.win(getNumberOfDirectory(level.name), level.name);
        int retour=JOptionPane.showConfirmDialog(null, "Come back to summary ?", "Victory", JOptionPane.YES_NO_OPTION);
        if(retour==JOptionPane.OK_OPTION){
            if(level.isTraining) control.switchTraining();
            else control.switchChallenge();
        }
        if(clip!=null){//arrete le son de victoire et remet la musique de fond
            clip.stop();
            if(musicOn) control.musicChangeState();
        }
    }
    
    String[] getCommandsArray(){
        return dragDrop.listToTab(dragDrop.getCommands());
    }
    
    int[] getNumbers(){//commandes, fonctions, variables
        int[] res={dragDrop.getNumberFromHead(), dragDrop.getNumberFunction(),
            dragDrop.getNumberFunctionInt(), variables.size()};
        return res;
    }
    
    int getNumberOfDirectory(String name){
        if(level.isTraining){
            File[] arrayLevels=nombreNiveau("levels/training/");
            for(int i=0; i<arrayLevels.length; i++){
                File[] arrayLevels2=nombreNiveau("levels/training/"+arrayLevels[i].getName());
                for(int j=0; j<arrayLevels2.length; j++){
                    String str=arrayLevels2[j].getName().substring(0, arrayLevels2[j].getName().length()-4);
                    if(str.equals(level.name)) return i;
                }
            }
        }
        return -1;//pas un training
    }

    boolean inWhiteBoard(Component c){//est dans whiteBoard
        Point p=c.getLocation();
        return p.x+c.getWidth()>0 && p.x<dragDrop.width/2 && p.y+c.getHeight()>0 && p.y<dragDrop.height;
    }
    
    
    /**********************  Classes internes  **********************/
    
    /***********************
    *      BlackBoard      *
    ***********************/
    
    public class PanelBlackBoard extends JPanel{
        protected boolean gridApparent=true;//par defaut, on voit la grille
        private Brush brush=new Brush();//fleche vide
        private int x, y, angle, size;//par defaut (0,0) et orientee "->" (angle 0° sur le cercle trigo)
        private Color brushColor;
        private boolean drawing=true, brush2;//pinceau posé par defaut, 2e pinceau de symetrie

        PanelBlackBoard(){
            this.setBounds(20, 20+buttonH*2, 400, 400);//marge gauche=20, 20+hauteur d un bouton en haut, taille 400*400
            this.setBackground(Color.BLACK);//fond noir
            this.x=level.brushX;
            this.y=level.brushY;
            this.angle=level.brushAngle;
            this.brushColor=level.brushFirstColor;
            this.size=400;
        }
        
        public void setBounds(int x, int y, int w, int h){
            super.setBounds(x, y, w, h);
            this.size=w;
        }
        
        
        /***** Drawing & Paint *****/

        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if(gridApparent) paintGrid(g);//grille apparente quand on le souhaite
            if(brush2) paintSymmetry(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setStroke(new BasicStroke(4));
            for(Vector v : level.pattern) paintVector(g2, v, true);//patron
            for(Vector v : level.getPlayerDraw()) paintVector(g2, v, false);//dessin du joueur, au debut vide
            paintBrush(g2, x, y, angle, brushColor);//pinceau en dernier car rotation
        }

        void paintGrid(Graphics g){//dessin de la grille
            g.setColor(Color.gray.darker());
            for(int i=1; i<4; i++){
                int j=i*size/4;
                g.drawLine(j, 20, j, size);
                g.drawLine(30, j, size, j);
                g.drawString(Integer.toString(i*100), j-10, 15);
                g.drawString(Integer.toString(i*100), 3, j+5);
            }
            g.drawString("0", 5, 15);
        }
        
        void paintSymmetry(Graphics g){
            g.setColor(Color.RED.darker());
            g.drawLine(size/2, 20, size/2, size);
        }
        
        void paintVector(Graphics2D g2, Vector v, boolean darker){
            if(darker) g2.setColor(v.color.darker());
            else g2.setColor(v.color);
            if(v instanceof Vector.VectorLine){
                Vector.VectorLine tmp=(Vector.VectorLine)v;
                g2.drawLine(tmp.x1*size/400, tmp.y1*size/400, tmp.x2*size/400, tmp.y2*size/400);
                return;
            }
            Vector.VectorArc tmp=(Vector.VectorArc)v;
            g2.drawArc(tmp.x1*size/400, tmp.y1*size/400, tmp.diameter*size/400, tmp.diameter*size/400, tmp.startAngle, tmp.scanAngle);
        }
        
        void paintBrush(Graphics2D g2, int x, int y, int angle, Color color){//dessin du pinceau
            g2.translate(x*size/400, y*size/400);
            g2.rotate(Math.toRadians(-angle), 0, 0);//tourne autour du point initial
            g2.setColor(color);
            g2.draw(brush);
        }
        
        
        /************************
        *      Brush Class      *
        ************************/
        
        private class Brush extends Path2D.Double{//pinceau
            Brush(){//fleche dans le coin superieur gauche de blackBoard
            	this.resetBrush();
            }
            
            void resetBrush(){
                PanelBlackBoard.this.x=level.brushX;
                PanelBlackBoard.this.y=level.brushY;
                PanelBlackBoard.this.angle=level.brushAngle;
                PanelBlackBoard.this.brushColor=level.brushFirstColor;
                PanelBlackBoard.this.drawing=true;
                PanelBlackBoard.this.brush2=false;//pas de symetrie
            	moveTo(-18,-5);
            	lineTo(2,-5);
            	lineTo(1,-12);
            	lineTo(18,0);
            	lineTo(1,12);
            	lineTo(2,5);
            	lineTo(-18,5);
            	lineTo(-18,-5);
            }
        }//fin classe interne interne Brush
    }//fin classe interne PanelBlackBoard
    
    
    /************************
    *     DragDropBoard     *
    ************************/
    
    public class PanelDragDropBoard extends JPanel implements MouseWheelListener{
        final int y00, width, height;//position initiale, largeur, hauteur du panel
        private LinkedList<Command> commands=new LinkedList<Command>();//commandes ayant ete drag sur whiteBoard
        private Command brightC;//commande allumee
        private LinkedList<NumberField> fields=new LinkedList<NumberField>();
        private NumberField brightF;//field allume
        private int lastPositionY=20, deltaY=0, nbOfFunV=0, nbOfFunI=0;//positionY libre, mouvement de la roulette
        private Bin bin;
        final Border borderV=BorderFactory.createLineBorder(new Color(255, 204, 255), 2);
        
        PanelDragDropBoard() throws IOException{
            this.y00=20+buttonH+ViewPlaying.this.getInsets().top;
            width=widthFS-460;//460=3 marges de colonne + taille blackBoard
            height=heightFS-40-buttonH;//40=marges haut+bas
            this.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
            this.setLayout(null); 
            this.setBounds(440, y00-ViewPlaying.this.getInsets().top, width, height);
            this.addMouseWheelListener(this);
            
            this.bin=new Bin();
            this.add(bin);
            if(level.numberOfVariables!=0) setVariableButton();
            this.setFunctionButton();
            this.addAvailableCommands();
        }
        
        int countConvert(Command c){
            int res=0;
            while(c!=null){
                res++;
                c=c.next;
            }
            return res;
        }
        
        String[] convertStart(){
            Command tmp=commands.getFirst().next;
            int size=countConvert(tmp);
            String[] res=new String[size];
            size=0;
            while(tmp!=null){
                res[size++]=(tmp instanceof CommandFunctionCall)?
                    ((CommandFunctionCall)tmp).name.getText():tmp.name;
                tmp=tmp.next;
            }
            return res;
        }
        
        String[] convertFunctions(){
            int size=0;
            for(Command c : commands){//ne save pas FunctionInitInt
                if(c.getClass()==CommandFunctionInit.class) size+=countConvert(c)+1;
            }
            String[] res=new String[size];
            size=0;
            for(Command c : commands){
                if(c.getClass()==CommandFunctionInit.class){
                    res[size++]="";//nouvelle fonction
                    res[size++]=((CommandFunctionInit)c).nameFunction.getText();
                    Command tmp=((CommandFunctionInit)c).next;
                    while(tmp!=null){
                        res[size++]=(tmp instanceof CommandFunctionCall)?
                            ((CommandFunctionCall)tmp).name.getText():tmp.name;
                        tmp=tmp.next;
                    }
                }
            }
            return res;
        }
        
        void loadCode(String[] toLoad, boolean isStart){
            LinkedList<Command> saveLast=new LinkedList<Command>();//celui qui doit etre lie
            saveLast.add(commands.getFirst());
            LinkedList<Command> heads=new LinkedList<Command>();//tetes de fonction/Start
            if(isStart) heads.add(commands.getFirst());
            else initializeAllFunction(toLoad, heads);//avant de remplir interieur des fonctions
            for(int i=0; i<toLoad.length; i++){
                Command last=saveLast.removeLast();
                if(toLoad[i].equals("")){//nouvelle fonction
                    CommandFunctionInit tmp=nameFUsed(toLoad[++i]);
                    saveLast.add(tmp.hookH);
                    saveLast.add(tmp);
                }
                else if(toLoad[i].equals("hookHorizontal")){
                    saveLast.getLast().previous=last;
                    last.next=saveLast.getLast();
                }
                else addLoadCode(toLoad[i], saveLast, last);
            }
            for(Command c : heads){//placer correctement
                if(c.previous!=null) c.previous.next=null;
                c.previous=null;
                c.updateHookVRec(c.getTmpCwc());//met a jour les hookV
                c.next.updateAllLocation();//accroche correctement
            }
        }
        
        void initializeAllFunction(String[] toLoad, LinkedList<Command> heads){//avant de remplir interieur des fonctions
            for(int i=0; i<toLoad.length; i++){
                if(toLoad[i].equals("")){//""==seperatif entre les fonctions
                    CommandFunctionInit c=(CommandFunctionInit)addLoad(toLoad[++i]);
                    lastPositionY+=addCommandCall(c, lastPositionY);
                    heads.add(c);
                    this.add(c);
                    commands.add(c);
                    this.add(c.hookH);
                    this.add(c.hookV);
                }
            }
        }
        
        void addLoadCode(String toLoad, LinkedList<Command> saveLast, Command last){
            Command toAdd=addLoad(toLoad);
            this.add(toAdd);//visible
            toAdd.newDrag(false);
            if(toAdd instanceof CommandWithCommands){
                CommandWithCommands cwc=(CommandWithCommands)toAdd;
                this.add(cwc.hookV);
                this.add(cwc.hookH);
                if(!(cwc instanceof CommandFunctionInit)) commands.add(cwc.hookH);
                else{//ajout du call de toAdd dans mainCode
                    toAdd=new CommandFunctionCall((CommandFunctionInit)toAdd, 0, 0);
                    this.add(toAdd);
                    commands.add(toAdd);
                }
            }
            toAdd.previous=last;//liens
            last.next=toAdd;
            if(last instanceof CommandWithCommands) saveLast.addLast(((CommandWithCommands)last).hookH);
            saveLast.addLast(toAdd);//prochain qui sera lie
        }
        
        void setVariableButton(){
            JButton createV=new JButton("Create a new variable");
            JButton removeV=new JButton("Remove one variable");
            Dimension dC=createV.getPreferredSize();
            Dimension dR=removeV.getPreferredSize();
            createV.setBounds(width/2-20-dC.width, 20, dC.width, dC.height);
            removeV.setBounds(createV.getX(), 30+dR.height, dR.width, dR.height);

            createV.addActionListener((event)->{
                String name=JOptionPane.showInputDialog(null, "Name of this variable ?", "Create variable", JOptionPane.QUESTION_MESSAGE);
                while(name!=null && ((name.equals("") || !name.matches("^[a-zA-Z]*$") || name.equals("x")
                || name.equals("y") || name.equals("angle") || variables.containsKey(name)) || name.length()>10))
                    name=JOptionPane.showInputDialog(null, errorName, "Create variable", JOptionPane.QUESTION_MESSAGE);
                if(name!=null){
                    addVariable(name, null);
                    removeV.setEnabled(true);
                }
                if(variables.size()==level.numberOfVariables){//max atteint
                    createV.setEnabled(false);
                    createV.setBackground(Color.RED.darker());
                }
            });
            removeV.addActionListener((event)->{
                Object[] choice=new String[variables.size()];
                int i=0;
                for(String name : variables.keySet()) choice[i++]=name;
                Object name=JOptionPane.showInputDialog(null, "Name of this variable ?",
                    "Delete variable", JOptionPane.QUESTION_MESSAGE, null, choice, choice[0]);
                if(name==null) return;//bouton annuler
                removeVariable(name.toString());
                if(variables.size()+1==level.numberOfVariables){//etait au max
                    createV.setEnabled(true);
                    createV.setBackground(null);
                }
                removeV.setEnabled(!variables.isEmpty());//plus de variable
            });
            this.add(createV);
            this.add(removeV);
            removeV.setEnabled(false);
        }
        
        void setFunctionButton(){
            if(level.numberOfFunctions!=0 && level.mainCode==null && level.functions==null){//fonctions deja initialisees
                JButton createF=new JButton("Create a new function");
                Dimension dF=createF.getPreferredSize();
                createF.setBounds(width/2-20-dF.width, 50+2*dF.height, dF.width, dF.height);
                createF.addActionListener((event)->{
                    String name=JOptionPane.showInputDialog(null, "Name of this fonction ?", "Create function", JOptionPane.QUESTION_MESSAGE);
                    while(name!=null && (name.equals("") || String.valueOf(name.charAt(0)).matches("[0-9]") || nameFUsed(name)!=null || !name.matches("^[a-zA-Z0-9]*$")))
                        name=JOptionPane.showInputDialog(null, errorName, "Create function", JOptionPane.QUESTION_MESSAGE);
                    if(name!=null) addFunction(name, width/2-20-dF.width, 70+4*dF.height, false);
                    if(nbOfFunV==level.numberOfFunctions){//max atteint
                        createF.setEnabled(false);
                        createF.setBackground(Color.RED.darker());
                    }
                });
                this.add(createF);
            }
            if(level.numberOfFunctionsInt!=0){
                JButton createF=new JButton("Create a function with return");
                Dimension dF=createF.getPreferredSize();
                createF.setBounds(width/2-20-dF.width, 60+3*dF.height, dF.width, dF.height);
                createF.addActionListener((event)->{
                    String name=JOptionPane.showInputDialog(null, "Name of this fonction ?", "Create function with return", JOptionPane.QUESTION_MESSAGE);
                    while(name!=null && (name.equals("") || String.valueOf(name.charAt(0)).matches("[0-9]") || nameFUsed(name)!=null || !name.matches("^[a-zA-Z0-9]*$")))
                        name=JOptionPane.showInputDialog(null, errorName, "Create function with return", JOptionPane.QUESTION_MESSAGE);
                    if(name!=null) addFunction(name, width/2-20-dF.width, 70+4*dF.height, true);
                    if(nbOfFunI==level.numberOfFunctionsInt){//max atteint
                        createF.setEnabled(false);
                        createF.setBackground(Color.RED.darker());
                    }
                });
                this.add(createF);
            }
        }
        
        CommandFunctionInit nameFUsed(String name){
            name="  "+name+"  ";//comme dans les labels
            for(Command c : commands){
                if(c instanceof CommandFunctionInit  &&
                    ((CommandFunctionInit)c).nameFunction.getText().equals(name)) return (CommandFunctionInit)c;
            }
            return null;
        }

        public void mouseWheelMoved(MouseWheelEvent e){
            int addToY=(e.getWheelRotation()<0?1:-1)*e.getScrollAmount()*10;
            if(e.getX()<this.getX()+width/2-getX()){//whiteBoard
                for(Component c : ViewPlaying.this.dragDrop.getComponents()){
                    if(c instanceof Command && commands.contains(c) && !(c instanceof HookHorizontal)
                    || (c instanceof Variable && !((Variable)c).lastCreated))
                        c.setLocation(c.getX(), c.getY()+addToY);
                }
            }
            else{//commandBoard
                deltaY+=addToY;
                for(Component c : ViewPlaying.this.dragDrop.getComponents()){
                    if(c instanceof Command && !commands.contains(c) && !(c instanceof HookHorizontal)
                    || (c instanceof Variable && ((Variable)c).lastCreated))
                        c.setLocation(c.getX(), c.getY()+addToY);
                }
            }
        }

        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            boolean isDark=ViewPlaying.this.control.darkModeOn();
            g2.setColor(isDark?new Color(231, 224, 203):Color.WHITE);//WhiteBoard a gauche
            g2.fillRect(0, 0, width/2, height);
            g2.setColor(isDark?new Color(163, 156, 137):new Color(213, 227, 255));//CommandBoard a droite
            g2.fillRect(width/2, 0, width/2, height);
        }
            
        public boolean isOptimizedDrawingEnabled(){//empecher foreground automatique
            return false;
        }
        
        void setToForeground(Component c){//met commandes a l avant plan
            this.remove(c);
            this.add(c, 0);
        }
            
        int regularize(int z, int w, boolean horizontal){//empeche commandes de sortir de dragDrop
            if(z<0) return 0;
            if(z+w>(horizontal?dragDrop.getSize().width:dragDrop.height))
                return (horizontal?dragDrop.getSize().width:dragDrop.height)-w;
            return z;
        }
        
        
        /***** Gestion Blocs *****/
        
        void addAvailableCommands() throws IOException{//premier ajout des commandes disponibles
            //ajout dans WhiteBoard
            commands.add(new CommandStart());//toujours le premier de la liste de commandes
            this.add(commands.getFirst());
            //ajout dans CommandBoard
            for(String c : level.availableCommands){
                if(c.equals("setAngle")) addTrigoCircle();
                lastPositionY+=addCommand(c, lastPositionY);
            }
        }
        
        void addTrigoCircle() throws IOException{//sert de guide au joueur
            JPanel pane=new JPanel();
            pane.setOpaque(false);
            JLabel circle=new JLabel(new ImageIcon("images/cercleTrigo.png"));
            pane.add(circle);
            pane.setBounds(3, height-303, 300, 300);
            this.add(pane);
        }
        
        int addCommand(String name, int positionY){//(re)generation des commandes
            Command toAdd;
            switch(name){
                case "for":
                    toAdd=new CommandFor(width/2+20, positionY);
                    break;
                case "if":
                    toAdd=new CommandIf(width/2+20, positionY);
                    break;
                case "while":
                    toAdd=new CommandWhile(width/2+20, positionY);
                    break;
                case "drawLine":
                    toAdd=new CommandDrawLine(width/2+20, positionY);
                    break;
                case "drawArc":
                    toAdd=new CommandDrawArc(width/2+20, positionY);
                    break;
                case "raisePutBrush":
                    toAdd=new CommandRaisePutBrush(width/2+20, positionY);
                    break;
                case "moveTo":
                    toAdd=new CommandMoveTo(width/2+20, positionY);
                    break;
                case "setAngle":
                    toAdd=new CommandSetAngle(width/2+20, positionY);
                    break;
                case "addAngle":
                    toAdd=new CommandAddAngle(width/2+20, positionY);
                    break;
                case "setColor":
                    toAdd=new CommandSetColor(width/2+20, positionY);
                    break;
                case "shiftColor":
                    toAdd=new CommandShiftColor(width/2+20, positionY);
                    break;
                case "symmetry":
                    toAdd=new CommandSymmetry(width/2+20, positionY);
                    break;
                case "affectation":
                    toAdd=new CommandAffectation(width/2+20, positionY);
                    break;
                case "addition":
                    toAdd=new CommandAddition(width/2+20, positionY);
                    break;
                case "soustraction":
                    toAdd=new CommandSoustraction(width/2+20, positionY);
                    break;
                case "multiplication":
                    toAdd=new CommandMultiplication(width/2+20, positionY);
                    break;
                case "division":
                    toAdd=new CommandDivision(width/2+20, positionY);
                    break;
                case "modulo":
                    toAdd=new CommandModulo(width/2+20, positionY);
                    break;
                default ://hookHorizontal
                    return 0;
            }
            this.add(toAdd);
            int res=toAdd.getHeight()+10;
            if(toAdd instanceof CommandWithCommands){
                CommandWithCommands tmp=(CommandWithCommands)toAdd;
                this.add(tmp.hookV);
                this.add(tmp.hookH);
                res+=tmp.hookV.getHeight()+tmp.hookH.getHeight();
            }
            SwingUtilities.updateComponentTreeUI(this);//refresh affichage
            return res;
        }
        
        Command addLoad(String name){//pour generer du code
            switch(name){
                case "for":
                    return new CommandFor(0, 0);
                case "if":
                    return new CommandIf(0, 0);
                case "while":
                    return new CommandWhile(0, 0);
                case "drawLine":
                    return new CommandDrawLine(0, 0);
                case "drawArc":
                    return new CommandDrawArc(0, 0);
                case "raisePutBrush":
                    return new CommandRaisePutBrush(0, 0);
                case "moveTo":
                    return new CommandMoveTo(0, 0);
                case "setAngle":
                    return new CommandSetAngle(0, 0);
                case "addAngle":
                    return new CommandAddAngle(0, 0);
                case "setColor":
                    return new CommandSetColor(0, 0);
                case "shiftColor":
                    return new CommandShiftColor(0, 0);
                case "symmetry":
                    return new CommandSymmetry(0, 0);
                case "affectation":
                    return new CommandAffectation(0, 0);
                case "addition":
                    return new CommandAddition(0, 0);
                case "soustraction":
                    return new CommandSoustraction(0, 0);
                case "multiplication":
                    return new CommandMultiplication(0, 0);
                case "division":
                    return new CommandDivision(0, 0);
                case "modulo":
                    return new CommandModulo(0, 0);
                default ://fonction void
                    CommandFunctionInit init=nameFUsed(name);
                    if(init==null){//init
                        nbOfFunV++;
                        return new CommandFunctionInit(name, width/2-200, 60+3*ViewPlaying.this.buttonH);
                    }
                    return new CommandFunctionCall(init, 0, 0);
            }
        }
        
        void addFunction(String name, int x, int y, boolean withArg){//nouvelle fonction, ajout dans WhiteBoard
            nbOfFunV++;
            CommandFunctionInit fC=(withArg)?new CommandFunctionInitInt(name, x, y):
                new CommandFunctionInit(name, x, y);
            Component[] toAdd={fC, fC.hookV, fC.hookH};
            for(Component c : toAdd) this.add(c, 0);
            if(withArg){
                addField(fC.input);
                addField(fC.hookH.input);
            }
            lastPositionY+=(withArg)?addCallInt(name,(CommandFunctionInitInt)fC):addCommandCall(fC,lastPositionY);
            commands.add(fC);
            SwingUtilities.updateComponentTreeUI(this);//refresh affichage
        }
        
        int addCommandCall(CommandFunctionInit init, int positionY){
            CommandFunctionCall callC=new CommandFunctionCall(init, width/2+20, positionY);
            this.add(callC);
            init.caller.add(callC);
            SwingUtilities.updateComponentTreeUI(this);//refresh affichage
            return callC.getHeight()+10;
        }
        
        int addCallInt(String name, CommandFunctionInitInt fC){
            if(nbOfFunI==0 && variables.isEmpty()) setBorderV(true);
            CommandFunctionCallInt call=new CommandFunctionCallInt(fC, width/2+20, lastPositionY);
            this.add(call);
            fC.caller.add(call);
            SwingUtilities.updateComponentTreeUI(this);//refresh affichage
            nbOfFunI++;
            return call.getHeight()+10;
        }
        
        void addVariable(String name, CommandFunctionInitInt fC){//nouvelle variable
            variables.put(name, 0);//met dans HashMap, par defaut variable=0
            varPanel.add(new VariablePanel(name, 0));//ajout de l'affichage            
            boolean firstSet=variables.size()==1;//ajout blocs de commande ou non
            if(firstSet){
                setBorderV(true);
                for(Component c : getComponents()){
                    if(c instanceof CommandOperationV && !inWhiteBoard(c)){//deja initialise une fois
                        firstSet=false;
                        break;
                    }
                }
            }
            if(firstSet) addFirstVariable();
            for(Component c : getComponents()){//ajout dans tous les comboBox
                if(c instanceof CommandIf || c instanceof CommandWhile || ((c instanceof CommandOperationV || c instanceof Variable)
                && (!firstSet || ((c instanceof Variable)?((Variable)c).varChoice.getItemCount()<1:
                ((CommandOperationV)c).variableG.getItemCount()<1)))) resizeBox(c, name, true);
            }
            resizeCommand();
        }
        
        void addFirstVariable(){
            lastPositionY+=addCommand("affectation", lastPositionY);
            lastPositionY+=addCommand("addition", lastPositionY);
            lastPositionY+=addCommand("soustraction", lastPositionY);
            lastPositionY+=addCommand("multiplication", lastPositionY);
            lastPositionY+=addCommand("division", lastPositionY);
            lastPositionY+=addCommand("modulo", lastPositionY);
            Variable variable=new Variable(width/2+20, lastPositionY, true);
            this.add(variable);
            lastPositionY+=variable.getHeight()+10;
            SwingUtilities.updateComponentTreeUI(this);//refresh affichage
        }
        
        void addSettedVariables(int positionY, CommandFunctionInitInt fC){//regeneration des variables
            Variable var=(fC==null)?new Variable(width/2+20, positionY, true):
                new CommandFunctionCallInt(fC, width/2+20, positionY);
            this.add(var);
            if(fC!=null) fC.caller.add((CommandFunctionCallInt)var);
            SwingUtilities.updateComponentTreeUI(this);//refresh affichage
        }
        
        void removeVariable(String name){
            variables.remove(name);
            for(Component c : varPanel.getComponents()){
                if(c instanceof VariablePanel && ((VariablePanel)c).getVarName().equals(name)){
                    varPanel.remove(c);
                    SwingUtilities.updateComponentTreeUI(varPanel);
                    break;
                }
            }
            if(variables.isEmpty()) makeVarEmpty();
            for(Component c : getComponents()){//retire la variable des comboBox
                if(c.getClass()==Variable.class || c instanceof CommandOperationV
                || c instanceof CommandIf || c instanceof CommandWhile) resizeBox(c, name, false);
            }
            resizeCommand();
        }
        
        void makeVarEmpty(){//vide les commandes en rapport avec Variable
            if(nbOfFunI==0) setBorderV(false);
            for(Component c : getComponents()){
                if(c instanceof CommandOperationV || (c instanceof Variable && !(c instanceof CommandFunctionCallInt))){
                    if(commands.remove(c)){//OperationV sur whiteBoard
                        ((Command)c).unStick();
                        if(((Command)c).next!=null) ((Command)c).next.unStick();
                        if(((Command)c).input.variable!=null && ((Command)c).input.variable instanceof CommandFunctionCallInt)
                            ((Command)c).input.variable.unStick();
                        fields.remove(((CommandOperationV)c).input);
                    }
                    else if(c instanceof Variable && ((Variable)c).linkedTo!=null) ((Variable)c).unStick();
                    if(inWhiteBoard(c)) this.remove(c);
                    SwingUtilities.updateComponentTreeUI(this);
                }
            }
        }
        
        void resizeBox(Component c, String name, boolean add){
            if(c instanceof CommandFunctionCallInt) return;
            JComboBox box=(c instanceof CommandIf)?((CommandIf)c).variableG:(c instanceof CommandWhile)?
                ((CommandWhile)c).variableG:(c instanceof Variable)?
                ((Variable)c).varChoice:((CommandOperationV)c).variableG;
            if(add) box.addItem(name);
            else box.removeItem(name);
            box.setPreferredSize(new Dimension(largestVariable(box), box.getHeight()));
            if(c instanceof Variable) ((Variable)c).resize();
        }
        
        int largestVariable(JComboBox box){//larger du comboBox apres retrait d un element
            if(box.getItemCount()==0) return 25;//liste vide
            JLabel largest=new JLabel(box.getItemAt(0).toString());
            for(int i=1; i<box.getItemCount(); i++){
                JLabel comp=new JLabel(box.getItemAt(i).toString());
                if(largest.getPreferredSize().width<comp.getPreferredSize().width) largest=comp;
            }
            largest.setText("  "+largest.getText()+"  ");
            return largest.getPreferredSize().width+15;
        }
        
        void resizeCommand(){//resize commandes sans variable
            for(Component c : getComponents()){
                if(c instanceof CommandFunctionCallInt) ((CommandFunctionCallInt)c).resize();
            }
            for(Component c : getComponents()){
                if(c instanceof Command) ((Command)c).resize();
            }
        }
        
        boolean toDelete(Component c){//quand pres de la poubelle
            if(c instanceof CommandFunctionInit) return false;//ne supprime pas les fonctions
            if(bin.getLocation().y>c.getLocation().y+c.getHeight()) return false;
            if(bin.getLocation().y+bin.getHeight()<c.getLocation().y) return false;
            if(bin.getLocation().x>c.getLocation().x+c.getWidth()) return false;
            return !(bin.getLocation().x+bin.getWidth()<c.getLocation().x);
        }

        void updateBinState(Component c){
            try{
                if(toDelete(c)) bin.loadBin("images/openBin.png");
                else bin.loadBin("images/closedBin.png");
            }
            catch(IOException e){}
        }
        
        void setBorderV(boolean color){//bord avec couleur ou non des NumberField
            for(NumberField f : fields) f.setBorder(color?borderV:null);
        }
            
        void addField(NumberField field){//fonction annexe
            if(field.container.getClass()!=CommandFunctionInit.class) fields.add(field);
            if(!variables.isEmpty() || nbOfFunI>0) field.setBorder(borderV);
        }
        
        
        /***** Create Levels *****/
        
        int getNumberFromHead(){//nombre de commandes depuis Start
            return getNumberFrom(commands.getFirst().next, new LinkedList<String>());
        }
        
        int getNumberFrom(Command tmp, LinkedList<String> functions){
            int res=0;
            while(tmp!=null){
                if(tmp instanceof CommandFunctionCall){//nouvelle fonction
                    CommandFunctionCall call=(CommandFunctionCall)tmp;
                    if(!functions.contains(call.name.getText())){
                        functions.add(call.name.getText());
                        res+=getNumberFrom(call.function.next, functions);
                    }
                }
                if(!(tmp instanceof CommandWithCommands)) res++;//cwc a toujours un hookH donc 2 commandes
                tmp=tmp.next;
            }
            return res;
        }
        
        int getNumberFunction(){//nombre de fonctions sans retour
            return getNumberFunction(commands.getFirst().next, new LinkedList<String>()).size();
        }
        
        LinkedList<String> getNumberFunction(Command tmp, LinkedList<String> functions){
            while(tmp!=null){
                if(tmp instanceof CommandFunctionCall){//nouvelle fonction
                    CommandFunctionCall call=(CommandFunctionCall)tmp;
                    if(!functions.contains(call.name.getText())){
                        functions.add(call.name.getText());
                        getNumberFunction(call.function.next, functions);
                    }
                }
                tmp=tmp.next;
            }
            return functions;
        }
        
        int getNumberFunctionInt(){//nombre de fonctions avec retour
            return getNumberFunctionInt(commands.getFirst().next, new LinkedList<String>()).size();
        }
        
        LinkedList<String> getNumberFunctionInt(Command tmp, LinkedList<String> functions){
            while(tmp!=null){
                if(tmp.input!=null){
                    addNameInt(tmp.input.variable, functions);
                    if(tmp instanceof CommandDrawArc || tmp instanceof CommandMoveTo)
                        addNameInt((tmp instanceof CommandDrawArc)?((CommandDrawArc)tmp).angleScan.variable:
                            ((CommandMoveTo)tmp).positionY.variable, functions);
                }
                tmp=tmp.next;
            }
            return functions;
        }
        
        void addNameInt(Variable var, LinkedList<String> functions){//fonction annexe
            if(var==null || !(var instanceof CommandFunctionCallInt)) return;
            CommandFunctionCallInt call=(CommandFunctionCallInt)var;
            if(!functions.contains(call.name.getText())){
                functions.add(call.name.getText());
                getNumberFunctionInt(call.function, functions);//fonction interne
            }
        }
        
        LinkedList<String> getCommands(){
            return getCommands(commands.getFirst().next, new LinkedList<String>(), new LinkedList<String>());
        }
        
        LinkedList<String> getCommands(Command tmp, LinkedList<String> commands, LinkedList<String> functions){//les differentes commandes utilisees
            while(tmp!=null){
                if(tmp instanceof CommandFunctionCall){//nouvelle fonction
                    CommandFunctionCall call=(CommandFunctionCall)tmp;
                    if(!functions.contains(call.name.getText())){
                        functions.add(call.name.getText());
                        getCommands(call.function.next, commands, functions);//ajout interne
                    }
                }
                else{
                    if(!(tmp instanceof CommandOperationV || commands.contains(tmp.name))) commands.add(tmp.name);
                    if(tmp.input!=null && tmp.input.variable!=null && tmp.input.variable instanceof CommandFunctionCallInt){
                        CommandFunctionCallInt var=(CommandFunctionCallInt)tmp.input.variable;
                        if(!functions.contains(var.name.getText())){
                            functions.add(var.name.getText());
                            getCommands(var.function.next, commands, functions);//ajout interne
                        }
                    }
                }
                tmp=tmp.next;
            }
            return commands;
        }
        
        String[] listToTab(LinkedList<String> list){
            int size=list.size();
            String[] res=new String[size];
            for(int i=0; i<size; i++) res[i]=list.get(i);
            return res;
        }
        
        
        /************************
        *          Bin          *
        ************************/
        
        private class Bin extends JPanel{
            BufferedImage state;

            Bin() throws IOException{
                this.setBounds(width/2-45, height-50, 40, 40);
                this.loadBin("images/closedBin.png");
                this.setOpaque(false);
            }

            void loadBin(String path) throws IOException{//ouverte ou fermee
                this.state=ImageIO.read(new File(path));
                this.repaint();
            }

            public void paintComponent(Graphics g){
                super.paintComponent(g);
                g.drawImage(this.state, 0, 0, 40, 40, null);
            }
        }


        /************************
        *     Command class     *
        ************************/

        class Command extends JPanel implements MouseInputListener{
            final String name;//if, else, for, while, ...
            final Color color;
            final int commandH=40, hookW=70, positionY;//hauteur d une commande, largeur d un hookH
            protected Command next, previous;//next a executer, previous pour ajuster l affichage
            private int mouseX, mouseY;//position initiale de la souris au moment du drag
            protected NumberField input;
            protected int commandW=0;//utile pour readapter largeur des NumberField
            
            Command(String name, Color color, int positionY){
                this.name=name;
                this.color=color;
                this.positionY=positionY;
                this.setBackground(color);
                this.setLayout(new GridBagLayout());//pour centrer verticalement les textes
                this.addMouseMotionListener(this);
                this.addMouseListener(this);
            }
            
            boolean canExecute(HashMap<String, Integer> map){//pour les commandes!=CWC qui ont exactement un
                return !input.isEmpty(map);
            }
            
            Command execute(HashMap<String, Integer> map){//chaque fonction les implemente
                return null;
            }
            
            void reset(){}//quand on interrompt le programme en plein milieu
            
            
            /***** Delete Command *****/
            
            void deleteSteps(){//enleve de commands et du panel
            	if(this instanceof CommandWithCommands){
                    ((CommandWithCommands)this).hookH.removeHH();
                    ((CommandWithCommands)this).hookV.removeHV();
                }
                PanelDragDropBoard.this.remove(this);
                if(commands.contains(this)){//sur whiteBoard
                    commands.remove(this);
                    if(input!=null){//variable
                        deleteVar(input);
                        if(this instanceof CommandDrawArc || this instanceof CommandMoveTo)
                            deleteVar((this instanceof CommandMoveTo)?
                                ((CommandMoveTo)this).positionY:((CommandDrawArc)this).angleScan);
                    }
                }
                else if(this instanceof CommandFunctionCall){
                    CommandFunctionCall tmp=(CommandFunctionCall)this;
                    tmp.function.caller.remove(tmp);
                    addCommandCall(tmp.function, tmp.positionY);
                }
                else addCommand(this.name, this.positionY);
                try{
                    bin.loadBin("images/closedBin.png");
                }
                catch(IOException e){}
                SwingUtilities.updateComponentTreeUI(ViewPlaying.this.dragDrop);//refresh affichage
                if(this.next!=null) this.next.deleteSteps();
            }
            
            void deleteVar(NumberField field){//fonction annexe
                if(field.variable!=null) field.variable.deleteSteps();
                else fields.remove(field);
            }
            
            
            /***** Regeneration of Command *****/
            
            void newDrag(boolean regen){//nouvelle commande qu on drag pour la premiere fois
                if(!inWhiteBoard(this) || commands.contains(this)) return;
                commands.add(this);
                if(this instanceof CommandWithCommands) commands.add(this.next);//ajout de HookH aussi
                if(level.numberOfVariables!=0 && input!=null){
                    addField(input);
                    if(this instanceof CommandDrawArc || this instanceof CommandMoveTo)
                        addField((this instanceof CommandDrawArc)?
                            ((CommandDrawArc)this).angleScan:((CommandMoveTo)this).positionY);
                }
                if(!regen) return;
                //pour regenerer commande utilisee :
                if(this instanceof CommandFunctionCall)
                    addCommandCall(((CommandFunctionCall)this).function, positionY);
                else addCommand(this.name, this.positionY);
            }

            
            /***** Stick together *****/
            
            void foundPrevious(){//reformation des liens previous/next
                if(brightC==null) return;//s il existe
                brightC.setNext(this);
                //update apres ajout de this :
                updateHookVRec(getTmpCwc());//adapte hauteur de tous les hookV recursivement
                updateAllLocation();//met a jour position des blocs impactes
            }
            
            void setNext(Command newNext){//changement pour que this.next=newNext, s utilise dans foundPrevious()
                if(this.next!=null){//insertion dans la liste chainee
                    Command end=newNext.getEnd();//fin de ce qu on drag
                    end.next=this.next;
                    this.next.previous=end;
                }
                this.next=newNext;
                newNext.previous=this;
            }

            void stick(){//colle this a son precedent
                if(!(this instanceof HookHorizontal)){//deplacement de HookH gerer dans celui de CWC
                    int positionX=previous.getLocation().x;//emplacement horizontal de this
                    if(previous instanceof CommandWithCommands)
                        positionX+=((CommandWithCommands)previous).hookV.getWidth();//colle a l interieur
                    this.setLocation(positionX, previous.getLocation().y+previous.getHeight());
                    stickVarToForeground();
                }
                if(this.next!=null) this.next.stick();
            }

            Command closeCommand(){//this et command sont assez proches pour se coller
                if(this instanceof CommandFunctionInit) return null;//initialisateur de fonction sans previous
                for(Command c : commands){
                    if(canStickFunctionInt(c) && canStickFunction(c)){
                        if(c instanceof CommandWithCommands){
                            if(closeHeight(c) && closeWidthIntern((CommandWithCommands)c)) return c;
                        }
                        else if(closeHeight(c) && closeWidth(c)) return c;
                    }
                }
                return null;
            }
            
            CommandFunctionCall nextCall(Command c){//prochain caller
                while(c!=null){
                    if(c instanceof CommandFunctionCall) return (CommandFunctionCall)c;
                    c=c.next;
                }
                return null;
            }
            
            boolean canStickFunction(Command c){//interdit recurrence dans fonctions de dessin
                if(c instanceof CommandFunctionInit){
                    CommandFunctionCall tmp=nextCall(this);
                    while(tmp!=null){
                        if(tmp.function==c) return false;
                        tmp=nextCall(tmp.next);
                    }
                }
                return true;
            }
            
            boolean canStickFunctionInt(Command c){//restriction de stick possible dans FunctionInt
                if(c instanceof CommandFunctionInitInt){
                    if(nextCall(this)!=null) return false;//ne permet pas stick de dessin
                    if(this instanceof CommandFor || this instanceof CommandIf || this instanceof CommandWhile
                        || this instanceof CommandOperationV) return true;
                    return !(c.getHead() instanceof CommandFunctionInitInt);
                }
                return true;
            }

            boolean closeHeight(Command c){//distance entre bas de c et haut de this
                int distance=this.getLocation().y-c.getLocation().y-c.getHeight();
                return distance>0 && distance<15;
            }

            boolean closeWidth(Command c){//distance entre cote gauche de this et celui de c
                int distance=this.getLocation().x-c.getLocation().x;
                return distance>-5 && distance<15;
            }

            boolean closeWidthIntern(CommandWithCommands c){//closeWidth pour CommandWithcCommands
                int distance=this.getLocation().x-c.getLocation().x;
                return distance>c.hookV.getWidth() && distance<c.hookV.getWidth()+20;
            }


            /***** Unstick *****/

            void unStick(){//decolle this (dragged) de son previous
                if(this.previous==null) return;//commande seule
                Command nextHookH=inCWC();//fin du bloc imbriquant ou null
                if(nextHookH!=null){//this etait dans un bloc imbriquant
                    Command tmpPrevious=this.previous;//pour update le bloc imbriquant ensuite
                    this.previous.next=nextHookH;//accroche precedent avec hookH du cwc
                    nextHookH.previous.next=null;//detache la fin du drag
                    nextHookH.previous=this.previous;//accroche hookH avec precedent
                    this.previous=null;//detache debut du drag
                    //update apres depart de this :
                    tmpPrevious.updateHookVRec(tmpPrevious.getTmpCwc());//taille des hookV
                    tmpPrevious.updateAllLocation();//position des blocs
                }
                else{//on decroche tout a partir de this
                    this.previous.next=null;
                    this.previous=null;
                }
                if(input!=null){
                    input.setBorder((variables.isEmpty() && nbOfFunI==0)?null:borderV);
                    if(this instanceof CommandMoveTo || this instanceof CommandDrawArc)
                        ((this instanceof CommandMoveTo)?((CommandMoveTo)this).positionY:
                        ((CommandDrawArc)this).angleScan).setBorder((variables.isEmpty() && nbOfFunI==0)?null:borderV);
                }
                if(limite!=null) limite.setValue(getNumberFromHead());
            }
            
            Command inCWC(){//cherche fin du bloc imbriquant
                Command tmp=this.next;
                while(tmp!=null){
                    if(tmp instanceof HookHorizontal){
                        if(tmp.getX()<this.getX()) return tmp;
                    }
                    tmp=tmp.next;
                }
                return null;
            }


            /***** Update of Command *****/
            
            Command getHead(){
                Command head=this;
                while(head.previous!=null) head=head.previous;
                return head;
            }
            
            Command getEnd(){
                Command end=this;
                while(end.next!=null) end=end.next;
                return end;
            }
            
            void updateAllLocation(){//met a jour localisation depuis tete de liste
                Command head=getHead();
                if(head.next!=null) head.next.stick();//met a jour recursivement blocs apres head
            }
            
            LinkedList<CommandWithCommands> getTmpCwc(){//renvoie toutes les cwc liees a this
                LinkedList<CommandWithCommands> res=new LinkedList<CommandWithCommands>();
                if(this instanceof CommandWithCommands) res.add((CommandWithCommands)this);
                Command c=this.previous;//pour parcourir les precedents
                while(c!=null){
                    if(c instanceof CommandWithCommands) res.add((CommandWithCommands)c);
                    c=c.previous;
                }
                c=this.next;//pour parcourir les suivants
                while(c!=null){
                    if(c instanceof CommandWithCommands) res.add((CommandWithCommands)c);
                    c=c.next;
                }
                return res;
            }
            
            CommandWithCommands innerHook(){//cherche les bloc imbriquants internes
                Command tmp=this.next;//this est forcement une cwc
                while(!(tmp instanceof HookHorizontal)){//donc s arrete
                    if(tmp instanceof CommandWithCommands) return (CommandWithCommands)tmp;
                    tmp=tmp.next;
                }
                return null;//pas de bloc imbriquante a l interieur de this, on peut ajuster this
            }
            
            void updateHookVRec(LinkedList<CommandWithCommands> list){//met a jour les hookV recursivement
                if(list.isEmpty()) return;
                CommandWithCommands first=list.getFirst();
                list.remove(first);
                CommandWithCommands inner=first.innerHook();//bloc imbriquante dans commande interne
                if(inner!=null) inner.updateHookVRec(list);
                first.updateHookV();//redimensionnement de first
                updateHookVRec(list);
            }
            
            void resize(){//readapte largeur, si necessaire
                if(input==null || (this instanceof HookHorizontal &&
                    !(((HookHorizontal)this).head instanceof CommandFunctionInitInt))) return;
                input.resizeField();//resize des composants du bloc
                
                int width=commandW+input.getPreferredSize().width;//resize du bloc
                NumberField f2=(this instanceof CommandMoveTo)?((CommandMoveTo)this).positionY:
                    (this instanceof CommandDrawArc)?((CommandDrawArc)this).angleScan:null;
                if(f2!=null){
                    f2.resizeField();//resize des composants du bloc
                    width+=f2.getPreferredSize().width;
                }
                else if(this instanceof CommandIf || this instanceof CommandWhile || this instanceof CommandOperationV)
                    width+=((this instanceof CommandIf)?((CommandIf)this).variableG:(this instanceof CommandWhile)?
                        ((CommandWhile)this).variableG:((CommandOperationV)this).variableG).getPreferredSize().width;
                setBounds(getX(), getY(), width, commandH);
                SwingUtilities.updateComponentTreeUI(this);
                
                if(f2!=null && f2.variable!=null) f2.variable.stick(false);//replacement de la 2e variable
                //replacement de variables initialisees a cote de comboBox
                else if(this instanceof CommandIf || this instanceof CommandWhile || this instanceof CommandOperationV){
                    Variable var=((Command)this).input.variable;
                    if(var!=null) var.stick(false);
                }
            }
            

            /***** Look *****/
            
            void setToForeground(){//this a l avant-plan quand on le drag==tete de liste
                if(this instanceof CommandWithCommands){
                    CommandWithCommands tmp=(CommandWithCommands)this;
                    ViewPlaying.this.dragDrop.remove(tmp.hookV);
                    ViewPlaying.this.dragDrop.add(tmp.hookV, 0);
                }
                dragDrop.setToForeground(this);
                stickVarToForeground();
                if(this.next!=null) this.next.setToForeground();//appel recursif
            }
            
            void stickVarToForeground(){
                if(input==null) return;
                stickVar(input.variable);
                if(this instanceof CommandDrawArc || this instanceof CommandMoveTo){
                    NumberField f=(this instanceof CommandDrawArc)?
                        ((CommandDrawArc)this).angleScan:((CommandMoveTo)this).positionY;
                    stickVar(f.variable);
                }
            }
            
            void stickVar(Variable var){//fonction annexe
                if(var==null) return;
                var.stick(false);
                dragDrop.setToForeground(var);
                if(var instanceof CommandFunctionCallInt){
                    Variable tmp=((CommandFunctionCallInt)var).input.variable;
                    if(tmp!=null) stickVar(tmp);
                }
            }
            
            void switchOff(){//eteint le seul bloc a eteindre
                if(brightC==null) return;
                brightC.setBackground(brightC.color);//pour eviter decalage brighter/darker
                brightC=null;
            }
            

            /***** Mouse Override *****/
            
            public void mousePressed(MouseEvent e){
                if(stop.isVisible()) return;
                unStick();//detache si a un precedent
                mouseX=e.getX();//position initiale de souris
                mouseY=e.getY();
                this.setToForeground();//mettre ce qu on drag a l avant-plan
                submit.setEnabled(false);
            }
            
            int canStick(Command nearby){//nombre de commandes qu on aura apres le stick
                Command previousNext=nearby.next;
                Command end=getEnd();
                nearby.next=this;//colle temporairement pour faire le calcul
                end.next=previousNext;
                int res=getNumberFromHead();
                nearby.next=previousNext;//remet le precedent
                end.next=null;
                return res;
            }

            public void mouseDragged(MouseEvent e){
                if(stop.isVisible() || this instanceof CommandOperationV && variables.isEmpty()) return;//immobile
                //drag this et ses next + variables
                int x=e.getXOnScreen()-ViewPlaying.this.dragDrop.getX()-mouseX-ViewPlaying.this.getX();
                int y=e.getYOnScreen()-y00-mouseY-ViewPlaying.this.getY();
                this.setLocation(regularize(x, getWidth(), true), regularize(y, getHeight(), false));
                stickVarToForeground();
                if(this.next!=null) this.next.stick();
                
                //allume et eteint les blocs selon les cas
                switchOff();//eteint tout d abord
                Command nearby=closeCommand();
                if(nearby!=null){//proche d un bloc
                    if(limite==null || canStick(nearby)<=level.numberOfCommands){//attachable
                        brightC=nearby;//allume le seul bloc a allumer
                        brightC.setBackground(brightC.getBackground().brighter());
                    }
                }
                updateBinState(this);//ouvre eventuellement la poubelle
            }

            public void mouseReleased(MouseEvent e){
                if(toDelete(this)) this.deleteSteps();
                else{
                    newDrag(true);
                    if(brightC!=null) foundPrevious();
                    switchOff();//eteint tout
                }
                if(limite!=null) limite.setValue(getNumberFromHead());
            }
            
            public void mouseMoved(MouseEvent e){}
            public void mouseClicked(MouseEvent e){}
            public void mouseEntered(MouseEvent e){}
            public void mouseExited(MouseEvent e){}
        }//fin classe interne interne Command
        

        /************************
        *  Subclass of Command  *
        ************************/
        
        class CommandStart extends Command{//bloc initial present que sur WhiteBoard
            CommandStart(){
                super("start", new Color(0, 204, 102), 20);
                this.add(new JLabel("  Start your code here !  "));
                this.setBounds(20, 20, getPreferredSize().width, commandH);
            }
            
            public void mouseDragged(MouseEvent e){}//empeche Start d etre deplacee
            
            boolean canExecute(HashMap<String, Integer> map){//verifie qu il n y a pas de champs vide, et champs vide en rouge
                Command tmp=this.next;
                boolean ok=true;
                while(tmp!=null){
                    if(!(tmp.canExecute(map))) ok=false;//on ne s arrete pas
                    tmp=tmp.next;
                }
                return ok;
            }
        }//fin classe interne Start
        
        
        class CommandWithCommands extends Command{
            final HookVertical hookV;//juste une accroche verticale
            final HookHorizontal hookH;//next si pas de commandes internes
            
            CommandWithCommands(String name, Color color, int x, int y){
                super(name, color, y);
                hookV=new HookVertical(color, commandH, x, y+commandH+deltaY);//apres la commande
                hookH=new HookHorizontal(this, x, y+2*commandH+deltaY);//apres hookV
                this.next=hookH;
            }
            
            boolean canExecute(HashMap<String, Integer> map){//ne peut pas etre vide
                boolean ok=true;
                if(this.getClass()!=CommandFunctionInit.class) ok=!input.isEmpty(map);
                else{//vide dans la fonction
                    Command tmp=this.next;
                    while(tmp!=hookH){
                        if(!(tmp.canExecute(map))) ok=false;//on ne s arrete pas
                        tmp=tmp.next;
                    }
                }
                if(ok && next==hookH){//champs remplis mais cwc vide
                    hookH.setBackground(Color.RED.darker());
                    return false;
                }
                hookH.setBackground(getBackground());//cwc rempli
                return ok;
            }

            boolean evaluate(String op, int varG, HashMap<String, Integer> map){
            	switch(op){
                    case "  <" : return varG<input.getNumber(map);
                    case " <=" : return varG<=input.getNumber(map);
                    case "  >" : return varG>input.getNumber(map);
                    case " >=" : return varG>=input.getNumber(map);
                    case " ==" : return varG==input.getNumber(map);
                    case " !=" : return varG!=input.getNumber(map);
                    case "%2=" : return varG%2==input.getNumber(map);
            	}
            	return false;
            }
            
            void error(CommandWithCommands cwc, boolean b){
                if(b){
                    cwc.setColor(Color.RED.darker());
                    stop();
                    return;
                }
                cwc.setColor(color);
            }
            
            void setColor(Color c){
                this.setBackground(c);
                hookH.setBackground(c);
                hookV.setBackground(c);
            }
            
            void updateHookV(){//met a jour hookV et accroche les hook a this
                int res=0;
                Command tmp=this.next;
                if(tmp==this.hookH) res=commandH;//cwc vide
                else{
                    while(tmp!=this.hookH){//tant qu on n a pas la fin du bloc imbriquant
                        res+=tmp.getHeight();
                        if(tmp instanceof CommandWithCommands){
                            if(((CommandWithCommands)tmp).hookH==tmp.next) res+=commandH;//cwc vide
                        }
                        tmp=tmp.next;
                    }
                }
                hookV.setBounds(res, hookH);
            }
            
            public void setLocation(int x, int y){
                super.setLocation(x, y);
                hookV.setLocation(x, y+getHeight());
                hookH.setLocation(x, hookV.getY()+hookV.getHeight());
                if(hookH.input.variable!=null) hookH.stickVar(hookH.input.variable);
            }

            class HookVertical extends JPanel{//accroche verticale des blocs imbriquants
                HookVertical(Color color, int commandH, int x, int y){
                    this.setBackground(color);
                    this.setBounds(x, y, 15, commandH);
                }
                
                void setBounds(int height, HookHorizontal hookH){
                    setBounds(getX(), getY(), getWidth(), height);
                    hookH.setLocation(getX(), getY()+height);
                    if(hookH.input.variable!=null) hookH.stickVar(hookH.input.variable);
                }
                
                void removeHV(){
                    PanelDragDropBoard.this.remove(this);
                }
            }//fin classe interne interne HookVertical
        }//fin classe CommandWithCommands


        class HookHorizontal extends Command{//fin de la boucle/condition
            final CommandWithCommands head;
            private JLabel ret=new JLabel("  return  ");
            private JLabel pres=new JLabel("  ");
            
            HookHorizontal(CommandWithCommands head, int x, int y){
                super("hookHorizontal", head.color, y);
                this.head=head;
                this.previous=head;
                input=new NumberField(this);
                Component[] toAdd={ret, input, pres};
                for(Component c : toAdd){
                    c.setVisible(false);
                    add(c);
                }
                this.setBounds(x, y, hookW, commandH/2);
                commandW=ret.getPreferredSize().width+10;
            }
            
            public void mouseDragged(MouseEvent e){}//ne peut pas etre dragged seul
            
            void removeHH(){
            	PanelDragDropBoard.this.remove(this);
                commands.remove(this);
            }
            
            boolean canExecute(HashMap<String, Integer> map){
                return (input.isVisible())?!input.isEmpty(map):true;
            }

            Command execute(HashMap<String, Integer> map){
                return head;//revenir en arriere pour for et while
            }
        }//fin classe HookHorizontal


        class CommandFor extends CommandWithCommands{//classe interne
            private int nbRepeats=-1;//pas initialisee
            
            CommandFor(int x, int y){
                super("for", new Color(230, 138, 0), x, y);
                super.input=new NumberField(this);//nombre de repetition a saisir
                
                Component[] toAdd={new JLabel("  Repeat  "), input, new JLabel("  time  ")};
                for(Component c : toAdd) this.add(c);
                setPreferredSize(new Dimension(getPreferredSize().width+10, getPreferredSize().height));//pb d affichage sinon
                this.setBounds(x, y+deltaY, getPreferredSize().width, commandH);
                super.commandW=getPreferredSize().width-input.getPreferredSize().width;
            }

            Command execute(HashMap<String, Integer> map){
                if(nbRepeats==-1){
                    int nb=input.getNumber(map);
                    nbRepeats=(nb<0)?0:nb;
                }
                if(--nbRepeats>=0) return next;
                return hookH.next;//nbRepeats reinitialisee a -1 a la fin
            }
            
            void reset(){
                this.nbRepeats=-1;
            }
        }//fin classe interne For


        class CommandIf extends CommandWithCommands{//classe interne
            private JComboBox variableG=new JComboBox(), operateur=new JComboBox();
            private String op="<";//par defaut
            //e.g x<100 <=> variableG(varG)="x", operateur="<", variableD(varD)="100"
            private boolean alreadyExecuted;
            
            CommandIf(int x, int y){
                super("if", new Color(188, 28, 132), x, y);
                super.input=new NumberField(this);//choix libre du joueur donc pas une liste
                
                String[] tmp={"x", "y", "angle"};
                for(String s : tmp) variableG.addItem(s);
                for(String s : variables.keySet()) variableG.addItem(s);

                this.operateur.addItemListener(new ItemListener(){
                    public void itemStateChanged(ItemEvent e){
                        op=(String)operateur.getSelectedItem();
                    }
                });
                String[] tmpOp={"  <", "  >", " <=", " >=", " ==", " !=", "%2="};
                for(String s : tmpOp) this.operateur.addItem(s);

                Component[] toAdd={new JLabel("  If  "), variableG, operateur, new JLabel("  "), input, new JLabel("  ")};
                for(Component c : toAdd) this.add(c);
                setPreferredSize(new Dimension(getPreferredSize().width+10, getPreferredSize().height));//pb d affichage sinon
                this.setBounds(x, y+deltaY, getPreferredSize().width, commandH);
                commandW=getPreferredSize().width-input.getPreferredSize().width-variableG.getPreferredSize().width;
            }

            Command execute(HashMap<String, Integer> map){
                int varG=variableG.getSelectedItem().equals("x")?blackBoard.x:
                     variableG.getSelectedItem().equals("y")?blackBoard.y:
                     variableG.getSelectedItem().equals("angle")?blackBoard.angle:
                     map.get(variableG.getSelectedItem().toString());
                alreadyExecuted=!alreadyExecuted;//quand hookH revient dessus, reinitialisera
                if(alreadyExecuted && evaluate(this.op, varG, map)) return next;
                reset();
                return hookH.next;
            }
            
            void reset(){
                alreadyExecuted=false;
            }
        }//fin classe interne If
        
        
        class CommandWhile extends CommandWithCommands implements ActionListener{//classe interne
            private JComboBox variableG=new JComboBox(), operateur=new JComboBox();
            private String op="<";
            private int whatIsVarG=0, limit=500;//x<=>0, y<=>1, angle<=>2, variables<=>3 ; pour simuler la terminaison
            
            CommandWhile(int x, int y){
                super("while", new Color(204, 102, 102), x, y);
                super.input=new NumberField(this);

                this.variableG.addActionListener(this);
                String[] tmp={"x", "y", "angle"};
                for(String s : tmp) variableG.addItem(s);
                for(String s : variables.keySet()) variableG.addItem(s);

                this.operateur.addItemListener(new ItemListener(){
                    public void itemStateChanged(ItemEvent e){
                        op=(String)operateur.getSelectedItem();
                    }
                });
                String[] tmpOp={"  <", "  >", " <=", " >=", " ==", " !=", "%2="};
                for(String s : tmpOp) this.operateur.addItem(s);

                Component[] toAdd={new JLabel("  While  "), variableG, operateur, new JLabel("  "), input, new JLabel("  ")};
                for(Component c : toAdd) this.add(c);
                setPreferredSize(new Dimension(getPreferredSize().width+10, getPreferredSize().height));//pb d affichage sinon
                this.setBounds(x, y+deltaY, getPreferredSize().width, commandH);
                commandW=getPreferredSize().width-input.getPreferredSize().width-variableG.getPreferredSize().width;
            }
            
            public void actionPerformed(ActionEvent e){
                whatIsVarG=variableG.getSelectedItem().equals("x")?0:
                           variableG.getSelectedItem().equals("y")?1:
                           variableG.getSelectedItem().equals("angle")?2:3;
            }

            Command execute(HashMap<String, Integer> map){
                if(limit==0){//si la terminaison a du etre simulee
                    error(this, true);
                    return null;
                }
                int varG=whatIsVarG==0?blackBoard.x:whatIsVarG==1?blackBoard.y:whatIsVarG==2?
                    blackBoard.angle:map.get(variableG.getSelectedItem().toString());
                if(evaluate(this.op, varG, map) && limit-->0) return next;
                reset();
                return hookH.next;
            }
            
            void reset(){
                limit=500;
                error(this, false);
            }
        }//fin classe interne While
        
        
        class CommandFunctionInit extends CommandWithCommands implements MouseListener{
            protected JLabel nameFunction;
            private CustomJButton changeName=new CustomJButton("", null);//pop up pour changer
            private LinkedList<CommandFunctionCall> caller=new LinkedList<CommandFunctionCall>();
            private boolean alreadyCall;
            
            CommandFunctionInit(String name, int x, int y){
                super("function", new Color(212, 115, 212), x, y-deltaY);
                super.input=new NumberField(this);
                input.setVisible(false);//visible que pour FunctionInt
                input.setPreferredSize(new Dimension(0, 0));
                try{
                    Image img=ImageIO.read(new File("images/engrenage.png"));
                    changeName.addImage(img);
                    changeName.setOpaque(false);
                }
                catch(IOException e){}
                changeName.setPreferredSize(new Dimension(commandH-10, commandH-10));
                changeName.addActionListener((event)->{
                    String newN=JOptionPane.showInputDialog(null, "New name ?", "Rename function", JOptionPane.QUESTION_MESSAGE);
                    while(newN!=null && (newN.equals("") || String.valueOf(newN.charAt(0)).matches("[0-9]")
                    || nameFUsed(newN)!=null || !newN.matches("^[a-zA-Z0-9]*$") || newN.length()>10))
                        newN=JOptionPane.showInputDialog(null, errorName, "Rename function", JOptionPane.QUESTION_MESSAGE);
                    if(newN!=null) rename(newN);
                });
                nameFunction=new JLabel("  "+name+"  ");
                
                Component[] toAdd={new JLabel("  "), changeName, nameFunction, new JLabel("( "), input, new JLabel(" )  ")};
                for(Component c : toAdd) this.add(c);
                setBounds(x, y, getPreferredSize().width+10, commandH);
                commandW=getPreferredSize().width+10;
            }
            
            void rename(String newName){
                this.nameFunction.setText("  "+newName+"  ");
                commandW=getPreferredSize().width+10;
                setBounds(getX(), getY(), commandW, commandH);
                commandW-=input.getPreferredSize().width;
                if(this instanceof CommandFunctionInitInt){
                    CommandFunctionInitInt init=(CommandFunctionInitInt)this;
                    SwingUtilities.updateComponentTreeUI(init);
                    if(init.input.variable!=null) init.input.variable.stick(false);
                    for(CommandFunctionCallInt c : init.caller) c.initializeDisplay();
                    return;
                }
                for(CommandFunctionCall c : caller) c.initializeDisplay();
            }
            
            void newDrag(){}//deja initialisee sur WhiteBoard
            void stick(){}//ne peut pas etre stick aux autres
            
            boolean canExecute(HashMap<String, Integer> map){
                error(this, false);
                try{
                    return super.canExecute(map);
                }
                catch(StackOverflowError e){
                    error(this, true);
                    return false;
                }
                catch(Exception e){
                    error(this, true);
                    return false;
                }
            }
            
            Command execute(HashMap<String, Integer> map){
                alreadyCall=!alreadyCall;
                if(alreadyCall) return next;
                return caller.getFirst().next;//first=celui qui a appele la fonction
            }
            
            void reset(){
                alreadyCall=false;
                Command tmp=next;
                while(tmp!=hookH){//reset interne
                    tmp.reset();
                    tmp=tmp.next;
                }
            }
        }//fin classe interne FunctionInit
        
        
        class CommandFunctionInitInt extends CommandFunctionInit{
            private LinkedList<CommandFunctionCallInt> caller=new LinkedList<CommandFunctionCallInt>();
            
            CommandFunctionInitInt(String name, int x, int y){
                super(name, x, y);
                Component[] visible={input, hookH.ret, hookH.input, hookH.pres};
                for(Component c : visible) c.setVisible(true);
                
                input.setBackground(new Color(255, 204, 255));
                input.setPreferredSize(new Dimension(input.fieldWidth, input.fieldHeight));
                setBounds(x, y, getPreferredSize().width+10, commandH);
                
                hookH.setBounds(hookH.getX(), hookH.getY(), hookH.commandW+input.fieldWidth, commandH);
            }
            
            boolean canExecute(HashMap<String, Integer> map){
                Command tmp=next;
                boolean ok=!input.isEmpty(map);
                while(tmp!=null){
                    if(!tmp.canExecute(map)) ok=false;
                    tmp=tmp.next;
                }
                return ok;
            }
            
            Command copy(){//pour le probleme de reset des cwc
                if(next==hookH) return hookH;
                Command tmp=next;//celui qu on copie
                LinkedList<Command> saveLast=new LinkedList<Command>();//derniers emplacements vides
                saveLast.add(new Command("",Color.BLACK,0));//inutile
                while(tmp!=hookH){
                    if(tmp instanceof HookHorizontal) saveLast.removeLast().next=saveLast.getLast();
                    else{
                        Command add=addLoad(tmp.name);
                        if(saveLast.size()>1) saveLast.removeLast().next=add;
                        else saveLast.getFirst().next=add;
                        if(add instanceof CommandWithCommands) saveLast.add(((CommandWithCommands)add).hookH);
                        saveLast.add(add);
                        setValue(add, tmp);
                    }
                    tmp=tmp.next;
                }
                saveLast.getLast().next=hookH;
                return saveLast.getFirst().next;
            }
            
            void setValue(Command add, Command tmp){//met les valeurs dans la copie de la fonction
                add.input=tmp.input;
                if(!(add instanceof CommandFor)){
                    if(add instanceof CommandOperationV)
                        ((CommandOperationV)add).variableG=((CommandOperationV)tmp).variableG;
                    else if(add instanceof CommandWhile){
                        CommandWhile cwc=(CommandWhile)add;
                        cwc.variableG=((CommandWhile)tmp).variableG;
                        cwc.whatIsVarG=((CommandWhile)tmp).whatIsVarG;
                        cwc.op=((CommandWhile)tmp).op;
                    }
                    else{
                        CommandIf cwc=(CommandIf)add;
                        cwc.variableG=((CommandIf)tmp).variableG;
                        cwc.op=((CommandIf)tmp).op;
                    }
                }
            }
            
            int executeInt(int arg){
                HashMap<String, Integer> localMap=new HashMap<String, Integer>();
                localMap.put(input.variable.varChoice.getSelectedItem().toString(), arg);
                Command runC=copy();
                while(runC!=hookH){
                    addToMap(runC, localMap);
                    runC=runC.execute(localMap);
                }
                return hookH.input.getNumber(localMap);
            }
            
            void addToMap(Command c, HashMap<String, Integer> map){
                if(c instanceof CommandFor || c instanceof HookHorizontal) return;
                String name="";
                if(c instanceof CommandOperationV) name=((CommandOperationV)c).variableG.getSelectedItem().toString();
                else name=((c instanceof CommandIf)?((CommandIf)c).variableG:
                    ((CommandWhile)c).variableG).getSelectedItem().toString();
                if(!map.containsKey(name)) map.put(name, 0);
            }
        }//fin classe interne FunctionInitInt
        
        
        class CommandFunctionCall extends Command{
            private CommandFunctionInit function;
            private JLabel name=new JLabel();
            
            CommandFunctionCall(CommandFunctionInit function, int x, int y){
                super("Call", new Color(212, 115, 212), y);
                this.function=function;
                initializeDisplay();
                this.add(name);
                this.add(new JLabel("( )  "));
                this.setBounds(x, y+deltaY, getPreferredSize().width, commandH);
            }
            
            void initializeDisplay(){
                name.setText(function.nameFunction.getText());
                setSize(getPreferredSize().width, commandH);
            }
            
            boolean inFunction(Command c){
                c=c.getHead();
                return c==this.function || c instanceof CommandFunctionInitInt;
            }
            
            Command closeCommand(){//this et c sont assez proches pour se coller
                for(Command c : commands){
                    if(!inFunction(c)){
                        if(c instanceof CommandWithCommands){
                            if(closeHeight(c) && closeWidthIntern((CommandWithCommands)c)) return c;
                        }
                        else if(closeHeight(c) && closeWidth(c)) return c;
                    }
                }
                return null;
            }
            
            boolean canExecute(HashMap<String, Integer> map){
                return function.canExecute(map);
            }
            
            Command execute(HashMap<String, Integer> map){
                function.reset();
                function.caller.remove(this);
                function.caller.addFirst(this);//se distingue des autres call
                return function.execute(map);
            }
        }//fin classe interne FunctionCall
        
        
        class CommandFunctionCallInt extends Variable{
            private CommandFunctionInitInt function;
            private JLabel name=new JLabel();
            private NumberField input=new NumberField(this);
            
            CommandFunctionCallInt(CommandFunctionInitInt function, int x, int y){
                super(x, y, false);
                this.function=function;
                this.remove(varChoice);
                
                initializeDisplay();
                Component[] toAdd={name, new JLabel("(  "), input, new JLabel("  )  ")};
                for(Component c : toAdd) this.add(c);
                this.setBounds(x, y+deltaY, getPreferredSize().width+10, variableH);
                variableW=getPreferredSize().width-input.getPreferredSize().width+10;
            }
            
            void initializeDisplay(){
                name.setText(function.nameFunction.getText());
                variableW=getPreferredSize().width-input.getPreferredSize().width+10;
                this.resize();
                if(input.variable!=null) input.variable.stick(false);
            }
        }//fin classe interne CommandFunctionCallInt


        /*************************
        *       Draw class       *
        *************************/

        class CommandDrawLine extends Command{//classe interne
            CommandDrawLine(int x, int y){
                super("drawLine", Color.CYAN.darker(), y);
                super.input=new NumberField(this);
                
                Component[] toAdd={new JLabel("  Draw a line of  "), input, new JLabel("  ")};
                for(Component c : toAdd) this.add(c);
                setPreferredSize(new Dimension(getPreferredSize().width+10, getPreferredSize().height));//pb d affichage sinon
                this.setBounds(x, y+deltaY, getPreferredSize().width, commandH);
                super.commandW=getPreferredSize().width-input.getPreferredSize().width;
            }

            Command execute(HashMap<String, Integer> map){
                int hypotenuse=input.getNumber(map);
                Vector v=new Vector();//pour creer objet interne
                Point p=v.destinationLine(blackBoard.x, blackBoard.y, blackBoard.angle, hypotenuse);
                
                //ajout du vecteur dans le dessin du joueur
                if(blackBoard.drawing){
                    Vector.VectorLine trait=v.new VectorLine(blackBoard.x,
                        blackBoard.y, p.x, p.y, blackBoard.angle, blackBoard.brushColor);
                    if(!level.addToDraw(trait)){//sort du tableau/trait trop petit, on arrete
                        input.border(true);
                        canSubmit=false;
                        return null;
                    }
                    input.border(false);//on enleve l erreur, si elle s est affichee precedemment
                    if(blackBoard.brush2){//symetrie
                        Vector.VectorLine trait2=v.new VectorLine(400-blackBoard.x,
                            blackBoard.y, 400-p.x, p.y, blackBoard.angle, blackBoard.brushColor);
                        level.addToDraw(trait2);
                    }
                }
                
                //nouvel emplacement du pinceau
                blackBoard.x=p.x;
                blackBoard.y=p.y;
                ViewPlaying.this.blackBoard.repaint();
                return next;
            }
        }//fin classe interne DrawLine


        class CommandDrawArc extends Command{//classe interne
            private NumberField angleScan=new NumberField(this);//hauteur==largeur, angle scane
            private JComboBox rightLeft=new JComboBox();
            private int sens=1;//1=gauche, -1=droite
            
            CommandDrawArc(int x, int y){
                super("drawArc", Color.CYAN.darker(), y);
                super.input=new NumberField(this);//radius
                
                rightLeft.addItemListener(new ItemListener(){
                    public void itemStateChanged(ItemEvent e){
                        if(e.getStateChange()==ItemEvent.SELECTED) sens*=-1;
                    }
                });
                rightLeft.addItem(" right ");
                rightLeft.addItem(" left ");
                
                Component[] toAdd={new JLabel("  Draw an arc with a radius of  "), input,
                    new JLabel("  and an angle of  "), angleScan, new JLabel("  on the  "), rightLeft, new JLabel("  ")};
                for(Component c : toAdd) this.add(c);
                setPreferredSize(new Dimension(getPreferredSize().width+10, getPreferredSize().height));//pb d affichage sinon
                this.setBounds(x, y+deltaY, getPreferredSize().width, commandH);
                commandW=getPreferredSize().width-input.getPreferredSize().width-angleScan.getPreferredSize().width;
            }
            
            boolean canExecute(HashMap<String, Integer> map){
                return !input.isEmpty(map) && !angleScan.isEmpty(map);
            }

            Command execute(HashMap<String, Integer> map){
                int rad=input.getNumber(map);
                int angleS=angleScan.getNumber(map);
                if(rad<2){//rayon trop petit, on arrete
                    input.border(true);
                    JOptionPane.showMessageDialog(null, "Radius so small !", "Warning !", JOptionPane.WARNING_MESSAGE);
                    canSubmit=false;
                    return null;
                }
                input.border(false);//on enleve l erreur, si elle s est affichee precedemment
                Vector v=new Vector();
                Point center=v.destinationLine(blackBoard.x, blackBoard.y, 180+blackBoard.angle, rad);//milieu du cercle
                Point origin=v.destinationLine(center.x, center.y, blackBoard.angle-sens*90, rad);//-90 pour gauche, +90 pour droite
                Point translation=new Point(blackBoard.x-origin.x, blackBoard.y-origin.y);
                
                if(blackBoard.drawing){//ajout du vecteur dans le dessin du joueur
                    Point square=v.destinationLine(center.x, center.y, 90, rad);//haut du carre
                    square=v.destinationLine(square.x, square.y, 180, rad);//coin gauche du carre
                    square=new Point(square.x+translation.x, square.y+translation.y);//carre translate
                    Vector.VectorArc arc=v.new VectorArc(square.x, square.y, rad*2, 
                        blackBoard.angle-90*sens, sens*angleS, blackBoard.brushColor);//-90*sens car translation
                    arc.center=new Point(center.x+translation.x, center.y+translation.y);//pour verifier si trop long
                    if(!level.addToDraw(arc)){//sort du tableau/trait trop petit, on arrete
                        angleScan.border(true);
                        canSubmit=false;
                        return null;
                    }
                    angleScan.border(false);//on enleve l erreur, si elle s est affichee precedemment
                    if(blackBoard.brush2){
                        Vector.VectorArc arc2=v.new VectorArc(400-square.x-rad*2, square.y, rad*2, 
                            180-(blackBoard.angle-90*sens), -sens*angleS, blackBoard.brushColor);
                        level.addToDraw(arc2);
                    }
                }
                
                //nouvel emplacement du pinceau
                Point dest=v.destinationLine(center.x, center.y, blackBoard.angle+(angleS-90)*sens, rad);
                blackBoard.x=dest.x+translation.x;
                blackBoard.y=dest.y+translation.y;
                blackBoard.angle=(angleS*sens+blackBoard.angle)%360;
                ViewPlaying.this.blackBoard.repaint();
                return next;
            }
        }//fin classe interne DrawArc


        class CommandRaisePutBrush extends Command{//classe interne
            private JComboBox raisePut=new JComboBox();
            private boolean choiceRes=true;//raise=false, put=true
            
            CommandRaisePutBrush(int x, int y){
                super("raisePutBrush", Color.LIGHT_GRAY.darker(), y);
                
                raisePut.addItemListener(new ItemListener(){
                    public void itemStateChanged(ItemEvent e){
                        if(e.getStateChange()==ItemEvent.SELECTED) choiceRes=!choiceRes;
                    }
                });
                raisePut.addItem(" Raise ");
                raisePut.addItem(" Put ");
                
                Component[] toAdd={new JLabel("  "), raisePut, new JLabel("  the pen  ")};
                for(Component c : toAdd) this.add(c);
                this.setBounds(x, y+deltaY, getPreferredSize().width+10, commandH);
            }
            
            boolean canExecute(HashMap<String, Integer> map){
                return true;
            }

            Command execute(HashMap<String, Integer> map){
                blackBoard.drawing=choiceRes;
                return(next!=null)?next.execute(map):null;
            }
        }//fin classe interne RaisePutBrush


        class CommandMoveTo extends Command{//classe interne
            private NumberField positionY=new NumberField(this);

            CommandMoveTo(int x, int y){
                super("moveTo", Color.LIGHT_GRAY.darker(), y);
                super.input=new NumberField(this);//positionX
                
                Component[] toAdd={new JLabel("  Move pen to (  "), input, new JLabel("  ,  "), positionY, new JLabel("  )  ")};
                for(Component c : toAdd) this.add(c);
                setPreferredSize(new Dimension(getPreferredSize().width+10, getPreferredSize().height));//pb d affichage sinon
                this.setBounds(x, y+deltaY, getPreferredSize().width, commandH);
                commandW=getPreferredSize().width-input.getPreferredSize().width-positionY.getPreferredSize().width;
            }
            
            boolean canExecute(HashMap<String, Integer> map){
                boolean empty=input.isEmpty(map);//pour afficher tous les champs d erreur en meme temps
                return !positionY.isEmpty(map) && !empty;
            }

            Command execute(HashMap<String, Integer> map){
                int[] newValues={input.getNumber(map), positionY.getNumber(map)};//x, y
                boolean x=newValues[0]<0 || newValues[0]>400;
                boolean y=newValues[1]<0 || newValues[1]>400;
                if(x || y){
                    JOptionPane.showMessageDialog(null, "You are out of bounds !", "Warning !", JOptionPane.WARNING_MESSAGE);
                    input.border(x);
                    positionY.border(y);
                    canSubmit=false;
                    return null;
                }
                input.border(false);
                positionY.border(false);
                blackBoard.x=input.getNumber(map);
                blackBoard.y=positionY.getNumber(map);
                ViewPlaying.this.blackBoard.repaint();
                return next;
            }
        }//fin de classe interne MoveTo


        class CommandSetAngle extends Command{//classe interne
            CommandSetAngle(int x, int y){
                super("setAngle", Color.LIGHT_GRAY.darker(), y);
                super.input=new NumberField(this);
                
                Component[] toAdd={new JLabel("  Set angle to  "), input, new JLabel("  ")};
                for(Component c : toAdd) this.add(c);
                setPreferredSize(new Dimension(getPreferredSize().width+10, getPreferredSize().height));//pb d affichage sinon
                this.setBounds(x, y+deltaY, getPreferredSize().width, commandH);
                super.commandW=getPreferredSize().width-input.getPreferredSize().width;
            }

            Command execute(HashMap<String, Integer> map){
                blackBoard.angle=input.getNumber(map);
                ViewPlaying.this.blackBoard.repaint();
                return next;
            }
        }//fin de classe interne ShiftAngle


        class CommandAddAngle extends Command{//classe interne
            CommandAddAngle(int x, int y){
                super("addAngle", Color.LIGHT_GRAY.darker(), y);
                super.input=new NumberField(this);
                
                Component[] toAdd={new JLabel("  Add  "), input, new JLabel("  to angle  ")};
                for(Component c : toAdd) this.add(c);
                setPreferredSize(new Dimension(getPreferredSize().width+10, getPreferredSize().height));//pb d affichage sinon
                this.setBounds(x, y+deltaY, getPreferredSize().width, commandH);
                super.commandW=getPreferredSize().width-input.getPreferredSize().width;
            }

            Command execute(HashMap<String, Integer> map){
                blackBoard.angle=(blackBoard.angle+input.getNumber(map))%360;
                ViewPlaying.this.blackBoard.repaint();
                return next;
            }
        }//fin de classe interne AddAngle


        class CommandSetColor extends Command{//classe interne
            private ColorBox colorChoice=new ColorBox();

            CommandSetColor(int x, int y){
                super("setColor", Color.LIGHT_GRAY.darker(), y);
                
                Component[] toAdd={new JLabel("  Set color to  "), colorChoice, new JLabel("  ")};
                for(Component c : toAdd) this.add(c);
                this.setBounds(x, y+deltaY, getPreferredSize().width+10, commandH);
            }
            
            boolean canExecute(HashMap<String, Integer> map){
                return true;
            }

            Command execute(HashMap<String, Integer> map){
                blackBoard.brushColor=colorChoice.colorRes;
                ViewPlaying.this.blackBoard.repaint();
                return next;
            }
        }//fin de classe interne ChangeColor
        

        class CommandShiftColor extends Command{
            CommandShiftColor(int x, int y){
                super("shiftColor", Color.LIGHT_GRAY.darker(), y);
                super.input=new NumberField(this);
                
                Component[] toAdd={new JLabel("  Add  "), input, new JLabel("  % to color  ")};
                for(Component c : toAdd) this.add(c);
                setPreferredSize(new Dimension(getPreferredSize().width+10, getPreferredSize().height));//pb d affichage sinon
                this.setBounds(x, y+deltaY, getPreferredSize().width, commandH);
                super.commandW=getPreferredSize().width-input.getPreferredSize().width;
            }
            
            int regularize(int n){
                if(n>255) return 255;
                if(n<0) return 0;
                return n;
            }

            Command execute(HashMap<String, Integer> map){
                int percent=(255*input.getNumber(map))/100;
                int nRed=regularize(blackBoard.brushColor.getRed()+percent);
                int nGreen=regularize(blackBoard.brushColor.getGreen()+percent);
                int nBlue=regularize(blackBoard.brushColor.getBlue()+percent);
                
                blackBoard.brushColor=new Color(nRed,nGreen,nBlue);
                ViewPlaying.this.blackBoard.repaint();
                return next;
            }
        }//fin de classe interne shiftColor


        class CommandSymmetry extends Command{//classe interne
            private JComboBox onOff=new JComboBox();
            private boolean choiceRes;//symetrie off=false, on=true
            
            CommandSymmetry(int x, int y){
                super("symmetry", Color.LIGHT_GRAY.darker(), y);
                
                onOff.addItemListener(new ItemListener(){
                    public void itemStateChanged(ItemEvent e){
                        if(e.getStateChange()==ItemEvent.SELECTED) choiceRes=!choiceRes;
                    }
                });
                onOff.addItem(" on ");
                onOff.addItem(" off ");
                
                Component[] toAdd={new JLabel("  Turn  "), onOff, new JLabel("  vertical symmetry  ")};
                for(Component c : toAdd) this.add(c);
                this.setBounds(x, y+deltaY, getPreferredSize().width+10, commandH);
            }
            
            boolean canExecute(HashMap<String, Integer> map){
                return true;
            }

            Command execute(HashMap<String, Integer> map){
                blackBoard.brush2=choiceRes;
                ViewPlaying.this.blackBoard.repaint();
                return (next!=null)?next.execute(map):null;
            }
        }//fin classe interne Symmetry


        /*************************
        *        Variable        *
        *************************/
        
        class Variable extends JPanel implements MouseInputListener{
            final int positionY, variableH;
            final Color color=new Color(255, 204, 255);
            private int mouseX, mouseY;
            protected int variableW;
            private boolean lastCreated=true;
            protected NumberField linkedTo;
            protected JComboBox varChoice=new JComboBox();
            
            Variable(int x, int y, boolean notCall){
                this.positionY=y;
                this.setBackground(color);
                this.setLayout(new GridBagLayout());
                this.addMouseMotionListener(this);
                this.addMouseListener(this);
                
                if(notCall){
                    varChoice.setBackground(color);
                    for(String varName : variables.keySet()) varChoice.addItem(varName);
                    varChoice.setPreferredSize(new Dimension(largestVariable(varChoice),varChoice.getPreferredSize().height-5));
                    Component[] toAdd={new JLabel("     "), varChoice, new JLabel("     ")};
                    for(Component c : toAdd) this.add(c);
                }
                NumberField tmp=new NumberField(null);//juste pour la hauteur
                variableH=tmp.fieldHeight;
                this.setBounds(x, y+deltaY, getPreferredSize().width+10, variableH);
                variableW=getPreferredSize().width-varChoice.getPreferredSize().width+10;
            }
            
            
            /***** Delete Variable *****/
            
            void deleteSteps(){//enleve variable du panel
                PanelDragDropBoard.this.remove(this);
                if(this instanceof CommandFunctionCallInt){
                    CommandFunctionCallInt tmp=(CommandFunctionCallInt)this;
                    tmp.function.caller.remove(tmp);
                    fields.remove(tmp.input);
                    if(tmp.input.variable!=null) tmp.input.variable.deleteSteps();
                }
                if(lastCreated) addSettedVariables(positionY, (this instanceof CommandFunctionCallInt)?
                    ((CommandFunctionCallInt)this).function:null);//regeneration
                try{
                    bin.loadBin("images/closedBin.png");
                }
                catch(IOException e){}
                SwingUtilities.updateComponentTreeUI(ViewPlaying.this.dragDrop);//refresh affichage
            }

            
            /***** Stick & Unstick *****/

            NumberField closeCommand(){//this et NumberField sont assez proches pour se coller
                CommandFunctionCallInt tmp=(this instanceof CommandFunctionCallInt)?(CommandFunctionCallInt)this:null;
                for(NumberField f : fields){//stick pas sur lui-meme ou dans variable interne/parametre de fonction
                    if((tmp==null || !((tmp.input==f || f.container instanceof CommandFunctionCallInt
                    || (f.container instanceof HookHorizontal && ((HookHorizontal)f.container).head==tmp.function))
                    || f.container instanceof CommandFunctionInitInt)) && closeHeight(f) && closeWidth(f)) return f;
                }
                return null;
            }

            boolean closeHeight(NumberField field){//distance entre haut de field et this
                int distance=getLocationOnScreen().y-field.getLocationOnScreen().y;
                return distance>-getHeight() && distance<getHeight();
            }
            
            boolean closeWidth(NumberField field){//distance entre cote gauche de this et celui de field
                int distance=getLocationOnScreen().x-field.getLocationOnScreen().x;
                return distance>-getWidth() && distance<field.getWidth();
            }
            
            void stick(boolean needResize){//colle this a son linkedTo
                if(needResize){
                    linkedTo=brightF;
                    linkedTo.variable=this;
                    if(linkedTo.container instanceof Command) ((Command)linkedTo.container).resize();
                    else ((CommandFunctionCallInt)linkedTo.container).resize();
                    fields.remove(linkedTo);//plus disponible pour liaison
                    switchOff();
                }
                int x=linkedTo.container.getX()+linkedTo.getX();
                int y=linkedTo.container.getY()+linkedTo.getY();
                this.setLocation(x, y);
                if(this instanceof CommandFunctionCallInt){
                    Variable tmp=((CommandFunctionCallInt)this).input.variable;
                    if(tmp!=null) tmp.stick(false);
                }
            }
            
            void unStick(){//decolle this (dragged) de son linkedTo
                if(linkedTo==null) return;
                fields.add(linkedTo);//disponible pour liaison
                linkedTo.variable=null;//suppression des liens
                if(linkedTo.container instanceof Command) ((Command)linkedTo.container).resize();
                else ((CommandFunctionCallInt)linkedTo.container).resize();
                linkedTo=null;
            }
            
            void resize(){
                if(this instanceof CommandFunctionCallInt){
                    CommandFunctionCallInt tmp=(CommandFunctionCallInt)this;
                    tmp.input.resizeField();
                    tmp.setBounds(getX(), getY(), tmp.variableW+tmp.input.getPreferredSize().width, getHeight());
                    if(tmp.linkedTo!=null) ((Command)tmp.linkedTo.container).resize();
                }
                else setBounds(getX(), getY(), variableW+varChoice.getPreferredSize().width, variableH);
                SwingUtilities.updateComponentTreeUI(this);
            }


            /***** Mouse Motion *****/
            
            void switchOff(){//eteint le seul field a eteindre
                brightF.setBorder(borderV);//pour eviter decalage brighter/darker
                brightF=null;
            }
            
            public void mousePressed(MouseEvent e){
                if(stop.isVisible()) return;
                unStick();//detache si etait attache
                mouseX=e.getX();//position initiale de souris
                mouseY=e.getY();
                dragDrop.setToForeground(this);//mettre ce qu on drag a l avant-plan
                submit.setEnabled(false);
            }

            public void mouseDragged(MouseEvent e){
                if(stop.isVisible() || (variables.isEmpty() && !(this instanceof CommandFunctionCallInt))) return;//immobile
                int x=e.getXOnScreen()-ViewPlaying.this.dragDrop.getX()-mouseX-ViewPlaying.this.getX();
                int y=e.getYOnScreen()-y00-mouseY-ViewPlaying.this.getY();
                this.setLocation(regularize(x, getWidth(), true), regularize(y, getHeight(), false));
                if(this instanceof CommandFunctionCallInt){
                    Variable tmp=((CommandFunctionCallInt)this).input.variable;
                    if(tmp!=null){
                        tmp.stick(false);
                        dragDrop.setToForeground(tmp);
                    }
                }
                
                //allume et eteint les NumberField selon les cas
                if(brightF!=null) switchOff();//eteint tout d abord
                NumberField nearby=closeCommand();
                if(nearby!=null){//proche d un bloc
                    brightF=nearby;//allume le seul necessaire
                    brightF.setBorder(BorderFactory.createLineBorder(Color.MAGENTA, 3));
                }
                updateBinState(this);//ouvre eventuellement la poubelle
            }

            public void mouseReleased(MouseEvent e){
                if(toDelete(this)) this.deleteSteps();
                else{
                    if(brightF!=null) stick(true);//accroche
                    if((inWhiteBoard(this) || linkedTo!=null) && lastCreated){
                        lastCreated=false;
                        CommandFunctionCallInt call=(this instanceof CommandFunctionCallInt)?(CommandFunctionCallInt)this:null;
                        addSettedVariables(positionY, (call!=null)?call.function:null);
                        if(call!=null) addField(call.input);
                    }
                }
            }
            
            public void mouseMoved(MouseEvent e){}
            public void mouseClicked(MouseEvent e){}
            public void mouseEntered(MouseEvent e){}
            public void mouseExited(MouseEvent e){}
        }//fin classe interne Variable
        
        
        class CommandOperationV extends Command{//classe interne
            protected JComboBox variableG=new JComboBox();
            
            CommandOperationV(String name, int x, int y, String sign){
                super(name, new Color(255, 153, 194), y);
                super.input=new NumberField(this);
                
                for(String varName : variables.keySet()) variableG.addItem(varName);
                Component[] toAdd={new JLabel("  "), variableG, new JLabel("  "+sign+"  "), input, new JLabel("  ")};
                for(Component c : toAdd) this.add(c);
                setPreferredSize(new Dimension(getPreferredSize().width+10, getPreferredSize().height));
                this.setBounds(x, y+deltaY, getPreferredSize().width, commandH);
                commandW=getPreferredSize().width-input.getPreferredSize().width-variableG.getPreferredSize().width;
            }
            
            void operation(HashMap<String, Integer> map, String name){}//override par enfants
            
            Command execute(HashMap<String, Integer> map){
                operation(map, variableG.getSelectedItem().toString());
                ViewPlaying.this.updateVariableDisplay();
                return next;
            }
        }//fin de classe interne OperationVariable


        class CommandAffectation extends CommandOperationV{//classe interne
            CommandAffectation(int x, int y){
                super("affectation", x, y, "=");
            }

            void operation(HashMap<String, Integer> map, String name){
                map.replace(name, input.getNumber(map));
            }
        }//fin de classe interne Affectation


        class CommandAddition extends CommandOperationV{//classe interne
            CommandAddition(int x, int y){
                super("addition", x, y, "+");
            }

            void operation(HashMap<String, Integer> map, String name){
                map.replace(name, map.get(name)+input.getNumber(map));
            }
        }//fin de classe interne Addition


        class CommandSoustraction extends CommandOperationV{//classe interne
            CommandSoustraction(int x, int y){
                super("soustraction", x, y, "-");
            }

            void operation(HashMap<String, Integer> map, String name){
                map.replace(name, map.get(name)-input.getNumber(map));
            }
        }//fin de classe interne Soustraction


        class CommandMultiplication extends CommandOperationV{//classe interne
            CommandMultiplication(int x, int y){
                super("multiplication", x, y, "*");
            }

            void operation(HashMap<String, Integer> map, String name){
                map.replace(name, map.get(name)*input.getNumber(map));
            }
        }//fin de classe interne Multiplication


        class CommandDivision extends CommandOperationV{//classe interne
            CommandDivision(int x, int y){
                super("division", x, y, "/");
            }

            void operation(HashMap<String, Integer> map, String name){
                map.replace(name, map.get(name)/input.getNumber(map));
            }
        }//fin de classe interne Division


        class CommandModulo extends CommandOperationV{//classe interne
            CommandModulo(int x, int y){
                super("modulo", x, y, "%");
            }

            void operation(HashMap<String, Integer> map, String name){
                map.replace(name, map.get(name)%input.getNumber(map));
            }
        }//fin de classe interne Division
        
        
        class NumberField extends JTextField{
            protected Variable variable;//varibale qu on lui stick
            final Component container;
            final int fieldHeight=30, fieldWidth=50;

            NumberField(Component c){
                super();
                setPreferredSize(new Dimension(fieldWidth, fieldHeight));
                this.container=c;
            }
            
            void resizeField(){
                if(container.getClass()==CommandFunctionInit.class) return;
                setPreferredSize(new Dimension((variable!=null)?
                    variable.getPreferredSize().width+10:fieldWidth, fieldHeight));
            }

            int getNumber(HashMap<String, Integer> map){
                if(variable!=null){
                    if(variable instanceof CommandFunctionCallInt){
                        CommandFunctionCallInt tmp=(CommandFunctionCallInt)variable;
                        tmp.function.error(tmp.function, false);
                        try{
                            return tmp.function.executeInt(tmp.input.getNumber(map));
                        }
                        catch(StackOverflowError e){
                            tmp.function.error(tmp.function, true);
                            return 0;
                        }
                        catch(Exception e){
                            tmp.function.error(tmp.function, true);
                            return 0;
                        }
                    }
                    String name=variable.varChoice.getSelectedItem().toString();
                    if(map.containsKey(name)) return map.get(name);
                }
                else{
                    try{
                        int res=Integer.parseInt(getText());
                        return res;
                    }
                    catch(NumberFormatException e){}
                }
                return 0;//rarement atteint en theorie
            }
            
            boolean isEmpty(HashMap<String, Integer> map){
                boolean empty=(variable==null && getText().isEmpty());
                if(variable!=null && variable instanceof CommandFunctionCallInt){
                    CommandFunctionCallInt call=(CommandFunctionCallInt)variable;
                    if(((Command)container).getHead() instanceof CommandFunctionInitInt) empty=call.input.isEmpty(map);
                    else empty=!call.function.canExecute(null);
                    if(empty) call.setBackground(Color.RED.darker());
                    else call.setBackground(call.color);
                    return call.input.isEmpty(map) || empty;
                }
                if(container instanceof CommandDivision) empty=getNumber(map)==0;
                if(empty){
                    border(true);
                    return true;
                }
                border(false);
                return false;
            }
            
            void border(boolean error){
                if(variable!=null) variable.setBackground(error?Color.RED.darker():variable.color);
                else setBorder(error?BorderFactory.createLineBorder(Color.RED.darker(), 3):
                    (variables.isEmpty() && nbOfFunI==0)?null:borderV);
            }

            protected Document createDefaultModel(){
                return new UpperCaseDocument();
            }

            class UpperCaseDocument extends PlainDocument{
                public void insertString(int offs, String s, AttributeSet a) throws BadLocationException{
                    if(stop.isVisible() || container instanceof CommandFunctionInitInt || variable!=null || s==null) return;
                    if(offs==0 && s.equals("-")) super.insertString(offs, s, a);//- en premier
                    else{
                        try{
                            Integer.parseInt(s);
                            super.insertString(offs, s, a);
                        }
                        catch(NumberFormatException e){}//pas un nombre
                    }
                }
                
                public void remove(int offs, int l) throws BadLocationException{
                    if(stop.isVisible()) return;
                    super.remove(offs, l);
                }
            }
        }//fin classe interne NumberField
    }//fin classe interne PanelDragDropBoard
}