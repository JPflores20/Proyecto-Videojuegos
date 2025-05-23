package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class Main extends SimpleApplication {

    private BulletAppState bulletAppState;
    private Node tower;

    private ArrayList<Geometry> activeBoxes = new ArrayList<>();
    private ArrayList<Geometry> targetBoxes = new ArrayList<>();

    private BitmapText scoreText, livesText, enemyText, gameOverText;
    private int score = 0;
    private int lives = 3;
    private boolean isGameOver = false;

    private Random rand = new Random();
    private float shootCooldown = 0.3f;
    private float timeSinceLastShot = 0;
    private float enemySpeed = 3f;

    private boolean isShooting = false;
    private float gameOverTimer = 0f;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(30);
        setUpLight();

        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        createFloor();
        createTower();

        // Ya no creamos cajas azules, eliminadas

        createTargetBoxes();

        initKeys();
        initHUD();
    }

    private void setUpLight() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f));
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
    }

    private void createFloor() {
        Box floorBox = new Box(50, 0.1f, 50);
        Geometry floorGeo = new Geometry("Floor", floorBox);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Gray);
        floorGeo.setMaterial(mat);
        floorGeo.setLocalTranslation(0, -0.1f, 0);
        floorGeo.addControl(new RigidBodyControl(0));
        bulletAppState.getPhysicsSpace().add(floorGeo.getControl(RigidBodyControl.class));
        rootNode.attachChild(floorGeo);
    }

    private void createTower() {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.DarkGray);

        Geometry base = new Geometry("Base", new Box(2, 0.2f, 2));
        base.setMaterial(mat);
        base.setLocalTranslation(0, 0.2f, 0);

        Geometry platform = new Geometry("Platform", new Box(1.5f, 0.1f, 1.5f));
        platform.setMaterial(mat);
        platform.setLocalTranslation(0, 6, 0);

        Geometry pole = new Geometry("Pole", new Box(0.3f, 3f, 0.3f));
        pole.setMaterial(mat);
        pole.setLocalTranslation(0, 3, 0);

        tower = new Node("Tower");
        tower.attachChild(base);
        tower.attachChild(platform);
        tower.attachChild(pole);

        base.addControl(new RigidBodyControl(0));
        bulletAppState.getPhysicsSpace().add(base.getControl(RigidBodyControl.class));
        rootNode.attachChild(tower);

        cam.setLocation(new Vector3f(0, 7, 5));
        cam.lookAt(tower.getLocalTranslation().add(0, 6, 0), Vector3f.UNIT_Y);
    }

    private void createTargetBoxes() {
        Material redMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        redMat.setColor("Color", ColorRGBA.Red);
        int numBoxes = 12;
        float radius = 20;

        for (int i = 0; i < numBoxes; i++) {
            float angle = (float) (i * (2 * Math.PI / numBoxes));
            float x = (float) (radius * Math.cos(angle));
            float z = (float) (radius * Math.sin(angle));
            Geometry box = new Geometry("Target Box", new Box(1, 1, 1));
            box.setMaterial(redMat);
            box.setLocalTranslation(x, 1, z);
            box.addControl(new RigidBodyControl(2));
            bulletAppState.getPhysicsSpace().add(box.getControl(RigidBodyControl.class));
            rootNode.attachChild(box);
            targetBoxes.add(box);
        }
    }

    private void initKeys() {
        inputManager.addMapping("Shoot", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(actionListener, "Shoot");
    }

    private final ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Shoot")) isShooting = isPressed;
        }
    };

    private void shootBullet() {
        Sphere sphere = new Sphere(16, 16, 0.2f);
        Geometry bullet = new Geometry("bullet", sphere);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Yellow);
        bullet.setMaterial(mat);
        bullet.setLocalTranslation(cam.getLocation().add(cam.getDirection().mult(2)));

        bullet.addControl(new RigidBodyControl(1));
        bullet.getControl(RigidBodyControl.class).setLinearVelocity(cam.getDirection().mult(50));
        bullet.getControl(RigidBodyControl.class).setGravity(Vector3f.ZERO);

        rootNode.attachChild(bullet);
        bulletAppState.getPhysicsSpace().add(bullet.getControl(RigidBodyControl.class));
    }

    private void initHUD() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");

        scoreText = new BitmapText(guiFont);
        scoreText.setColor(ColorRGBA.White);
        scoreText.setLocalTranslation(10, settings.getHeight() - 10, 0);
        guiNode.attachChild(scoreText);

        livesText = new BitmapText(guiFont);
        livesText.setColor(ColorRGBA.Red);
        livesText.setLocalTranslation(10, settings.getHeight() - 40, 0);
        guiNode.attachChild(livesText);

        enemyText = new BitmapText(guiFont);
        enemyText.setColor(ColorRGBA.Green);
        enemyText.setLocalTranslation(10, settings.getHeight() - 70, 0);
        guiNode.attachChild(enemyText);

        updateHUD();
    }

    private void updateHUD() {
        scoreText.setText("Score: " + score);
        livesText.setText("Lives: " + lives);
        enemyText.setText("Enemies: " + targetBoxes.size());
    }

    private void addScore(int points) {
        score += points;
        if (score % 10 == 0) enemySpeed += 0.5f;
        updateHUD();
    }

    private void loseLife() {
        lives--;
        updateHUD();
        if (lives <= 0 && !isGameOver) {
            isGameOver = true;
            gameOverText = new BitmapText(guiFont);
            gameOverText.setSize(40);
            gameOverText.setColor(ColorRGBA.Red);
            gameOverText.setText("GAME OVER\nReiniciando...");
            gameOverText.setLocalTranslation(settings.getWidth() / 2 - 150, settings.getHeight() / 2, 0);
            guiNode.attachChild(gameOverText);
        }
    }

    private void resetGame() {
        score = 0;
        lives = 3;
        enemySpeed = 3f;
        isGameOver = false;
        gameOverTimer = 0;
        guiNode.detachChild(gameOverText);

        for (Geometry geo : activeBoxes) {
            bulletAppState.getPhysicsSpace().remove(geo.getControl(RigidBodyControl.class));
            rootNode.detachChild(geo);
        }
        activeBoxes.clear();

        for (Geometry geo : targetBoxes) {
            bulletAppState.getPhysicsSpace().remove(geo.getControl(RigidBodyControl.class));
            rootNode.detachChild(geo);
        }
        targetBoxes.clear();

        for (Spatial bullet : rootNode.getChildren()) {
            if (bullet.getName() != null && bullet.getName().equals("bullet")) {
                bulletAppState.getPhysicsSpace().remove(bullet.getControl(RigidBodyControl.class));
                rootNode.detachChild(bullet);
            }
        }

        createTargetBoxes();
        // Ya no generamos cajas azules
        updateHUD();
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (isGameOver) {
            gameOverTimer += tpf;
            if (gameOverTimer >= 5f) resetGame();
            return;
        }

        timeSinceLastShot += tpf;
        if (isShooting && timeSinceLastShot >= shootCooldown) {
            shootBullet();
            timeSinceLastShot = 0;
        }

        ArrayList<Geometry> toRemove = new ArrayList<>();
        for (Geometry box : activeBoxes) {
            if (box.getLocalTranslation().y < 0) {
                toRemove.add(box);
            }
        }
        for (Geometry box : toRemove) {
            activeBoxes.remove(box);
            bulletAppState.getPhysicsSpace().remove(box.getControl(RigidBodyControl.class));
            rootNode.detachChild(box);
            addScore(1);
            // Ya no generamos cajas azules
        }

        ArrayList<Geometry> toRemoveEnemies = new ArrayList<>();
        for (Geometry enemy : targetBoxes) {
            Vector3f dir = tower.getWorldTranslation().subtract(enemy.getWorldTranslation()).normalize();
            enemy.getControl(RigidBodyControl.class).setLinearVelocity(dir.mult(enemySpeed));
            if (enemy.getWorldTranslation().distance(tower.getWorldTranslation()) < 2f) {
                loseLife();
                toRemoveEnemies.add(enemy);
            }
        }
        for (Geometry enemy : toRemoveEnemies) {
            targetBoxes.remove(enemy);
            bulletAppState.getPhysicsSpace().remove(enemy.getControl(RigidBodyControl.class));
            rootNode.detachChild(enemy);
        }

        for (Spatial bullet : new ArrayList<>(rootNode.getChildren())) {
            if ("bullet".equals(bullet.getName())) {
                Iterator<Geometry> enemyIter = targetBoxes.iterator();
                while (enemyIter.hasNext()) {
                    Geometry enemy = enemyIter.next();
                    if (bullet.getWorldTranslation().distance(enemy.getWorldTranslation()) < 1f) {
                        addScore(2);
                        bulletAppState.getPhysicsSpace().remove(enemy.getControl(RigidBodyControl.class));
                        bulletAppState.getPhysicsSpace().remove(bullet.getControl(RigidBodyControl.class));
                        rootNode.detachChild(enemy);
                        rootNode.detachChild(bullet);
                        enemyIter.remove();
                        break;
                    }
                }
            }
        }

        updateHUD();
    }
}