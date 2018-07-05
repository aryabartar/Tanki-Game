package Engine; /*** In The Name of Allah ***/

import Blocks.Block;
import Blocks.DestroyableBlock;
import Blocks.UnDestroyableBlock;
import EnemyTanks.*;
import Equipment.*;
import Others.Geometry;
import Others.Point;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

/**
 * This class holds the state of game and all of its elements.
 * This class also handles user inputs, which affect the game state.
 *
 * @author ... :D
 */
public class GameState {

    //	public int locX, locY, diam;
    public boolean gameOver;

    private boolean keyUP, keyDOWN, keyRIGHT, keyLEFT;
    private boolean mouseLeftClicked, mouseRightClicked; //mouseRightClicked : false => bullet / true => rocket
    private int mouseX, mouseY; // for clicking
    private int mouseMotionX, mouseMotionY; //for mouse motion
    private KeyHandler keyHandler;
    private MouseHandler mouseHandler;

    private Tank mainTank;

    private static ArrayList<Bullet> bullets;
    private static ArrayList<Rocket> rockets;
    private static ArrayList<EnemyTank> enemyTanks;
    private static ArrayList<Block> blocks;
    private static ArrayList<Equipment> equipments;
    private static ArrayList<Point> destroyedTankTemporaryTrashPoints;
    private static ArrayList<MovingSmile> movingSmiles ;

    public GameState() {

        mainTank = new Tank();
        bullets = new ArrayList<>();
        rockets = new ArrayList<>();
        enemyTanks = new ArrayList<>();
        blocks = new ArrayList<>();
        equipments = new ArrayList<>();
        destroyedTankTemporaryTrashPoints = new ArrayList<>() ;
        movingSmiles = new ArrayList<>() ;

        addMapObjects();

        gameOver = false;
        //
        keyUP = false;
        keyDOWN = false;
        keyRIGHT = false;
        keyLEFT = false;
        //
        mouseLeftClicked = false;
        mouseRightClicked = false;

        mouseX = 0;
        mouseY = 0;
        //
        keyHandler = new KeyHandler();
        mouseHandler = new MouseHandler();
    }

    private void addMapObjects() {

        //add tanks here
        enemyTanks.add(new StaticTankEasy(300, 600));
        enemyTanks.add(new StaticTankHard(100, 600));
        enemyTanks.add(new DynamicTankEasy(1000, 200, 1000, 500));
        enemyTanks.add(new DynamicTankHard(600, 600, 1000, 600));

        // add blocks here
        blocks.add(new UnDestroyableBlock(500, 100));
        blocks.add(new UnDestroyableBlock(500, 200));
        blocks.add(new UnDestroyableBlock(500, 300));
        blocks.add(new UnDestroyableBlock(600, 300));
        blocks.add(new UnDestroyableBlock(600, 200));
        blocks.add(new UnDestroyableBlock(600, 100));
        blocks.add(new DestroyableBlock(500, 400));
        blocks.add(new DestroyableBlock(500, 500));

        // add equipment here
        equipments.add(new UpdateWeapon(800, 100));
        equipments.add(new Repair(800, 200));
        equipments.add(new Cartridge(800, 300));

        //Add moving smiles here :D
        movingSmiles.add(new MovingSmile(900 , 600)) ;
        movingSmiles.add(new MovingSmile(500 , 500)) ;
        movingSmiles.add(new MovingSmile(400 , 500)) ;

    }

    /**
     * The method which updates the game state.
     */
    public void update() {
        if (mouseLeftClicked) {
            if (mouseRightClicked == false) //bullet
                bullets.add(new Bullet(mainTank.getTankCenterX(), mainTank.getTankCenterY(), mainTank.getGunAndBodyRadian(), false));
            else {//rocket
                rockets.add(new Rocket(mainTank.getTankCenterX(), mainTank.getTankCenterY(), mainTank.getGunAndBodyRadian(), false));
            }
            mouseLeftClicked = !mouseLeftClicked;
        }

        if (keyUP)
            mainTank.moveLocY(-11);
        if (keyDOWN)
            mainTank.moveLocY(11);
        if (keyLEFT)
            mainTank.moveLocX(-11);
        if (keyRIGHT)
            mainTank.moveLocX(11);

        try {

            //moves bullets
            for (Bullet bullet : bullets) {
                if (bullet != null)
                    bullet.move();
            }
        } catch (ConcurrentModificationException e) {

        }

        try {
            for (Rocket rocket : rockets) {
                if (rocket != null)
                    rocket.move();

            }
        } catch (ConcurrentModificationException e) {

        }

        setMainTankAndGunRadian();
        setEnemyTanksRadian();

        moveDynamicTanks();
        updateAlphaInEquipments();

        checkShootHits();

        removeDeadBullets();
        removeDeadTanks();
        removeDestroyedBlocks();
        checkHitToEquipments();
        renderDestroyedTankPoints() ;
        attackMovingSmiles();
        findSmileFacesIntersects();
    }

    private void findSmileFacesIntersects() {
        for (int i = 0 ; i < movingSmiles.size(); i++ ) {
            Rectangle smileFaceRec = new Rectangle(movingSmiles.get(i).getLocX(), movingSmiles.get(i).getLocY(), MovingSmile.xPixels, MovingSmile.yPixels);
            Rectangle mainTankRec = new Rectangle(mainTank.getLocX(), mainTank.getLocY(), mainTank.getxPixels(), mainTank.getyPixels());

            if (smileFaceRec.intersects(mainTankRec)) {
                Point tempPoint = new Point(movingSmiles.get(i).getLocX() , movingSmiles.get(i).getLocY()) ;
                destroyedTankTemporaryTrashPoints.add(tempPoint) ;
                movingSmiles.remove(i);
                i-- ;
            }
        }
    }

    private void attackMovingSmiles () {
        for ( MovingSmile movingSmile : movingSmiles) {
            movingSmile.attackToThisLocation(mainTank.getTankCenterX() , mainTank.getTankCenterY());
        }
    }

    private void renderDestroyedTankPoints () {
        for (int i = 0 ; i < destroyedTankTemporaryTrashPoints.size() ; i++) {
            destroyedTankTemporaryTrashPoints.get(i).reduceTimeToRemove(3);

            if (destroyedTankTemporaryTrashPoints.get(i).getTimeToRemove() < 1)
                destroyedTankTemporaryTrashPoints.remove(i) ;
        }
    }

    private void checkHitToEquipments() {
        for (int i = 0; i < equipments.size(); i++) {
            if ((mainTank.getTankCenterX() > equipments.get(i).getLocX()) && (mainTank.getTankCenterX() < equipments.get(i).getEndX()) &&
                    (mainTank.getTankCenterY() > equipments.get(i).getLocY()) && (mainTank.getTankCenterY() < equipments.get(i).getEndY())) {
                //complete here later.
                equipments.remove(i);
                i--;
                System.out.println("Hit to equipment . Do sth later !");
            }
        }
    }

    private void updateAlphaInEquipments() {
        for (Equipment equipment : equipments) {
            equipment.updateAlpha();
        }
    }

    private void removeDestroyedBlocks() {

        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).isAlive() == false) {
                blocks.remove(i);
                i--;
            }
        }

    }

    private void moveDynamicTanks() {
        for (EnemyTank enemyTank : enemyTanks) {
            if (enemyTank instanceof DynamicTankEasy) {
                ((DynamicTankEasy) enemyTank).moveAutomatic(mainTank);
            }
            if (enemyTank instanceof DynamicTankHard) {
                ((DynamicTankHard) enemyTank).moveAutomatic(mainTank);
            }
        }
    }

    private void setEnemyTanksRadian() {
        for (EnemyTank enemyTank : enemyTanks) {
            enemyTank.setGunAndBodyRadian(Geometry.radian(enemyTank.getTankCenterX(), enemyTank.getTankCenterY(),
                    mainTank.getTankCenterX(), mainTank.getTankCenterY()));
        }
    }

    private void setMainTankAndGunRadian() {
        mainTank.setGunAndBodyRadian(Geometry.radian(getMainTank().getTankCenterX(), getMainTank().getTankCenterY(),
                getMouseMotionX(), getMouseMotionY()));
    }

    public KeyListener getKeyListener() {
        return keyHandler;
    }

    public MouseListener getMouseListener() {
        return mouseHandler;
    }

    public MouseMotionListener getMouseMotionListener() {
        return mouseHandler;
    }


    /**
     * The keyboard handler.
     */
    class KeyHandler extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W:
                    keyUP = true;
                    break;
                case KeyEvent.VK_S:
                    keyDOWN = true;
                    break;
                case KeyEvent.VK_A:
                    keyLEFT = true;
                    break;
                case KeyEvent.VK_D:
                    keyRIGHT = true;
                    break;
                case KeyEvent.VK_ESCAPE:
                    gameOver = true;
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W:
                    keyUP = false;
                    break;
                case KeyEvent.VK_S:
                    keyDOWN = false;
                    break;
                case KeyEvent.VK_A:
                    keyLEFT = false;
                    break;
                case KeyEvent.VK_D:
                    keyRIGHT = false;
                    break;
            }
        }
    }

    /**
     * This method will check if the bullets/rockets hit the enemy .
     */
    private void checkShootHits() {
        for (EnemyTank enemyTank : enemyTanks) {
            for (int i = 0; i < rockets.size(); i++) {
                if (rockets.get(i) != null) {
                    if ((rockets.get(i).getLocX() > enemyTank.getLocX()) && (rockets.get(i).getLocX() < enemyTank.getEndLocX()) &&
                            (rockets.get(i).getLocY() > enemyTank.getLocY()) && (rockets.get(i).getLocY() < enemyTank.getEndLocY())) {
                        if (rockets.get(i).isFromEnemy() == false) {
                            enemyTank.reduceHealth(Rocket.DAMAGE);
                        }
                        rockets.remove(i);
                    }
                }
            }
        }
        for (EnemyTank enemyTank : enemyTanks) {
            for (int i = 0; i < bullets.size(); i++) {
                if (bullets.get(i) != null) {
                    if ((bullets.get(i).getLocX() > enemyTank.getLocX()) && (bullets.get(i).getLocY() > enemyTank.getLocY()) &&
                            (bullets.get(i).getLocX() < enemyTank.getEndLocX()) && (bullets.get(i).getLocY() < enemyTank.getEndLocY())) {
                        if (bullets.get(i).isFromEnemy() == false) {
                            enemyTank.reduceHealth(Bullet.DAMAGE);
                        }
                        bullets.remove(i);
                    }
                }
            }
        }


        for (Block block : blocks) {
            for (int i = 0; i < rockets.size(); i++) {
                if (rockets.get(i) != null) {
                    if ((rockets.get(i).getLocX() > block.getLocX()) && (rockets.get(i).getLocX() < block.getEndX()) &&
                            (rockets.get(i).getLocY() > block.getLocY()) && (rockets.get(i).getLocY() < block.getEndY())) {

                        if (rockets.get(i).isFromEnemy() == false)
                            block.reduceHealth(Rocket.DAMAGE);

                        rockets.remove(i);

                    }
                }

            }
        }

        for (Block block : blocks) {
            for (int i = 0; i < bullets.size(); i++) {

                if (bullets.get(i) != null) {
                    if ((bullets.get(i).getLocX() > block.getLocX()) && (bullets.get(i).getLocY() > block.getLocY()) &&
                            (bullets.get(i).getLocX() < block.getEndX()) && (bullets.get(i).getLocY() < block.getEndY())) {
                        if (bullets.get(i).isFromEnemy() == false)
                            block.reduceHealth(Bullet.DAMAGE);
                        bullets.remove(i);
                    }
                }
            }
        }


        for (int i = 0; i < rockets.size(); i++) {
            if (rockets.get(i) != null) {
                if ((rockets.get(i).getLocX() > mainTank.getLocX()) && (rockets.get(i).getLocX() < mainTank.getEndLocX()) &&
                        (rockets.get(i).getLocY() > mainTank.getLocY()) && (rockets.get(i).getLocY() < mainTank.getEndLocY())) {

                    if (rockets.get(i).isFromEnemy() == true) {

                        //do sth here later !
                    }
                    rockets.remove(i);
                }
            }
        }
        for (int i = 0; i < bullets.size(); i++) {
            if (bullets.get(i) != null) {
                if ((bullets.get(i).getLocX() > mainTank.getLocX()) && (bullets.get(i).getLocY() > mainTank.getLocY()) &&
                        (bullets.get(i).getLocX() < mainTank.getEndLocX()) && (bullets.get(i).getLocY() < mainTank.getEndLocY())) {
                    if (bullets.get(i).isFromEnemy() == true) {
                    // do sth here later
                    }
                    bullets.remove(i);
                }
            }
        }
    }

    private void removeDeadBullets() {
        for (int i = 0; i < bullets.size(); i++) {
            if (bullets.get(i) != null)
                if (bullets.get(i).checkAlive() == false) {
                    bullets.remove(i);
                    i -= 1;
                }
        }
        for (int i = 0; i < rockets.size(); i++) {
            if (rockets.get(i) != null)
                if (rockets.get(i).checkAlive() == false) {
                    rockets.remove(i);
                    i--;
                }
        }
    }

    public void removeDeadTanks() {
        for (int i = 0; i < enemyTanks.size(); i++) {
            if (enemyTanks.get(i).getHealth() < 1) {
                Point tempPoint = new Point(enemyTanks.get(i).getLocX() , enemyTanks.get(i).getLocY()) ;
                destroyedTankTemporaryTrashPoints.add(tempPoint) ;
                enemyTanks.remove(i);
                i--;
            }
        }
    }

    /**
     * The mouse handler.
     */
    class MouseHandler extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();
            if (SwingUtilities.isLeftMouseButton(e)) {
                mouseLeftClicked = true;
            }
            if (SwingUtilities.isRightMouseButton(e)) {
                mouseRightClicked = !mouseRightClicked;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mouseLeftClicked = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
//			mouseX = e.getX();
//			mouseY = e.getY();
        }

        // for moving mouse !
        @Override
        public void mouseMoved(MouseEvent e) {
            mouseMotionX = e.getX();
            mouseMotionY = e.getY();
        }
    }

    public Tank getMainTank() {
        return mainTank;
    }

    public int getMouseMotionX() {
        return mouseMotionX;
    }

    public int getMouseMotionY() {
        return mouseMotionY;
    }

    public static ArrayList<Bullet> getBullets() {
        return bullets;
    }

    public static ArrayList<Rocket> getRockets() {
        return rockets;
    }

    public static ArrayList<EnemyTank> getEnemyTanks() {
        return enemyTanks;
    }

    public static void addToBullets(Bullet bullet) {
        bullets.add(bullet);
    }

    public static void addToRockets(Rocket rocket) {
        rockets.add(rocket);
    }

    public static ArrayList<Block> getBlocks() {
        return blocks;
    }

    public static ArrayList<Equipment> getEquipments() {
        return equipments;
    }

    public static ArrayList<Point> getDestroyedTankTemporaryTrashPoints() {
        return destroyedTankTemporaryTrashPoints;
    }

    public static ArrayList<MovingSmile> getMovingSmiles() {
        return movingSmiles;
    }
}