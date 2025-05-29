package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
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
import com.jme3.terrain.heightmap.HeightMap;
import com.jme3.math.FastMath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class Main extends SimpleApplication {

    private BulletAppState bulletAppState;
    private Node tower;
    private TerrainQuad terrain;

    private ArrayList<Spatial> activeBullets = new ArrayList<>();
    private ArrayList<Node> enemyCowboys = new ArrayList<>();

    private HashMap<Node, Integer> enemyHealth = new HashMap<>();
    private HashMap<Node, HealthBar> enemyHealthBars = new HashMap<>();
    private Material healthBarBgMat;
    private Material healthBarFillMat;

    // Campos para el HUD
    private Geometry scoreBackground;
    private Geometry livesBackground;
    private Geometry enemiesBackground;
    private BitmapText scoreLabelText, livesLabelText, enemiesLabelText;
    private BitmapText scoreValueText, livesValueText, enemiesValueText;
    private BitmapText gameOverText;
    private float hudElementWidth = 180f;
    private float hudElementHeight = 40f;
    private float hudPadding = 15f;
    
    private int score = 0;
    private int lives = 3;
    private boolean isGameOver = false;
    private int currentLevel = 1;
    private BitmapText levelCompleteText;
    private float levelCompleteTimer = 0f;
    private boolean showingLevelComplete = false;

    private Random rand = new Random();
    private float shootCooldown = 0.15f;
    private float timeSinceLastShot = 0;
    private float enemySpeed = 3f;

    private boolean isShooting = false;
    private float gameOverTimer = 0f;

    private enum MovementPattern {
        SINUSOIDAL, SPIRAL, RANDOM, ZIGZAG, CIRCLE_APPROACH, RETREAT_AND_ADVANCE
    }
    private HashMap<Node, MovementPattern> enemyMovementPatterns = new HashMap<>();
    private HashMap<Node, Float> enemyMovementTimers = new HashMap<>();

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
        bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, -9.8f, 0));

        createFloor();
        createTower();
        
        initHealthBarMaterials();
        createEnemyCowboys();

        initKeys();
        initHUD();
    }

    private void initHealthBarMaterials() {
        healthBarBgMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        healthBarBgMat.setColor("Color", ColorRGBA.DarkGray);
        
        healthBarFillMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        healthBarFillMat.setColor("Color", ColorRGBA.Red);
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
        terrain.addControl(terrainControl);
        terrainControl.setFriction(0.8f);
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

        Geometry base = new Geometry("Base", new Box(2, 0.2f, 2));
        base.setMaterial(mat);
        base.setLocalTranslation(0, terrainHeight + 0.2f, 0);

        Geometry platform = new Geometry("Platform", new Box(3f, 0.1f, 3f));
        platform.setMaterial(mat);
        platform.setLocalTranslation(0, terrainHeight + 6, 0);

        Geometry pole = new Geometry("Pole", new Box(0.3f, 3f, 0.3f));
        pole.setMaterial(mat);
        pole.setLocalTranslation(0, terrainHeight + 3, 0);

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
        base.addControl(basePhysics);
        basePhysics.setFriction(0.7f);
        basePhysics.setRestitution(0.1f);
        bulletAppState.getPhysicsSpace().add(basePhysics);
        rootNode.attachChild(tower);

        cam.setLocation(new Vector3f(0, terrainHeight + 7, 5));
        cam.lookAt(tower.getLocalTranslation().add(0, 6, 0), Vector3f.UNIT_Y);
    }

    private void createEnemyCowboys() {
        int numCowboys = 3 + (currentLevel - 1) * 3;
        float minDistance = 90f;
        float maxDistance = 120f;
        int maxHealth = 3;

        MovementPattern[] patterns = MovementPattern.values();
        
        for (int i = 0; i < numCowboys; i++) {
            Vector3f position;
            float terrainHeight;
            do {
                float x = rand.nextFloat() * (maxDistance * 2) - maxDistance;
                float z = rand.nextFloat() * (maxDistance * 2) - maxDistance;
                terrainHeight = terrain.getHeight(new Vector2f(x, z));
                position = new Vector3f(x, terrainHeight + 1, z);
            } while (position.distance(Vector3f.ZERO) < minDistance);

            Spatial cowboyModel = assetManager.loadModel("Models/Cowboy/Cowboy.j3o");
            cowboyModel.setLocalTranslation(0, 0, 0);
            cowboyModel.scale(0.5f);

            Node cowboyNode = new Node("Cowboy_" + i);
            cowboyNode.setLocalTranslation(position);
            cowboyNode.attachChild(cowboyModel);
            
            HealthBar healthBar = new HealthBar(healthBarBgMat, healthBarFillMat);
            cowboyNode.attachChild(healthBar.getNode());
            enemyHealthBars.put(cowboyNode, healthBar);

            RigidBodyControl physics = new RigidBodyControl(new BoxCollisionShape(new Vector3f(1, 1, 1)), 2);
            cowboyNode.addControl(physics);
            physics.setPhysicsLocation(position);
            physics.setFriction(0.6f);
            physics.setRestitution(0.2f);
            physics.setKinematic(false);

            bulletAppState.getPhysicsSpace().add(physics);
            rootNode.attachChild(cowboyNode);
            
            enemyCowboys.add(cowboyNode);
            enemyHealth.put(cowboyNode, maxHealth);
            
            MovementPattern pattern = patterns[i % patterns.length];
            enemyMovementPatterns.put(cowboyNode, pattern);
            enemyMovementTimers.put(cowboyNode, rand.nextFloat() * 5f);
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
        Spatial bulletModel = assetManager.loadModel("Models/Bullet/Bullet.j3o");
        bulletModel.setLocalTranslation(0, 0, 0);
        bulletModel.rotate(0, -FastMath.HALF_PI, 0);

        Node bulletNode = new Node("bullet");
        bulletNode.attachChild(bulletModel);

        Vector3f spawnPosition = cam.getLocation().add(cam.getDirection().mult(2));
        bulletNode.setLocalTranslation(spawnPosition);
        bulletNode.setLocalScale(0.5f);

        Vector3f direction = cam.getDirection().normalize();
        bulletNode.lookAt(spawnPosition.add(direction), Vector3f.UNIT_Y);

        RigidBodyControl bulletPhysics = new RigidBodyControl(1);
        bulletNode.addControl(bulletPhysics);
        bulletPhysics.setLinearVelocity(direction.mult(50));
        bulletPhysics.setGravity(Vector3f.ZERO);

        rootNode.attachChild(bulletNode);
        bulletAppState.getPhysicsSpace().add(bulletPhysics);
        activeBullets.add(bulletNode);
    }

    private void initHUD() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        
        Material hudBgMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        hudBgMat.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.1f, 0.7f));
        
        Material hudHighlightMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        hudHighlightMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.1f, 0.8f));
        
        createScoreDisplay(hudBgMat, hudHighlightMat);
        createLivesDisplay(hudBgMat, hudHighlightMat);
        createEnemiesDisplay(hudBgMat, hudHighlightMat);
        
        gameOverText = new BitmapText(guiFont, false);
        gameOverText.setSize(guiFont.getCharSet().getRenderedSize() * 2.5f);
        gameOverText.setColor(new ColorRGBA(1, 0.2f, 0.2f, 1));
        gameOverText.setText("GAME OVER\nReiniciando...");
        
        levelCompleteText = new BitmapText(guiFont, false);
        levelCompleteText.setSize(guiFont.getCharSet().getRenderedSize() * 3f);
        levelCompleteText.setColor(new ColorRGBA(0.2f, 1, 0.2f, 1));
    }

    private void createScoreDisplay(Material bgMat, Material highlightMat) {
        Box scoreBox = new Box(hudElementWidth/2, hudElementHeight/2, 0);
        scoreBackground = new Geometry("ScoreBackground", scoreBox);
        scoreBackground.setMaterial(bgMat);
        scoreBackground.setLocalTranslation(hudElementWidth/2 + hudPadding, 
            settings.getHeight() - hudElementHeight/2 - hudPadding, 0);
        guiNode.attachChild(scoreBackground);
        
        scoreLabelText = new BitmapText(guiFont, false);
        scoreLabelText.setSize(24);
        scoreLabelText.setColor(new ColorRGBA(1, 1, 0.8f, 1));
        scoreLabelText.setText("SCORE:");
        scoreLabelText.setLocalTranslation(hudPadding * 2, 
            settings.getHeight() - hudPadding - hudElementHeight/4, 0);
        guiNode.attachChild(scoreLabelText);
        
        scoreValueText = new BitmapText(guiFont, false);
        scoreValueText.setSize(28);
        scoreValueText.setColor(ColorRGBA.Yellow);
        scoreValueText.setText("0");
        scoreValueText.setLocalTranslation(hudElementWidth - hudPadding * 2 - 30, 
            settings.getHeight() - hudPadding - hudElementHeight/4, 0);
        guiNode.attachChild(scoreValueText);
    }

    private void createLivesDisplay(Material bgMat, Material highlightMat) {
        Box livesBox = new Box(hudElementWidth/2, hudElementHeight/2, 0);
        livesBackground = new Geometry("LivesBackground", livesBox);
        livesBackground.setMaterial(bgMat);
        livesBackground.setLocalTranslation(hudElementWidth/2 + hudPadding, 
            settings.getHeight() - hudElementHeight - hudPadding * 2 - hudElementHeight/2, 0);
        guiNode.attachChild(livesBackground);
        
        livesLabelText = new BitmapText(guiFont, false);
        livesLabelText.setSize(24);
        livesLabelText.setColor(new ColorRGBA(1, 0.8f, 0.8f, 1));
        livesLabelText.setText("LIVES:");
        livesLabelText.setLocalTranslation(hudPadding * 2, 
            settings.getHeight() - hudPadding * 2 - hudElementHeight - hudElementHeight/4, 0);
        guiNode.attachChild(livesLabelText);
        
        livesValueText = new BitmapText(guiFont, false);
        livesValueText.setSize(28);
        livesValueText.setColor(new ColorRGBA(1, 0.6f, 0.6f, 1));
        livesValueText.setText("3");
        livesValueText.setLocalTranslation(hudElementWidth - hudPadding * 2 - 30, 
            settings.getHeight() - hudPadding * 2 - hudElementHeight - hudElementHeight/4, 0);
        guiNode.attachChild(livesValueText);
    }

    private void createEnemiesDisplay(Material bgMat, Material highlightMat) {
        Box enemiesBox = new Box(hudElementWidth/2, hudElementHeight/2, 0);
        enemiesBackground = new Geometry("EnemiesBackground", enemiesBox);
        enemiesBackground.setMaterial(bgMat);
        enemiesBackground.setLocalTranslation(hudElementWidth/2 + hudPadding, 
            settings.getHeight() - hudElementHeight * 2 - hudPadding * 3 - hudElementHeight/2, 0);
        guiNode.attachChild(enemiesBackground);
        
        enemiesLabelText = new BitmapText(guiFont, false);
        enemiesLabelText.setSize(24);
        enemiesLabelText.setColor(new ColorRGBA(0.8f, 1, 0.8f, 1));
        enemiesLabelText.setText("ENEMIES:");
        enemiesLabelText.setLocalTranslation(hudPadding * 2, 
            settings.getHeight() - hudPadding * 3 - hudElementHeight * 2 - hudElementHeight/4, 0);
        guiNode.attachChild(enemiesLabelText);
        
        enemiesValueText = new BitmapText(guiFont, false);
        enemiesValueText.setSize(28);
        enemiesValueText.setColor(new ColorRGBA(0.6f, 1, 0.6f, 1));
        enemiesValueText.setText("12");
        enemiesValueText.setLocalTranslation(hudElementWidth - hudPadding * 2 - 30, 
            settings.getHeight() - hudPadding * 3 - hudElementHeight * 2 - hudElementHeight/4, 0);
        guiNode.attachChild(enemiesValueText);
    }

    private void updateHUD() {
        scoreValueText.setText(String.format("%,d", score));
        livesValueText.setText(String.valueOf(lives));
        enemiesValueText.setText(String.valueOf(enemyCowboys.size()));
        
        updateScoreDisplay();
        updateLivesDisplay();
    }

    private void updateScoreDisplay() {
        if (score > 0 && score % 100 == 0) {
            scoreBackground.getMaterial().setColor("Color", new ColorRGBA(0.3f, 0.3f, 0, 0.9f));
            scoreValueText.setColor(ColorRGBA.Orange);
            scoreValueText.setSize(32);
        } else {
            scoreBackground.getMaterial().setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.1f, 0.7f));
            scoreValueText.setColor(ColorRGBA.Yellow);
            scoreValueText.setSize(28);
        }
    }

    private void updateLivesDisplay() {
        if (lives <= 1) {
            livesBackground.getMaterial().setColor("Color", new ColorRGBA(0.3f, 0, 0, 0.9f));
            livesValueText.setColor(ColorRGBA.Red);
            livesValueText.setSize(32);
        } else {
            livesBackground.getMaterial().setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.1f, 0.7f));
            livesValueText.setColor(new ColorRGBA(1, 0.6f, 0.6f, 1));
            livesValueText.setSize(28);
        }
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
            gameOverText.setLocalTranslation(
                settings.getWidth()/2 - gameOverText.getLineWidth()/2, 
                settings.getHeight()/2 + gameOverText.getLineHeight(), 
                0
            );
            guiNode.attachChild(gameOverText);
        }
    }

    private void showLevelComplete() {
        levelCompleteText.setText("LEVEL " + currentLevel + " COMPLETE!");
        levelCompleteText.setLocalTranslation(
            settings.getWidth()/2 - levelCompleteText.getLineWidth()/2, 
            settings.getHeight()/2 + levelCompleteText.getLineHeight(), 
            0
        );
        guiNode.attachChild(levelCompleteText);
        showingLevelComplete = true;
        levelCompleteTimer = 0f;
    }

    private void resetGame() {
        score = 0;
        lives = 3;
        currentLevel = 1;
        enemySpeed = 3f;
        isGameOver = false;
        gameOverTimer = 0;
        guiNode.detachChild(gameOverText);
        guiNode.detachChild(levelCompleteText);
        showingLevelComplete = false;

        for (Spatial bullet : activeBullets) {
            bulletAppState.getPhysicsSpace().remove(bullet.getControl(RigidBodyControl.class));
            rootNode.detachChild(bullet);
        }
        activeBullets.clear();

        for (Node cowboy : enemyCowboys) {
            bulletAppState.getPhysicsSpace().remove(cowboy.getControl(RigidBodyControl.class));
            rootNode.detachChild(cowboy);
        }
        enemyCowboys.clear();
        enemyHealth.clear();
        enemyHealthBars.clear();
        enemyMovementPatterns.clear();
        enemyMovementTimers.clear();

        createEnemyCowboys();
        updateHUD();
    }

    private void updateEnemyMovement(float tpf) {
        ArrayList<Node> toRemoveCowboys = new ArrayList<>();
        for (Node cowboy : enemyCowboys) {
            RigidBodyControl physics = cowboy.getControl(RigidBodyControl.class);
            if (physics != null) {
                Vector3f currentPos = physics.getPhysicsLocation();
                Vector3f towerPos = tower.getWorldTranslation();
                Vector3f direction = towerPos.subtract(currentPos).normalize();
                
                cowboy.getChild(0).lookAt(towerPos, Vector3f.UNIT_Y);
                
                MovementPattern pattern = enemyMovementPatterns.get(cowboy);
                float timer = enemyMovementTimers.get(cowboy);
                timer += tpf;
                enemyMovementTimers.put(cowboy, timer);
                
                Vector3f horizontalVelocity;
                float distanceToTower = currentPos.distance(towerPos);
                
                switch (pattern) {
                    case SINUSOIDAL:
                        float amplitude = 5f * (distanceToTower / 120f);
                        float frequency = 2f;
                        float lateralMovement = amplitude * FastMath.sin(timer * frequency);
                        
                        Vector3f perpendicular = new Vector3f(-direction.z, 0, direction.x).normalize();
                        
                        float speedFactor = 0.5f + (distanceToTower / 120f) * 0.5f;
                        horizontalVelocity = direction.mult(enemySpeed * speedFactor)
                                           .add(perpendicular.mult(lateralMovement));
                        break;
                        
                    case SPIRAL:
                        float spiralRadius = 8f * (distanceToTower / 120f);
                        float spiralSpeed = 3f;
                        
                        Vector3f spiralOffset = new Vector3f(
                            FastMath.cos(timer * spiralSpeed) * spiralRadius,
                            0,
                            FastMath.sin(timer * spiralSpeed) * spiralRadius
                        );
                        
                        float spiralSpeedFactor = 1f + (1 - distanceToTower / 120f) * 2f;
                        horizontalVelocity = direction.mult(enemySpeed * spiralSpeedFactor)
                                           .add(spiralOffset.normalize().mult(2f));
                        break;
                        
                    case RANDOM:
                        if (timer > 1f) {
                            timer = 0f;
                            enemyMovementTimers.put(cowboy, timer);
                            
                            float randomness = 0.7f * (distanceToTower / 120f);
                            Vector3f randomOffset = new Vector3f(
                                rand.nextFloat() * 2 - 1,
                                0,
                                rand.nextFloat() * 2 - 1
                            ).normalize().mult(randomness);
                            
                            direction = direction.add(randomOffset).normalize();
                        }
                        
                        float randomSpeedFactor = 0.7f + rand.nextFloat() * 0.6f;
                        horizontalVelocity = direction.mult(enemySpeed * randomSpeedFactor);
                        break;
                        
                    case ZIGZAG:
                        if (timer > 1.5f) {
                            timer = 0f;
                            enemyMovementTimers.put(cowboy, timer);
                            
                            Vector3f zigzagOffset = new Vector3f(
                                rand.nextFloat() > 0.5f ? 1 : -1,
                                0,
                                rand.nextFloat() > 0.5f ? 1 : -1
                            ).normalize().mult(3f);
                            
                            direction = direction.add(zigzagOffset).normalize();
                        }
                        
                        horizontalVelocity = direction.mult(enemySpeed * 1.2f);
                        break;
                        
                    case CIRCLE_APPROACH:
                        float circleRadius = 10f * (distanceToTower / 120f);
                        float circleSpeed = 2f;
                        
                        Vector3f circleOffset = new Vector3f(
                            FastMath.cos(timer * circleSpeed) * circleRadius,
                            0,
                            FastMath.sin(timer * circleSpeed) * circleRadius
                        );
                        
                        float approachSpeed = enemySpeed * 0.7f;
                        float circleComponentSpeed = enemySpeed * 0.5f;
                        horizontalVelocity = direction.mult(approachSpeed)
                                               .add(circleOffset.normalize().mult(circleComponentSpeed));
                        break;
                        
                    case RETREAT_AND_ADVANCE:
                        if (timer > 2f) {
                            timer = 0f;
                            enemyMovementTimers.put(cowboy, timer);
                            
                            if (rand.nextFloat() < 0.3f) {
                                direction = direction.mult(-1);
                            } else {
                                direction = direction.mult(1.5f);
                            }
                        }
                        
                        horizontalVelocity = direction.mult(enemySpeed * 0.8f);
                        break;
                        
                    default:
                        horizontalVelocity = direction.mult(enemySpeed);
                        break;
                }
                
                float verticalMovement = FastMath.sin(timer * 3f) * 0.3f;
                physics.setLinearVelocity(new Vector3f(
                    horizontalVelocity.x,
                    verticalMovement,
                    horizontalVelocity.z
                ));
                
                if (distanceToTower < 5f) {
                    toRemoveCowboys.add(cowboy);
                    loseLife();
                }
            }
        }
        
        for (Node cowboy : toRemoveCowboys) {
            removeEnemy(cowboy);
        }
    }

    private void removeEnemy(Node cowboy) {
        enemyCowboys.remove(cowboy);
        enemyHealth.remove(cowboy);
        enemyHealthBars.remove(cowboy);
        enemyMovementPatterns.remove(cowboy);
        enemyMovementTimers.remove(cowboy);
        bulletAppState.getPhysicsSpace().remove(cowboy.getControl(RigidBodyControl.class));
        rootNode.detachChild(cowboy);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (isGameOver) {
            gameOverTimer += tpf;
            if (gameOverTimer >= 5f) resetGame();
            return;
        }

        if (showingLevelComplete) {
            levelCompleteTimer += tpf;
            if (levelCompleteTimer >= 2f) {
                guiNode.detachChild(levelCompleteText);
                showingLevelComplete = false;
                createEnemyCowboys();
            }
            return;
        }

        timeSinceLastShot += tpf;
        if (isShooting && timeSinceLastShot >= shootCooldown) {
            shootBullet();
            timeSinceLastShot = 0;
        }

        ArrayList<Spatial> toRemoveBullets = new ArrayList<>();
        for (Spatial bullet : activeBullets) {
            if (bullet.getLocalTranslation().y < -10) {
                toRemoveBullets.add(bullet);
            }
        }
        for (Spatial bullet : toRemoveBullets) {
            activeBullets.remove(bullet);
            bulletAppState.getPhysicsSpace().remove(bullet.getControl(RigidBodyControl.class));
            rootNode.detachChild(bullet);
        }

        updateEnemyMovement(tpf);
        checkCollisions();
        updateHUD();

        if (enemyCowboys.isEmpty() && !isGameOver && !showingLevelComplete) {
            currentLevel++;
            showLevelComplete();
        }
    }

    private void checkCollisions() {
        Iterator<Spatial> bulletIterator = activeBullets.iterator();
        while (bulletIterator.hasNext()) {
            Spatial bullet = bulletIterator.next();
            Iterator<Node> cowboyIterator = enemyCowboys.iterator();
            while (cowboyIterator.hasNext()) {
                Node cowboy = cowboyIterator.next();
                if (bullet.getWorldBound().intersects(cowboy.getWorldBound())) {
                    int maxHealth = 3;
                    int health = enemyHealth.getOrDefault(cowboy, maxHealth);
                    health--;
                    
                    HealthBar healthBar = enemyHealthBars.get(cowboy);
                    if (healthBar != null) {
                        healthBar.update(health, maxHealth);
                    }
                    
                    if (health <= 0) {
                        bulletAppState.getPhysicsSpace().remove(cowboy.getControl(RigidBodyControl.class));
                        rootNode.detachChild(cowboy);
                        cowboyIterator.remove();
                        enemyHealth.remove(cowboy);
                        enemyHealthBars.remove(cowboy);
                        enemyMovementPatterns.remove(cowboy);
                        enemyMovementTimers.remove(cowboy);
                        addScore(10);
                    } else {
                        enemyHealth.put(cowboy, health);
                    }

                    bulletAppState.getPhysicsSpace().remove(bullet.getControl(RigidBodyControl.class));
                    rootNode.detachChild(bullet);
                    bulletIterator.remove();
                    break;
                }
            }
        }
        
        for (Node cowboy : enemyCowboys) {
            HealthBar healthBar = enemyHealthBars.get(cowboy);
            if (healthBar != null) {
                healthBar.getNode().lookAt(cam.getLocation(), Vector3f.UNIT_Y);
            }
        }
    }

    private class HealthBar {
        private Node barNode;
        private Geometry background;
        private Geometry healthFill;
        private float maxWidth = 2f;
        private float height = 0.2f;
        private float offsetY = 2f;

        public HealthBar(Material bgMat, Material fillMat) {
            background = new Geometry("HealthBarBG", new Box(maxWidth/2, height/2, 0.01f));
            background.setMaterial(bgMat);
            
            healthFill = new Geometry("HealthFill", new Box(maxWidth/2, height/2, 0.02f));
            healthFill.setMaterial(fillMat);
            
            barNode = new Node("HealthBar");
            barNode.attachChild(background);
            barNode.attachChild(healthFill);
            barNode.setLocalTranslation(0, offsetY, 0);
        }

        public void update(float currentHealth, float maxHealth) {
            float healthPercent = currentHealth / maxHealth;
            float currentWidth = maxWidth * healthPercent;
            
            healthFill.setLocalScale(healthPercent, 1, 1);
            healthFill.setLocalTranslation(-maxWidth/2 + currentWidth/2, 0, 0.01f);
        }

        public Node getNode() {
            return barNode;
        }
    }
}