package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapFont;
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
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class Main extends SimpleApplication {

    private BulletAppState bulletAppState;
    private Node tower;

    // Listas para controlar las cajas
    private ArrayList<Geometry> activeBoxes = new ArrayList<>(); // cajas azules en la plataforma
    private ArrayList<Geometry> targetBoxes = new ArrayList<>(); // cajas rojas alrededor

    // Para el score
    private BitmapText scoreText;
    private int score = 0;

    private Random rand = new Random();

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Cámara y luz
        flyCam.setMoveSpeed(30);
        setUpLight();

        // Física
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        // Piso y torre
        createFloor();
        createTower();

        // Cajas iniciales azules en la plataforma
        for (int i = 0; i < 2; i++) {
            spawnBoxOnPlatform();
        }

        // Cajas rojas alrededor
        createTargetBoxes();

        // Controles
        initKeys();

        // Score UI
        initScoreText();
    }

    private void setUpLight() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f));
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        rootNode.addLight(new DirectionalLight(new Vector3f(0.5f, -0.5f, 0.5f)));
    }

    private void createFloor() {
        Box floorBox = new Box(50, 0.1f, 50);
        Geometry floorGeo = new Geometry("Floor", floorBox);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Gray);
        floorGeo.setMaterial(mat);
        floorGeo.setLocalTranslation(0, -0.1f, 0);

        RigidBodyControl floorPhy = new RigidBodyControl(0);
        floorGeo.addControl(floorPhy);
        bulletAppState.getPhysicsSpace().add(floorPhy);

        rootNode.attachChild(floorGeo);
    }

    private void createTower() {
        Box towerBase = new Box(2, 0.2f, 2);
        Geometry towerGeo = new Geometry("Tower Base", towerBase);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.DarkGray);
        towerGeo.setMaterial(mat);
        towerGeo.setLocalTranslation(0, 0.2f, 0);

        Box platform = new Box(1.5f, 0.1f, 1.5f);
        Geometry platformGeo = new Geometry("Platform", platform);
        Material platformMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        platformMat.setColor("Color", ColorRGBA.LightGray);
        platformGeo.setMaterial(platformMat);
        platformGeo.setLocalTranslation(0, 6, 0);

        Box pole = new Box(0.3f, 3f, 0.3f);
        Geometry poleGeo = new Geometry("Tower Pole", pole);
        poleGeo.setMaterial(mat);
        poleGeo.setLocalTranslation(0, 3, 0);

        tower = new Node("Tower");
        tower.attachChild(towerGeo);
        tower.attachChild(poleGeo);
        tower.attachChild(platformGeo);

        RigidBodyControl towerPhy = new RigidBodyControl(0);
        towerGeo.addControl(towerPhy);
        bulletAppState.getPhysicsSpace().add(towerPhy);

        rootNode.attachChild(tower);

        cam.setLocation(new Vector3f(0, 7, 5));
        cam.lookAt(tower.getLocalTranslation().add(0, 6, 0), Vector3f.UNIT_Y);
    }

    private void createTargetBoxes() {
        Material boxMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", ColorRGBA.Red);

        int numBoxes = 12;
        float radius = 20;

        for (int i = 0; i < numBoxes; i++) {
            float angle = (float) (i * (2 * Math.PI / numBoxes));
            float x = (float) (radius * Math.cos(angle));
            float z = (float) (radius * Math.sin(angle));

            Box boxShape = new Box(1, 1, 1);
            Geometry boxGeo = new Geometry("Target Box " + i, boxShape);
            boxGeo.setMaterial(boxMat);
            boxGeo.setLocalTranslation(x, 1, z);

            RigidBodyControl boxPhy = new RigidBodyControl(2);
            boxGeo.addControl(boxPhy);
            bulletAppState.getPhysicsSpace().add(boxPhy);

            rootNode.attachChild(boxGeo);
            targetBoxes.add(boxGeo);
        }
    }

    private void spawnBoxOnPlatform() {
        Material blueMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        blueMat.setColor("Color", ColorRGBA.Blue);

        Box boxShape = new Box(0.5f, 0.5f, 0.5f);
        Geometry boxGeo = new Geometry("Platform Box", boxShape);
        boxGeo.setMaterial(blueMat);

        // Posición aleatoria dentro de la plataforma (1.5f x 1.5f)
        float x = (rand.nextFloat() * 3f) - 1.5f;
        float z = (rand.nextFloat() * 3f) - 1.5f;
        float y = 6.5f; // ligeramente arriba de la plataforma para evitar solapamientos

        boxGeo.setLocalTranslation(x, y, z);

        RigidBodyControl boxPhy = new RigidBodyControl(1);
        boxGeo.addControl(boxPhy);
        bulletAppState.getPhysicsSpace().add(boxPhy);

        rootNode.attachChild(boxGeo);
        activeBoxes.add(boxGeo);
    }

    private void initKeys() {
        inputManager.addMapping("Shoot", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(actionListener, "Shoot");
    }

    private ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Shoot") && !isPressed) {
                shootBullet();
            }
        }
    };

    private void shootBullet() {
        Sphere sphere = new Sphere(16, 16, 0.2f);
        Geometry bullet = new Geometry("bullet", sphere);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Yellow);
        bullet.setMaterial(mat);

        bullet.setLocalTranslation(cam.getLocation().add(cam.getDirection().mult(2)));

        RigidBodyControl bulletPhy = new RigidBodyControl(1);
        bullet.addControl(bulletPhy);
        bulletPhy.setLinearVelocity(cam.getDirection().mult(50));
        bulletPhy.setGravity(Vector3f.ZERO);

        rootNode.attachChild(bullet);
        bulletAppState.getPhysicsSpace().add(bulletPhy);
    }

    private void initScoreText() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        scoreText = new BitmapText(guiFont, false);
        scoreText.setSize(guiFont.getCharSet().getRenderedSize());
        scoreText.setColor(ColorRGBA.White);
        scoreText.setText("Score: 0");
        scoreText.setLocalTranslation(10, 30, 0); // esquina inferior izquierda (y=30 para que se vea desde abajo)
        guiNode.attachChild(scoreText);
    }

    private void addScore(int points) {
        score += points;
        scoreText.setText("Score: " + score);
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Evitar ConcurrentModificationException con listas auxiliares

        ArrayList<Geometry> toRemoveActive = new ArrayList<>();
        for (Geometry box : activeBoxes) {
            if (box.getLocalTranslation().y < 0) {
                toRemoveActive.add(box);
            }
        }
        for (Geometry box : toRemoveActive) {
            activeBoxes.remove(box);
            bulletAppState.getPhysicsSpace().remove(box.getControl(RigidBodyControl.class));
            rootNode.detachChild(box);
            addScore(1);
            spawnBoxOnPlatform();
            spawnBoxOnPlatform();
        }

        ArrayList<Geometry> toRemoveTarget = new ArrayList<>();
        for (Geometry box : targetBoxes) {
            if (box.getLocalTranslation().y < 0) {
                toRemoveTarget.add(box);
            }
        }
        for (Geometry box : toRemoveTarget) {
            targetBoxes.remove(box);
            bulletAppState.getPhysicsSpace().remove(box.getControl(RigidBodyControl.class));
            rootNode.detachChild(box);
            addScore(2);
        }
    }
}