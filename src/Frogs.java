import java.applet.Applet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import TUIO.TuioBlob;
import TUIO.TuioClient;
import TUIO.TuioCursor;
import TUIO.TuioListener;
import TUIO.TuioObject;
import TUIO.TuioTime;

public class Frogs extends Applet implements Runnable, TuioListener {

    // collisions constant
    final int HORIZONTAL_COLLIDE = 1;
    final int VERTICAL_COLLIDE = 2;
    final int INIT_COLLIDE = 4;

    // initial position of mouse
    final int NOWHERE = 9999;

    // constant factors that we can adjust for better animations
    final int OUT_FACTOR = 1000;
    final int IN_FACTOR = 15;
    final int FROG_FACTOR_ONE = 3; // square root frog movement speed
    final int FROG_FACTOR_TWO = 3; // constant frog movement speed
    final int FROG_FACTOR_THREE = 5; // sensitivity for landing

    // flags for getVelocity()
    final int AWAY = 1;
    final int BACK = 2;
    final int FROG = 3;
    final int NOT_JUMPING = -99;

    // index of mouse, non-zero indexes are for multipoints simulations
    final int MOUSE = 0;
    Point fish;

    Image offscreen;
    Image background;

    // Declare a Thread object
    Thread myThread;

    // num parameters, feel free to change it
    final int numPic = 12;
    final int numBigLotus = 15;
    final int numFrog = 10; // strictly numFrog < numBigLotus
    final int numObject = 120;
    int numPeople = 0; // Start at zero, incrememnt every time someone new
                       // enteres the scene

    // radius constants
    final int radius = 500; // radius for lotus
    final int frogRadius = 300; // radius for triggering frogs to jump

    // for checking whether the cursor is in the applet
    boolean mouseInScreen = false;

    // width and height of the window
    final int WIDTH = 1900;
    final int HEIGHT = 900;

    // random
    Random rnd = new Random();

    // lists
    BufferedImage[] pics = new BufferedImage[numPic]; // the set of pics
    Frog[] frog = new Frog[numFrog]; // the set of frogs
    // Point[] mouse = new Point[numPeople]; // the set of people
    Point[] list = new Point[numObject]; // the set of objects

    Map<TuioObject, Point> tuioToPoints = new TreeMap<TuioObject, Point>();
    Map<TuioBlob, Point> blobsToPoints = new TreeMap<TuioBlob, Point>();

    // speed of strawberries
    final int simulationSpeed = 10;

    // Tuio stuff
    TuioClient tuioClient;

    private class Point { // the object class, also sometimes used to pass x y
                          // corrdinates
        public double x, y, directionX, directionY, speedX, speedY, initX,
                initY, degrees;
        public double prevX, prevY; // x y positions of the point in the
                                    // previous frame
        public Color c;
        public Image img;
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
            this.c = new Color(rnd.nextInt(256), rnd.nextInt(256),
                    rnd.nextInt(256));
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

        public void update(double newX, double newY) {
            x = newX;
            y = newY;
        }

        public void setImage(Image i) {
            this.img = i;
        }

        public Point getLastPoint() {
            return new Point(this.prevX, this.prevY);
        }

        public Point getRelativeCenter(Point p) {
            double tempX = this.x
                    + (this.img.getWidth(Frogs.this) - p.img
                            .getWidth(Frogs.this)) / 2;
            double tempY = this.y
                    + (this.img.getHeight(Frogs.this) - p.img
                            .getHeight(Frogs.this)) / 2;
            return new Point(tempX, tempY);
        }
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

    // initialise applet
    public void init() {
        System.out.println("WE ARE INITIING");

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
            pics[10] = ImageIO.read(new File("frog.gif"));
            pics[11] = ImageIO.read(new File("fish.jpg"));
            background = ImageIO.read(new File("background.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        tuioClient = new TuioClient(3333);
        tuioClient.connect();

        for (int i = 0; i < numObject; i++) {
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
            // newPoint.setImage(pics[rnd.nextInt(5)].getScaledInstance(picWidth,
            // picHeight, 0));
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
            } else {
                i--;
            }
        }

        myThread = new Thread(this);
        myThread.start();
    }

    /**
     * The run method.
     */
    public void run() {

        System.out.println("WHAT UP HOMIES");

        while (true) {
            Graphics g = offscreen.getGraphics();
            // g.drawImage(background, 0, 0, this);
            g.setColor(Color.cyan);
            g.fillRect(0, 0, WIDTH, HEIGHT);
            for (int i = 0; i < numObject; i++) {
                this.list[i].update();
            }

            for (int i = 0; i < numObject; i++) {
                ArrayList<Integer> collide = new ArrayList<Integer>();
                for (int b = 0; b < numObject; b++) {
                    if (i != b && checkCollide(list[i], list[b]) != 0) {
                        collide.add(checkCollide(list[i], list[b]));
                    }
                }
                /*
                 * if (!list[i].multiCollide && collide.size() > 1) {
                 * list[i].img = loading[list[i].img.getWidth(this)];
                 * list[i].multiCollide = true; } if (checkCollide(list[i],
                 * this.mouse) != 0) { collide.add(checkCollide(list[i],
                 * this.mouse)); }
                 */
                if (list[i].y >= HEIGHT - list[i].img.getHeight(this)) {
                    // list[i].changeDirectionY();
                    list[i].y = HEIGHT - list[i].img.getHeight(this);
                }
                if (list[i].y <= 0) {
                    list[i].y = 0;
                }/*
                  * else if (!collide.isEmpty() &&
                  * !collide.contains(HORIZONTAL_COLLIDE) || collide.size() > 1)
                  * { //!=1 list[i].changeDirectionY(); }
                  */
                if (list[i].x >= WIDTH - list[i].img.getWidth(this)) {
                    // list[i].changeDirectionX();
                    list[i].x = WIDTH - list[i].img.getWidth(this);
                }
                if (list[i].x <= 0) {
                    list[i].x = 0;
                }/*
                  * else if (!collide.isEmpty() &&
                  * !collide.contains(VERTICAL_COLLIDE) || collide.size() > 1) {
                  * list[i].changeDirectionX(); }
                  */
                boolean first = true;

                this.list[i].speedX = 0;
                this.list[i].speedY = 0;
                boolean check = true;

                // for (int p = 0; p < this.numPeople; p++) {
                for (Point p : blobsToPoints.values()) {
                    double distance = this.getDistance(this.list[i], p);

                    if (distance < this.radius - this.radius / 10) {
                        check = false;
                        Point v = this.getVelocity(this.list[i], p, AWAY);

                        this.list[i].speedX += v.x;
                        this.list[i].speedY += v.y;

                    } else if (distance < this.radius) {
                        check = false;
                        this.list[i].speedX += 0;
                        this.list[i].speedY += 0;
                    } else {
                        double x = (this.list[i].initX - this.list[i].x);
                        double y = (this.list[i].initY - this.list[i].y);
                        if (x != 0 && y != 0 /* && p == numPeople - 1 */
                                && check) {
                            Point init = new Point(this.list[i].initX,
                                    this.list[i].initY);
                            Point v = this
                                    .getVelocity(init, this.list[i], BACK);
                            this.list[i].speedX += v.x;
                            this.list[i].speedY += v.y;
                        } else {
                            this.list[i].speedX += 0;
                            this.list[i].speedY += 0;
                        }
                    }
                }
                g.drawImage(list[i].img, (int) list[i].x, (int) list[i].y, this);
            }

            for (Point p : blobsToPoints.values()) {
                g.drawImage(p.img, (int) p.x, (int) p.y, this);
            }

            for (int i = 0; i < numFrog; i++) {
                this.frog[i].update();
                if (!this.frog[i].jump) {
                    Point c = this.list[this.frog[i].destination]
                            .getRelativeCenter((Point) this.frog[i]);
                    this.frog[i].x = c.x;
                    this.frog[i].y = c.y;
                } else {
                    Point v = this.getVelocity(
                            this.list[this.frog[i].destination]
                                    .getRelativeCenter(this.frog[i]),
                            this.frog[i], FROG);
                    this.frog[i].speedX = v.x;
                    this.frog[i].speedY = v.y;
                    double distance = this.getDistance(
                            this.list[this.frog[i].destination]
                                    .getRelativeCenter(this.frog[i]),
                            this.frog[i]);
                    if (distance <= FROG_FACTOR_THREE) {
                        this.frog[i].land();
                    }
                }
                for (Point p : blobsToPoints.values()) {
                    double distance = this.getDistance(p, this.frog[i]);
                    if (!this.frog[i].jump && distance < this.frogRadius) {
                        int destination;
                        boolean taken = false;
                        int prev = this.frog[i].destination;
                        do {
                            taken = false;
                            destination = rnd.nextInt(numBigLotus);
                            for (int j = 0; j < numFrog; j++) {
                                if (j != i
                                        && this.frog[j].destination == destination) {
                                    taken = true;
                                    break;
                                }
                            }
                        } while (destination == prev || taken);
                        this.frog[i].startJump(destination);
                    }
                }
                g.drawImage(this.rotate(this.frog[i]), (int) this.frog[i].x,
                        (int) this.frog[i].y, this);
            }
            repaint();

            delay(50);
        }
    }

    // paint method
    public void paint(Graphics g) {
        g.drawImage(offscreen, 0, 0, this);
    }

    // this gets rid of the flash with the usage of offscreens
    public void update(Graphics g) {
        paint(g);
    }

    /**
     * Delay the thread.
     * 
     * @param time
     *            the time to delay the thread, in milliseconds
     */
    public void delay(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts an Image to a BufferedImage
     * 
     * @param img
     *            the image to be converted
     * @return the BufferedImage instance of img
     */
    public BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        BufferedImage bimage = new BufferedImage(img.getWidth(null),
                img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();
        return bimage;
    }

    /**
     * Rotates the image of a object, by degrees given by itself
     * 
     * @param p
     *            the object to retoate
     * @return the instance of the rotated Image
     */
    public Image rotate(Point p) {
        double convert = (360 - p.degrees + 90) % 360;
        double rotationRequired = Math.toRadians(convert);
        double locationX = p.img.getWidth(this) / 2;
        double locationY = p.img.getHeight(this) / 2;
        AffineTransform tx = AffineTransform.getRotateInstance(
                rotationRequired, locationX, locationY);
        AffineTransformOp op = new AffineTransformOp(tx,
                AffineTransformOp.TYPE_BILINEAR);
        return op.filter(this.toBufferedImage(p.img), null);
    }

    /**
     * Checks collisions between two objects.
     * 
     * @param p1
     *            the object to check collisions with
     * @param p2
     *            the other object to check collisions with
     * @return collision flag, 0 is no collision 1 is horizontal collision only
     *         2 is vertical collision only 3 is both collisions 4 is a
     *         collision during initialization
     */
    public int checkCollide(Point p1, Point p2) {
        int rt = 0;
        int w1 = p1.img.getWidth(this);
        int h1 = p1.img.getHeight(this);
        int w2 = p2.img.getWidth(this);
        int h2 = p2.img.getHeight(this);
        Point prev1 = p1.getLastPoint();
        Point prev2 = p2.getLastPoint();
        if (p1.x <= p2.x + w2 && p1.x + w1 >= p2.x && p1.y <= p2.y + h2
                && p1.y + h1 >= p2.y) {
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

    /**
     * Gets distance between two points
     * 
     * @param a
     *            a point
     * @param b
     *            the other point
     * @return the distance between two points in double
     */
    public double getDistance(Point a, Point b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    /**
     * Gets the velocity of a point to another point
     * 
     * @param
     * @param
     * @param flag
     *            1 is when the point(object) is moving away from the mouse 2 is
     *            when the point(object) is moving back to its original position
     *            3 is when the a frog is involved
     * @param the
     *            velocity in the form of a Point
     */
    public Point getVelocity(Point to, Point from, int flag) {
        double distance = getDistance(to, from);
        double x = (to.x - from.x);
        double y = (to.y - from.y);
        double magnitude;
        if (flag == AWAY) {
            magnitude = OUT_FACTOR / distance;
        } else if (flag == BACK) {
            magnitude = distance / IN_FACTOR;
        } else {
            magnitude = Math.sqrt(distance) * FROG_FACTOR_ONE + FROG_FACTOR_TWO;
        }
        if (flag == FROG) {
            Frog f = (Frog) from;
            if (!f.directionSet) {
                f.directionSet = true;
                double tempY = -y;
                double degrees = Math.toDegrees(Math.atan(tempY / x));
                if (x >= 0 && tempY >= 0) {
                    System.out.println("quadrant 1");
                } else if (x <= 0 && tempY >= 0) {
                    System.out.println("before conversion " + degrees);
                    degrees = 180 + degrees;
                    System.out.println("after convresion " + degrees);
                    System.out.println("quadrant 2");
                } else if (x <= 0 && tempY <= 0) {
                    degrees = 180 + degrees;
                    System.out.println("quadrant 3");
                } else if (x >= 0 && tempY <= 0) {
                    degrees = 360 + degrees;
                    System.out.println("quadrant 4");
                }
                from.degrees = degrees;
            }
        }
        double k = Math.sqrt(Math.pow(magnitude, 2)
                / ((Math.pow(x, 2)) + (Math.pow(y, 2))));
        Point v = new Point(k * x, k * y);
        return v;
    }

    /*
     * //Methods for implementing TuioListener /** this is called when an object
     * becomes visible
     */
    public void addTuioObject(TuioObject tobj) {

        Point pointToAdd = new Point(tobj.getX(), tobj.getY());
        pointToAdd.setImage(pics[6].getScaledInstance(50, 50, 0));
        tuioToPoints.put(tobj, pointToAdd);
        numPeople++;

    }

    /**
     * an object was moved on the table surface
     */
    public void updateTuioObject(TuioObject tobj) {

        // Update the coords of the point in the table
        tuioToPoints.get(tobj).update(tobj.getX(), tobj.getY());

    }

    /**
     * an object was removed from the table
     */
    public void removeTuioObject(TuioObject tobj) {

        tuioToPoints.remove(tobj);
        numPeople--;
    }

    /**
     * this is called when a new cursor is detected
     */
    public void addTuioCursor(TuioCursor tcur) {

    }

    /**
     * a cursor was moving on the table surface
     */
    public void updateTuioCursor(TuioCursor tcur) {

    }

    /**
     * a cursor was removed from the table
     */
    public void removeTuioCursor(TuioCursor tcur) {

    }

    /**
     * this is called when a new blob is detected
     */
    public void addTuioBlob(TuioBlob tblob) {

        Point pointToAdd = new Point(tblob.getX(), tblob.getY());
        pointToAdd.setImage(pics[6].getScaledInstance(50, 50, 0));
        blobsToPoints.put(tblob, pointToAdd);
        numPeople++;

    }

    /**
     * a blob is moving on the table surface
     */
    public void updateTuioBlob(TuioBlob tblob) {

        // Update the coords of the point in the table
        blobsToPoints.get(tblob).update(tblob.getX(), tblob.getY());
    }

    /**
     * a blob was removed from the table
     */
    public void removeTuioBlob(TuioBlob tblob) {

        blobsToPoints.remove(tblob);
        numPeople--;

    }

    /**
     * this method is called after each bundle, use it to repaint your screen
     * for example
     */
    public void refresh(TuioTime frameTime) {

    }

}