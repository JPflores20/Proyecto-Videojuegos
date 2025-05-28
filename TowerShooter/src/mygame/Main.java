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
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;
import com.jme3.terrain.heightmap.HeightMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class Main extends SimpleApplication {

    private BulletAppState bulletAppState;
    private Node tower;
    private TerrainQuad terrain;

    private ArrayList<Geometry> activeBoxes = new ArrayList<>();
    private ArrayList<Geometry> targetBoxes = new ArrayList<>();

    private HashMap<Geometry, Integer> enemyHealth = new HashMap<>();

    private BitmapText scoreText, livesText, enemyText, gameOverText;
    private int score = 0;
    private int lives = 3;
    private boolean isGameOver = false;

    private Random rand = new Random();
    private float shootCooldown = 0.15f;
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

        // Inicialización correcta de BulletAppState
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        // Configurar gravedad después de adjuntar
        bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, -9.8f, 0));

        createFloor();
        createTower();
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
        Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/grand_mountain.png");
        heightMapImage.setWrap(WrapMode.Repeat);
        HeightMap heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightmap.load();

        terrain = new TerrainQuad("myTerrain", 65, 513, heightmap.getHeightMap());

        Material mat = new Material(assetManager, "Common/MatDefs/Terrain/Terrain.j3md");
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        mat.setTexture("Tex1", grass);
        mat.setFloat("Tex1Scale", 64f);

        terrain.setMaterial(mat);
        terrain.setLocalTranslation(0, 0, 0);
        terrain.setLocalScale(2f, 1f, 2f);

        RigidBodyControl terrainControl = new RigidBodyControl(0);
        terrain.addControl(terrainControl); // Primero añadir control
        terrainControl.setFriction(0.8f); // Luego configurar propiedades
        bulletAppState.getPhysicsSpace().add(terrainControl);
        rootNode.attachChild(terrain);
    }

    private void createTower() {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.DarkGray);
        mat.setColor("Specular", ColorRGBA.White);
        mat.setFloat("Shininess", 64f);

        float terrainHeight = terrain.getHeight(new Vector2f(0, 0));

        // Base de la torre
        Geometry base = new Geometry("Base", new Box(2, 0.2f, 2));
        base.setMaterial(mat);
        base.setLocalTranslation(0, terrainHeight + 0.2f, 0);

        // Plataforma
        Geometry platform = new Geometry("Platform", new Box(3f, 0.1f, 3f));
        platform.setMaterial(mat);
        platform.setLocalTranslation(0, terrainHeight + 6, 0);

        // Poste central
        Geometry pole = new Geometry("Pole", new Box(0.3f, 3f, 0.3f));
        pole.setMaterial(mat);
        pole.setLocalTranslation(0, terrainHeight + 3, 0);

        // Columnas alrededor
        Material columnMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        columnMat.setBoolean("UseMaterialColors", true);
        columnMat.setColor("Diffuse", ColorRGBA.LightGray);
        columnMat.setColor("Specular", ColorRGBA.White);
        columnMat.setFloat("Shininess", 32f);

        Node columns = new Node("Columns");
        float colHeight = 3f;
        float colRadius = 0.15f;
        float distFromCenter = 1.2f;
        for (int i = 0; i < 4; i++) {
            Geometry column = new Geometry("Column" + i, new Box(colRadius, colHeight, colRadius));
            column.setMaterial(columnMat);
            float angle = i * (float)(Math.PI / 2);
            float x = distFromCenter * (float)Math.cos(angle);
            float z = distFromCenter * (float)Math.sin(angle);
            column.setLocalTranslation(x, terrainHeight + colHeight, z);
            columns.attachChild(column);
        }

        tower = new Node("Tower");
        tower.attachChild(base);
        tower.attachChild(platform);
        tower.attachChild(pole);
        tower.attachChild(columns);

        RigidBodyControl basePhysics = new RigidBodyControl(0);
        base.addControl(basePhysics); // Primero añadir
        basePhysics.setFriction(0.7f); // Luego configurar
        basePhysics.setRestitution(0.1f);
        bulletAppState.getPhysicsSpace().add(basePhysics);
        rootNode.attachChild(tower);

        cam.setLocation(new Vector3f(0, terrainHeight + 7, 5));
        cam.lookAt(tower.getLocalTranslation().add(0, 6, 0), Vector3f.UNIT_Y);
    }

    private void createTargetBoxes() {
        Material redMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        redMat.setColor("Color", ColorRGBA.Red);
        int numBoxes = 12;
        float minDistance = 90f;
        float maxDistance = 120f;

        for (int i = 0; i < numBoxes; i++) {
            Vector3f position;
            float terrainHeight;
            do {
                float x = rand.nextFloat() * (maxDistance * 2) - maxDistance;
                float z = rand.nextFloat() * (maxDistance * 2) - maxDistance;
                terrainHeight = terrain.getHeight(new Vector2f(x, z));
                position = new Vector3f(x, terrainHeight + 1, z);
            } while (position.distance(Vector3f.ZERO) < minDistance);

            Geometry box = new Geometry("Target Box", new Box(1, 1, 1));
            box.setMaterial(redMat);
            box.setLocalTranslation(position);
            
            RigidBodyControl enemyPhysics = new RigidBodyControl(2);
            box.addControl(enemyPhysics); // Primero añadir
            enemyPhysics.setFriction(0.6f); // Luego configurar
            enemyPhysics.setRestitution(0.2f);
            
            bulletAppState.getPhysicsSpace().add(enemyPhysics);
            rootNode.attachChild(box);
            targetBoxes.add(box);
            enemyHealth.put(box, 1);
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

        RigidBodyControl bulletPhysics = new RigidBodyControl(1);
        bullet.addControl(bulletPhysics); // Primero añadir
        bulletPhysics.setLinearVelocity(cam.getDirection().mult(50));
        bulletPhysics.setGravity(Vector3f.ZERO);

        rootNode.attachChild(bullet);
        bulletAppState.getPhysicsSpace().add(bulletPhysics);
        activeBoxes.add(bullet);
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
        enemyHealth.clear();

        for (Spatial bullet : new ArrayList<>(rootNode.getChildren())) {
            if (bullet.getName() != null && bullet.getName().equals("bullet")) {
                bulletAppState.getPhysicsSpace().remove(bullet.getControl(RigidBodyControl.class));
                rootNode.detachChild(bullet);
            }
        }

        createTargetBoxes();
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

        // Remover balas caídas
        ArrayList<Geometry> toRemoveBullets = new ArrayList<>();
        for (Geometry bullet : activeBoxes) {
            if (bullet.getLocalTranslation().y < -10) {
                toRemoveBullets.add(bullet);
            }
        }
        for (Geometry bullet : toRemoveBullets) {
            activeBoxes.remove(bullet);
            bulletAppState.getPhysicsSpace().remove(bullet.getControl(RigidBodyControl.class));
            rootNode.detachChild(bullet);
        }

        // Mover enemigos hacia la torre
        ArrayList<Geometry> toRemoveEnemies = new ArrayList<>();
        for (Geometry enemy : targetBoxes) {
            Vector3f dir = tower.getWorldTranslation().subtract(enemy.getWorldTranslation()).normalize();
            enemy.getControl(RigidBodyControl.class).setLinearVelocity(dir.mult(enemySpeed));
            if (enemy.getWorldTranslation().distance(tower.getWorldTranslation()) < 2f) {
                toRemoveEnemies.add(enemy);
                loseLife();
            }
        }
        for (Geometry enemy : toRemoveEnemies) {
            targetBoxes.remove(enemy);
            enemyHealth.remove(enemy);
            bulletAppState.getPhysicsSpace().remove(enemy.getControl(RigidBodyControl.class));
            rootNode.detachChild(enemy);
        }

        checkCollisions();
        updateHUD();

        if (targetBoxes.isEmpty() && !isGameOver) {
            createTargetBoxes();
        }
    }

    private void checkCollisions() {
        Iterator<Geometry> bulletIterator = activeBoxes.iterator();
        while (bulletIterator.hasNext()) {
            Geometry bullet = bulletIterator.next();
            Iterator<Geometry> enemyIterator = targetBoxes.iterator();
            while (enemyIterator.hasNext()) {
                Geometry enemy = enemyIterator.next();
                if (bullet.getWorldBound().intersects(enemy.getWorldBound())) {
                    int health = enemyHealth.getOrDefault(enemy, 1);
                    health--;
                    if (health <= 0) {
                        bulletAppState.getPhysicsSpace().remove(enemy.getControl(RigidBodyControl.class));
                        rootNode.detachChild(enemy);
                        enemyIterator.remove();
                        enemyHealth.remove(enemy);
                        addScore(10);
                    } else {
                        enemyHealth.put(enemy, health);
                    }

                    bulletAppState.getPhysicsSpace().remove(bullet.getControl(RigidBodyControl.class));
                    rootNode.detachChild(bullet);
                    bulletIterator.remove();
                    return;
                }
            }
        }
    }
}