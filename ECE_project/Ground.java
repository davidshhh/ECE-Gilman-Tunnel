import java.awt.*; 
import java.awt.image.*;
import java.applet.*;
import java.util.*;
import javax.imageio.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.geom.Line2D;
import javax.swing.*;

public class Ground extends JApplet implements Runnable, MouseListener, MouseMotionListener {

   final int JHULOC = 153;
   final int ECELOC = 338;

   final int picHeight = 10;
   final int picWidth = 10;

   // collisions constant
   final int HORIZONTAL_COLLIDE = 1;
   final int VERTICAL_COLLIDE = 2;
   final int INIT_COLLIDE = 4;
   
   //initial position of mouse
   final int NOWHERE = 9999;
   
   //constant factors that we can adjust for better animations
   final int OUT_FACTOR = 5000;
   final int IN_FACTOR = 10;
   final int FROG_FACTOR_ONE = 3; // square root frog movement speed
   final int FROG_FACTOR_TWO = 3; // constant frog movement speed
   final int FROG_FACTOR_THREE = 5; // sensitivity for landing
   
   final int BOXDIM = 200;
   final int WAVEWIDTH = 300;
   
   //flags for getVelocity()
   final int AWAY = 1;
   final int BACK = 2;
   final int FROG = 3;
   final int NOT_JUMPING = -99;
   
   //index of mouse, non-zero indexes are for multipoints simulations
   final int MOUSE = 0;
   Point fish;
   
   Image offscreen;
   Image background;

   //Declare a Thread object
   Thread myThread;
   
   //num parameters, feel free to change it
   final int numPic = 12;
   final int numBigLotus = 684;
   final int numFrog = 0;  // strictly numFrog < numBigLotus
   final int numObject = 684;
   final int numPeople = 1;
   final int numFrame = 28;
   final int numTrigger = 8;
   
   //radius constants
   final int radius = 200;  // radius for lotus
   final int frogRadius = 300; // radius for triggering frogs to jump

   //for checking whether the cursor is in the applet
   boolean mouseInScreen = false;
   
   //width and height of the window
   final int WIDTH = 1900;
   final int HEIGHT = 900;
   
   //random
   Random rnd = new Random();
   
   Trigger [] trigger;

   int timer = 0;

   //lists
   BufferedImage [] pics = new BufferedImage[numPic]; // the set of pics
   BufferedImage [] frames = new BufferedImage[numFrame]; // the set of frames
   Frog [] frog = new Frog[numFrog]; // the set of frogs
   ArrayList<Point> mouse = new ArrayList<Point>();
   Point [] list = new Point[numObject];  // the set of objects
   int [] letters = new int[6];
   
   LinkedList<Point> position = new LinkedList<Point>();
   
   // speed of strawberries
   final int simulationSpeed = 10;

   private class Point { // the object class, also sometimes used to pass x y corrdinates
      public double x, y, directionX, directionY, speedX, speedY, initX, initY, degrees;
      public double prevX, prevY; // x y positions of the point in the previous frame
      public Image img;
      public char letter;
      public boolean multiCollide;
      
      public Point(double x, double y) {
         this.x = x;
         this.y = y;
         this.prevX = this.x;
         this.prevY = this.y;
         this.directionX = 1;
         this.directionY = 1;
         this.speedX = 0;
         this.speedY = 0;
         this.initX = x;
         this.initY = y;
         this.multiCollide = false;
         this.degrees = rnd.nextInt(360);
      }
      public void changeDirectionX() {
         this.directionX *= -1;
      }
      public void changeDirectionY() {
         this.directionY *= -1;
      }
      public void update() {
         this.prevX = this.x;
         this.prevY = this.y;
         this.x = this.x + speedX * directionX;
         this.y = this.y + speedY * directionY;
      }
      public void setImage(Image i) {
         this.img = i;
      }
   
      public Point getLastPoint() {
         return new Point(this.prevX, this.prevY);
      }
      public Point getDrawPlace() {
         double tempX = this.x - picWidth / 2.0;
         double tempY = this.y - picHeight / 2.0;
         return new Point(tempX, tempY);
      }
     /* public Point getRelativeCenter(Point p) {
         double tempX = this.x + (this.img.getWidth(Test.this) - p.img.getWidth(Test.this)) / 2;
         double tempY = this.y + (this.img.getHeight(Test.this) - p.img.getHeight(Test.this)) / 2;
         return new Point(tempX, tempY);
      }*/
   }

   private class Frog extends Point {
      int home, destination;
      boolean jump, directionSet;
      public Frog(double x, double y, int home) {
         super(x, y);
         this.home = home;
         this.destination = home;
         this.jump = false;
         this.directionSet = false;
      }
      
      public void startJump(int destination) {
         this.jump = true;
         this.destination = destination;
      }
      public void land() {
         this.jump = false;
         this.directionSet = false;
      }
      public boolean isJumping() {
         return this.jump;
      }
   }

   private class Trigger extends Point {
      double waveCount;
      boolean wave;
      Color c;
      public Trigger(double x, double y, Color c) {
         super(x, y);
         this.waveCount = 0;
         this.wave = false;
         this.c = c;
      }
   }

	// initialise applet
   public void init()
   {
      offscreen = createImage(WIDTH, HEIGHT);
      try {
         pics[0] = ImageIO.read(new File("strawberry.gif"));
         pics[1] = ImageIO.read(new File("apple.jpg"));
         pics[2] = ImageIO.read(new File("banana.jpg"));
         pics[3] = ImageIO.read(new File("watermelon.jpg"));
         pics[4] = ImageIO.read(new File("orange.jpg"));
         pics[5] = ImageIO.read(new File("warning.jpg"));
         pics[6] = ImageIO.read(new File("good.jpg"));
         pics[7] = ImageIO.read(new File("lotus1.gif"));
         pics[8] = ImageIO.read(new File("lotus2.jpg"));
         pics[9] = ImageIO.read(new File("flower.jpg"));
         pics[10] = ImageIO.read(new File("orb.gif"));
         pics[10] = toBufferedImage(pics[10].getScaledInstance(90, 70, 0));
         pics[11] = ImageIO.read(new File("fish.jpg"));
         background = ImageIO.read(new File("background.jpg"));
      } 
      catch (IOException e) {}
      
      trigger = new Trigger[numTrigger];
      trigger[0] = new Trigger(100, 400, new Color(255, 0, 0));
      trigger[1] = new Trigger(250, 100, new Color(255, 127, 0));
      trigger[2] = new Trigger(1100, 200, new Color(255, 255, 0));
      trigger[3] = new Trigger(1600, 300, new Color(0, 255, 0));
      trigger[4] = new Trigger(1400, 50, new Color(0, 0, 255));
      trigger[5] = new Trigger(1200, 600, new Color(75, 0, 130));
      trigger[6] = new Trigger(450, 650, new Color(238, 130, 238));
      trigger[7] = new Trigger(700, 200, new Color(0, 255, 255));
      
      
      /*for (int i = 0; i < numObject; i++) {
         int picWidth = rnd.nextInt(40) + 60;
         if (i < numBigLotus) {
            picWidth = 150;
         }
         int picHeight = picWidth;
         int a = rnd.nextInt(WIDTH - picWidth);
         int b = rnd.nextInt(HEIGHT - picHeight);
         boolean repeat = false;
         Point newPoint = new Point(a, b);
         newPoint.setImage(pics[7].getScaledInstance(picWidth, picHeight, 0));
         //newPoint.setImage(pics[rnd.nextInt(5)].getScaledInstance(picWidth, picHeight, 0));
         for (int j = 0; j < i; j++) {
            if (checkCollide(list[j], newPoint) == 4) {
               repeat = true;
               break;
            }
         }
         if (!repeat) {
            this.list[i] = newPoint;
            if (i < this.numFrog) {
               this.frog[i] = new Frog(0, 0, i);
               this.frog[i].setImage(pics[10]);
            }
         } 
         else {
            i--;
         }
      }*/
      
      
      for (int i = 0; i < numFrog; i++) {
         this.frog[i] = new Frog(0, 0, 400 + i);
         this.frog[i].setImage(pics[10]);
      }
      
      int count = 0;
      for (int i = 0; i < (WIDTH / 50); i++) {
         for (int j = 0; j < (HEIGHT / 50); j++) {
            list[i * (HEIGHT / 50) + j] = new Point(i * 50, j * 50);
            list[i * (HEIGHT / 50) + j].letter = (char)rnd.nextInt(26);
            list[i * (HEIGHT / 50) + j].letter += 'A';
            //list[i * (HEIGHT / 50) + j].setImage(pics[7].getScaledInstance(10, 10, 0));
         }
      }
      list[JHULOC].letter = 'J';
      list[JHULOC + (HEIGHT / 50)].letter = 'H';
      list[JHULOC + (HEIGHT / 50) * 2].letter = 'U';
      list[ECELOC].letter = 'E';
      list[ECELOC + (HEIGHT / 50)].letter = 'C';
      list[ECELOC + (HEIGHT / 50) * 2].letter = 'E';
      list[200].letter = 'J';
      list[201].letter = 'I';
      list[202].letter = 'A';
      list[203].letter = 'W';
      list[204].letter = 'E';
      list[205].letter = 'I';

      letters[0] = JHULOC;
      letters[1] = JHULOC + (HEIGHT / 50);
      letters[2] = JHULOC + (HEIGHT / 50) * 2;
      letters[3] = ECELOC;
      letters[4] = ECELOC + (HEIGHT / 50);
      letters[5] = ECELOC + (HEIGHT / 50) * 2;
      
      
      
      Point p = new Point(NOWHERE, NOWHERE);
      p.setImage(pics[6].getScaledInstance(50, 50, 0));
      mouse.add(p);
      for (int i = MOUSE + 1; i < numPeople; i++) {
         Point temp = new Point(rnd.nextInt(WIDTH - 50), rnd.nextInt(HEIGHT - 50));
         temp.setImage(pics[0].getScaledInstance(50, 50, 0));
         temp.speedX = (rnd.nextInt(2) * 2 - 1) * this.simulationSpeed;
         temp.speedY = (rnd.nextInt(2) * 2 - 1) * this.simulationSpeed;
         mouse.add(temp);
      }
      addMouseListener(this);
      addMouseMotionListener(this);
      myThread = new Thread(this);
      myThread.start();
   }

   int max = -2;

   /** The run method.
   */
   public void run(){
      while(true){
         Graphics2D g = (Graphics2D) offscreen.getGraphics();
         //g.drawImage(background, 0, 0, this);
         g.setColor(Color.black);
         g.fillRect(0, 0, WIDTH, HEIGHT);
         //g.drawImage(this.frames[this.timer % 28], 0, 0, this);
         for (int i = 0; i < numObject; i++) {
            this.list[i].update();
         }
         
         for (int i = MOUSE + 1; i < numPeople; i++) {
            
            Point p = this.mouse.get(i);
            p.update();
            if (p.y >= HEIGHT - p.img.getHeight(this) || p.y <= 0) {
               p.changeDirectionY();
            }
            if (p.x >= WIDTH - p.img.getWidth(this) || p.x <= 0) {
               p.changeDirectionX();
            }
            mouse.set(i, p);
         }
         
         for (int i = 0; i < numObject; i++) {
            ArrayList<Integer> collide = new ArrayList<Integer>();
            if (list[i].y >= HEIGHT - picHeight) {
               list[i].y = HEIGHT - picHeight;
            }
            if (list[i].y < 0) {
               list[i].y = 1;
            }
            if (list[i].x >= WIDTH - picWidth) {
               list[i].x = WIDTH - picWidth;
            }
            if (list[i].x < 0) {
               list[i].x = 1;
            }
            
            this.list[i].speedX = 0;
            this.list[i].speedY = 0;
            boolean check = true;
            
            for (int p = 0; p < this.mouse.size(); p++) {
               double distance = this.getDistance(this.list[i], this.mouse.get(p));
               if (distance < this.radius  - this.radius / 10) {
                  check = false;
                  Point v = this.getVelocity(this.list[i], this.mouse.get(p), AWAY);
                  this.list[i].speedX += v.x;
                  this.list[i].speedY += v.y;
               } 
               else if (distance < this.radius) {
                  check = false;
                  this.list[i].speedX += 0;
                  this.list[i].speedY += 0;   
               } 
               else {
                  double x = (this.list[i].initX - this.list[i].x);
                  double y = (this.list[i].initY - this.list[i].y);
                  if (x != 0 && y != 0 && p == this.mouse.size() - 1 && check) {
                     Point init = new Point(this.list[i].initX, this.list[i].initY);
                     Point v = this.getVelocity(init, this.list[i], BACK);
                     this.list[i].speedX += v.x;
                     this.list[i].speedY += v.y;
                  } 
                  else {
                     this.list[i].speedX += 0;
                     this.list[i].speedY += 0;
                  }
               }
            }
            int alpha = 0;
            
            int red = 255;
            int green = 255;
            int blue = 255;
            
            int closest = NOWHERE;
            int index = 0;
            
            // determines which pedestrian is the closest, and sets the block's transparency
            for (int j = 0; j < this.mouse.size(); j++) {
               int d = (int)this.getDistance(this.list[i], this.mouse.get(j));
               if (d < closest) {
                  index = j;
                  closest = d;
               }
            }
            if (this.mouse.get(index).x == NOWHERE) {
               alpha = 0;
            } 
            else {
               alpha = (int) (255.0 * (1.0 / this.getDistance(this.list[i], this.mouse.get(index)) * 150));
            }
            if (alpha > 255 || alpha < 0) {
               alpha = 0;
            }
            boolean special = false;
            int triggerCount = 1;
            for (int j = 0; j < numTrigger; j++) {
               if (this.trigger[j].wave) {               
                  this.trigger[j].waveCount += 0.05;
                  Point t = new Point(this.trigger[j].x + BOXDIM/2, this.trigger[j].y + BOXDIM/2);
                  if (this.trigger[j].waveCount > 1000) {
                     this.trigger[j].waveCount = 0;
                     this.trigger[j].wave = false;
                  } 
                  else if (this.getDistance(list[i], t) > (this.trigger[j].waveCount) && this.getDistance(this.list[i], t) < (this.trigger[j].waveCount + WAVEWIDTH)) {
                     Color c = this.trigger[j].c;
                     red += c.getRed();
                     green += c.getGreen();
                     blue += c.getBlue();
                     triggerCount++;
                     for (int s = 0; s < letters.length; s++) {
                        if (i == letters[s]) {
                           special = true;
                        }
                     }
                  }
               }
               else {
                  for (int k = 0; k < this.mouse.size(); k++) {
                     if (!this.trigger[j].wave && this.inBox(this.mouse.get(k), this.trigger[j], BOXDIM, BOXDIM)) {
                        this.trigger[j].wave = true;
                     }
                  }
               }
            }
            boolean checkT = false;
            if (triggerCount > 1) {
               triggerCount--;
               red -= 255;
               green -= 255;
               blue -= 255;
               checkT = true;
            }
            red /= triggerCount;
            green /= triggerCount;
            blue /= triggerCount;
            if (alpha > max) {
               max = alpha;
            }
            Color c;
            if (checkT) {
               c = new Color(red, green, blue, 255);
               g.setColor(c);
            } 
            else {
               c = new Color(red, green, blue, alpha);
               g.setColor(c);
            }
            //g.drawImage(list[i].img, (int) list[i].x, (int) list[i].y, this);
            Point draw = list[i].getDrawPlace();
            //g.fillRect((int) draw.x, (int) draw.y, 10, 10);
            //c = new Color((int) (red * 0.50), (int) (green * 0.50), (int) (blue * 0.50), 255);
            //g.setColor(c);
            g.setStroke(new BasicStroke(2));
            if (special) {
               g.setColor(Color.white);
               g.setFont(new Font("TimesRoman", Font.PLAIN, 40));
               //Color c2 = new Color(red / 2, green / 2, blue / 2, 255);
               //g.setColor(c2);
               g.drawString(list[i].letter + "", (int)draw.x + 2, (int)draw.y - 2);
               g.setColor(new Color(255, 255, 255, 123));   
               g.setFont(new Font("TimesRoman", Font.PLAIN, 46)); 
               //g.setColor(c);
            } 
            else {
               g.setFont(new Font("TimesRoman", Font.PLAIN, 20)); 
            }
            g.drawString(list[i].letter + "", (int)draw.x, (int)draw.y);
            //g.drawRect((int) draw.x, (int) draw.y, 10, 10);
         }
         for (int i = 0; i < numTrigger; i++) {
            for (int j = 0; j < 10; j++) {
               Color temp = this.trigger[i].c;
               Color c = new Color(temp.getRed(), temp.getGreen(), temp.getBlue(), 10);
               g.setColor(c);
               g.fillOval((int) this.trigger[i].x + j * 10, (int) this.trigger[i].y + j * 10, BOXDIM - j * 10 * 2, BOXDIM - j * 10 * 2);
            }
         }
      
         for (int i = MOUSE + 1; i < this.mouse.size(); i++) {
            g.drawImage(this.mouse.get(i).img, (int) this.mouse.get(i).x, (int) this.mouse.get(i).y, this);
         }
         if (position.size() > 30) {
            position.remove();
         }
         /*Point last = new Point(this.mouse.get(MOUSE).x, this.mouse.get(MOUSE).y);
         position.add(last);
         for (int i = 1; i < position.size(); i++) {
            Color wakeC = new Color(247, 255, 124, i*6);
            g.setColor(wakeC);
            if (!(position.get(i - 1).x == position.get(i).x && position.get(i - 1).y == position.get(i).y)) {               
               //g.drawOval((int) this.position.get(i).x - (this.position.size() - i), (int) this.position.get(i).y - (this.position.size() - i), 70 - i * 2, 70 - i * 2);
               g.drawOval((int) this.position.get(i).x - (this.position.size() - i), (int) this.position.get(i).y - (this.position.size() - i), (900 - (int)Math.pow(i, 2)) / 20, (900 - (int)Math.pow(i, 2)) / 20);
            }
         }*/
         
        /* for (int i = 0; i < numFrog; i++) {
            this.frog[i].update();
            if (!this.frog[i].jump) {
               Point c = this.list[this.frog[i].destination].getRelativeCenter((Point) this.frog[i]);
               this.frog[i].x = c.x;
               this.frog[i].y = c.y;
            } 
            else {
               Point v = this.getVelocity(this.list[this.frog[i].destination].getRelativeCenter(this.frog[i]), this.frog[i], FROG);
               this.frog[i].speedX = v.x;
               this.frog[i].speedY = v.y;
               double distance = this.getDistance(this.list[this.frog[i].destination].getRelativeCenter(this.frog[i]), this.frog[i]);
               if (distance <= FROG_FACTOR_THREE) {
                  this.frog[i].land();
               }
            }
            for (int p = 0; p < this.mouse.size(); p++) {
               double distance = this.getDistance(this.mouse.get(p), this.frog[i]);
               if (!this.frog[i].jump && distance < this.frogRadius) {
                  int destination;
                  boolean taken = false;
                  int prev = this.frog[i].destination;
                  do {
                     taken = false;
                     destination = rnd.nextInt(numBigLotus);
                     for (int j = 0; j < numFrog; j++) {
                        if (j != i && this.frog[j].destination == destination) {
                           taken = true;
                           break;
                        }
                     }
                  } while (destination == prev || taken);
                  this.frog[i].startJump(destination);
               }
            }
            g.drawImage(this.rotate(this.frog[i]), (int) this.frog[i].x, (int) this.frog[i].y, this);
         }*/
         this.timer++;
         repaint();
         delay(50);    
      }
   }

	// paint method
   public void paint(Graphics g){
      g.drawImage(offscreen, 0, 0, this);
   }
   
   // this gets rid of the flash with the usage of offscreens
   public void update(Graphics g) {
      paint(g);
   }
   
   /** Delay the thread.
      @param time the time to delay the thread, in milliseconds
   */
   public void delay(int time) {
      try {
         Thread.sleep(time);
      } 
      catch (InterruptedException e) {
         e.printStackTrace();
      }
   }
   
   /** Converts an Image to a BufferedImage
      @param img the image to be converted
      @return the BufferedImage instance of img
   */
   public BufferedImage toBufferedImage(Image img) {
      if (img instanceof BufferedImage) {
         return (BufferedImage) img;
      }
      BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      Graphics2D bGr = bimage.createGraphics();
      bGr.drawImage(img, 0, 0, null);
      bGr.dispose();
      return bimage;
   }
   
   public boolean inBox(Point p, Point box, int x, int y) {
      if (p.x < box.x || p.x > box.x + x || p.y < box.y || p.y > box.y + y) {
         return false;
      }
      return true;
   }
   
   /** Rotates the image of a object, by degrees given by itself
      @param p the object to retoate
      @return the instance of the rotated Image
   */
   public Image rotate(Point p) {
      double convert = (360 - p.degrees + 90) % 360;
      double rotationRequired = Math.toRadians(convert);
      double locationX = p.img.getWidth(this) / 2;
      double locationY = p.img.getHeight(this) / 2;
      AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
      AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
      return op.filter(this.toBufferedImage(p.img), null);
   }

   /** Checks collisions between two objects.
      @param p1 the object to check collisions with
      @param p2 the other object to check collisions with
      @return collision flag, 0 is no collision
                              1 is horizontal collision only
                              2 is vertical collision only
                              3 is both collisions
                              4 is a collision during initialization
   */
   public int checkCollide(Point p1, Point p2) {
      int rt = 0;
      int w1 = p1.img.getWidth(this);
      int h1 = p1.img.getHeight(this);
      int w2 = p2.img.getWidth(this);
      int h2 = p2.img.getHeight(this);
      Point prev1 = p1.getLastPoint();
      Point prev2 = p2.getLastPoint();
      if (p1.x <= p2.x + w2 && p1.x + w1 >= p2.x && p1.y <= p2.y + h2 && p1.y + h1 >= p2.y) {
         if (prev1.x > prev2.x + w2 || prev1.x + w1 < prev2.x) {
            rt += HORIZONTAL_COLLIDE;
         }
         if (prev1.y > prev2.y + h2 || prev1.y + h1 < prev2.y) {
            rt += VERTICAL_COLLIDE;
         }
         if (rt == 0) {
            return INIT_COLLIDE;
         }
      }
      return rt;
   }
   
   /** Gets distance between two points
      @param a a point
      @param b the other point
      @return the distance between two points in double
   */
   public double getDistance(Point a, Point b) {
      return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
   }
   
   /** Gets the velocity of a point to another point
      @param
      @param
      @param flag 1 is when the point(object) is moving away from the mouse
                  2 is when the point(object) is moving back to its original position
                  3 is when the a frog is involved
      @param the velocity in the form of a Point
   */
   public Point getVelocity(Point to, Point from, int flag) {
      double distance = getDistance(to, from);
      double x = (to.x - from.x);
      double y = (to.y - from.y);
      double magnitude;
      if (flag == AWAY) {
         magnitude = OUT_FACTOR / distance;
      } 
      else if (flag == BACK) {
         magnitude = distance / IN_FACTOR;   
      } 
      else {
         magnitude = Math.sqrt(distance) * FROG_FACTOR_ONE + FROG_FACTOR_TWO;
      }
      if (flag == FROG) {
         Frog f = (Frog) from;
         if (!f.directionSet) {
            f.directionSet = true;
            double tempY = -y;
            double degrees = Math.toDegrees(Math.atan(tempY/x));
            if (x >= 0 && tempY >= 0) {
            } 
            else if (x <= 0 && tempY >= 0) {
               degrees = 180 + degrees;
            } 
            else if (x <= 0 && tempY <= 0) {
               degrees = 180 + degrees;
            } 
            else if (x >= 0 && tempY <= 0) {
               degrees = 360 + degrees;
            }
            from.degrees = degrees;
         }
      }
      double k = Math.sqrt(Math.pow(magnitude, 2) / ((Math.pow(x, 2)) + (Math.pow(y, 2))));
      Point v = new Point(k*x, k*y);
      return v;
   }
   
   // Methods from MouseMotionListener
   public void mouseMoved(MouseEvent e ) {  // called during motion when no buttons are down
      Point p = this.mouse.get(MOUSE);
      p.x = e.getX();
      p.y = e.getY();
      this.mouse.set(MOUSE, p);
      e.consume();
   }
   public void mouseDragged(MouseEvent e ) {  // called during motion with buttons down
      Point p = this.mouse.get(MOUSE);
      p.x = e.getX();
      p.y = e.getY();
      this.mouse.set(MOUSE, p);
      e.consume();
   }
   
   // Methods from MouseListener
   public void mousePressed(MouseEvent e) {}
   public void mouseReleased(MouseEvent e) {}
   public void mouseClicked(MouseEvent e) {}
   public void mouseEntered(MouseEvent e) {
      this.mouseInScreen = true;
   }

   public void mouseExited(MouseEvent e) {
      this.mouseInScreen = false;
      Point p = this.mouse.get(MOUSE);
      p.x = NOWHERE;
      p.y = NOWHERE;
      this.mouse.set(MOUSE, p);
   }

}