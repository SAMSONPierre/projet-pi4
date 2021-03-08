import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;

public class ViewSummaryChallenge extends ViewGame{//sommaire des defis
    final int buttonHeight=super.getButtonHeight();//hauteur d un bouton
    
    ViewSummaryChallenge(Player player){
        super(player);
        listeNiveau();
    }
    
    void listeNiveau(){
        JPanel summary=new JPanel();
        summary.setLayout(new FlowLayout());
        summary.setBounds(0,50+buttonHeight,widthFS,heightFS);
        File[] arrayLevels=nombreNiveau("levels/challenge/");
        for(int i=0; i<arrayLevels.length; i++){
            try{
                String name=arrayLevels[i].getName().substring(0, arrayLevels[i].getName().length()-4);
                Image img=ImageIO.read(new File("preview/challenge/"+name+".png"));
                CustomJButton jb=new CustomJButton(name, img);
                jb.addActionListener((event)->super.control.load("challenge/"+name));
                jb.setPreferredSize(new Dimension(200, 200));
                summary.add(jb);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        this.add(summary);
    }
    
    File[] nombreNiveau(String path){
        File file=new File(path);
        File[] files=file.listFiles();
        return files;
    }
    
    
    class CustomJButton extends JButton{
        private String text;
        private Image image;
        
        CustomJButton(String text, Image image){
            this.text=text;
            this.image=image;
        }
        
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if(image==null) return;
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
            if(text!=null){
                g.setColor(Color.WHITE);
                FontMetrics metric=g.getFontMetrics();
                int width=metric.stringWidth(text);
                g.drawString(text, (getWidth()-width)/2, (getHeight()+70)/2);
            }
        }
    }
}