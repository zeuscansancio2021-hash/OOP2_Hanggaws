import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;

// --- PROJECTILE CLASS ---
class Bullet {
    int x, y, speed = 10;
    int dirX, dirY;
    int size = 8;
    boolean isEnemyBullet; // Tracks bullet alignment

    public Bullet(int x, int y, String direction, boolean isEnemyBullet) {
        this.x = x;
        this.y = y;
        this.isEnemyBullet = isEnemyBullet;
        
        if (direction.equals("UP")) { dirX = 0; dirY = -speed; }
        else if (direction.equals("DOWN")) { dirX = 0; dirY = speed; }
        else if (direction.equals("LEFT")) { dirX = -speed; dirY = 0; }
        else { dirX = speed; dirY = 0; }
    }

    public void update() {
        x += dirX;
        y += dirY;
    }

    public void draw(Graphics g) {
        // Red-Orange for security threats, yellow for player
        g.setColor(isEnemyBullet ? new Color(255, 69, 0) : Color.YELLOW);
        g.fillOval(x, y, size, size);
    }

    public Rectangle getBounds() { return new Rectangle(x, y, size, size); }
}

// --- ENEMY / SECURITY CLASS ---
class Enemy {
    int x, y, width = 40, height = 40;
    int health = 3;
    boolean alive = true;
    boolean isSecurity = false;
    int shootCooldown = 30;

    public Enemy(int x, int y, boolean isSecurity) {
        this.x = x;
        this.y = y;
        this.isSecurity = isSecurity;
        if (isSecurity) {
            this.health = 4; // Security guards are tougher
        }
    }

    public void updateAI(Player player, ArrayList<Bullet> bullets) {
        if (!alive || !isSecurity) return;

        if (shootCooldown > 0) {
            shootCooldown--;
        } else {
            // Calculated directional targeting based on player alignment
            String shootDir = "DOWN";
            int dx = player.x - this.x;
            int dy = player.y - this.y;

            if (Math.abs(dx) > Math.abs(dy)) {
                shootDir = (dx > 0) ? "RIGHT" : "LEFT";
            } else {
                shootDir = (dy > 0) ? "DOWN" : "UP";
            }

            // Fire an enemy aligned projectile
            bullets.add(new Bullet(this.x + 16, this.y + 16, shootDir, true));
            shootCooldown = 50 + (int) (Math.random() * 40); // Randomized firing patterns
        }
    }

    public void draw(Graphics g) {
        if (!alive) return;
        
        // Security guards wear deep blue uniforms, town syndicate wears crimson red
        g.setColor(isSecurity ? new Color(25, 25, 112) : new Color(200, 50, 50)); 
        g.fillRect(x, y, width, height);
        
        // Elite visors
        g.setColor(isSecurity ? Color.CYAN : Color.BLACK);
        g.fillRect(x + 5, y + 10, 30, 5);
        
        // Health tracking graphics
        g.setColor(Color.RED);
        g.fillRect(x, y - 10, width, 5);
        g.setColor(Color.GREEN);
        int maxHP = isSecurity ? 4 : 3;
        g.fillRect(x, y - 10, (width * health) / maxHP, 5);
    }

    public Rectangle getBounds() { return new Rectangle(x, y, width, height); }
}

// --- BUILDING CLASS ---
class Building {
    int x, y, width, height;
    String name;
    Color color;
    Rectangle doorway;

    public Building(int x, int y, int width, int height, String name, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.name = name;
        this.color = color;
        this.doorway = new Rectangle(x + (width / 2) - 15, y + height - 5, 30, 12);
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(x, y, width, height);
        
        g.setColor(color.darker());
        g.fillRect(x, y, width, 15);

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(doorway.x, doorway.y, doorway.width, doorway.height);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(name, x + 15, y + 35);
    }

    public Rectangle getSolidBounds() { 
        return new Rectangle(x, y, width, height - 5); 
    }
}

// --- PLAYER CLASS ---
class Player {
    String name;
    int x, y, speed = 4;
    int width = 32, height = 32;
    Color color;
    String direction = "DOWN";
    
    // Combat mechanics
    int health = 10;
    int maxHealth = 10;
    int wallet = 0;

    public Player(String name, Color color, int x, int y) {
        this.name = name;
        this.color = color;
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(x, y, width, height);
        g.setColor(Color.WHITE);
        if (direction.equals("UP")) { g.fillRect(x + 4, y + 4, 6, 6); g.fillRect(x + 22, y + 4, 6, 6); }
        else if (direction.equals("DOWN")) { g.fillRect(x + 4, y + 22, 6, 6); g.fillRect(x + 22, y + 22, 6, 6); }
        else if (direction.equals("LEFT")) { g.fillRect(x + 4, y + 8, 6, 16); }
        else if (direction.equals("RIGHT")) { g.fillRect(x + 22, y + 8, 6, 16); }
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.drawString(name, x - 5, y - 8);
    }
    
    public Rectangle getBounds() { return new Rectangle(x, y, width, height); }
}

// --- MAIN GAMEPANEL ENGINE ---
class GamePanel extends JPanel implements ActionListener {
    private Player player;
    private ArrayList<Bullet> bullets = new ArrayList<>();
    private ArrayList<Enemy> townEnemies = new ArrayList<>();
    private ArrayList<Enemy> bankSecurity = new ArrayList<>();
    private ArrayList<Building> townBuildings = new ArrayList<>();
    
    private Timer gameLoop;
    private Set<Integer> activeKeys = new HashSet<>();
    private int shootCooldown = 0;

    // State configurations
    private enum LocationState { TOWN, HOUSE_INTERIOR, BANK_INTERIOR }
    private LocationState currentLocation = LocationState.TOWN;
    
    // Heist State configurations
    private Rectangle bankVault = new Rectangle(360, 100, 80, 60);
    private boolean vaultHasCash = true;
    private boolean alarmTriggered = false;

    private int returnTownX, returnTownY;
    private Rectangle interiorExitMat = new Rectangle(375, 520, 50, 40);

    public GamePanel(String name, Color choice) {
        this.player = new Player(name, choice, 200, 350);
        this.setFocusable(true);

        // Core structures setup
        townBuildings.add(new Building(120, 100, 140, 110, "MY HOUSE", new Color(139, 69, 19)));
        townBuildings.add(new Building(480, 80, 180, 130, "CITY BANK", new Color(112, 128, 144)));

        // Base maps syndicate targets
        townEnemies.add(new Enemy(150, 450, false));
        townEnemies.add(new Enemy(550, 400, false));

        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) { activeKeys.add(e.getKeyCode()); }
            public void keyReleased(KeyEvent e) { activeKeys.remove(e.getKeyCode()); }
        });

        gameLoop = new Timer(16, this);
        gameLoop.start();
    }

    private void drawTownOverworld(Graphics g) {
        g.setColor(new Color(34, 139, 34)); 
        g.fillRect(0, 0, getWidth(), getHeight());
        
        g.setColor(new Color(210, 180, 140));
        g.fillRect(0, 230, getWidth(), 60);
        g.fillRect(360, 230, 70, getHeight() - 230);

        for (Building b : townBuildings) b.draw(g);
        for (Enemy en : townEnemies) en.draw(g);
        for (Bullet b : bullets) b.draw(g);
    }

    private void drawHouseInterior(Graphics g) {
        g.setColor(new Color(222, 184, 135)); 
        g.fillRect(100, 50, 600, 500);
        
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 100, getHeight()); g.fillRect(700, 0, getWidth() - 700, getHeight());
        g.fillRect(100, 0, 600, 50); g.fillRect(100, 550, 600, getHeight() - 550);

        g.setColor(Color.RED);
        g.fillRect(interiorExitMat.x, interiorExitMat.y, interiorExitMat.width, interiorExitMat.height);
        
        g.setColor(new Color(100, 50, 10));
        g.fillRect(200, 150, 80, 60);
        g.setColor(Color.WHITE);
        g.drawString("Table", 220, 185);
    }

    private void drawBankInterior(Graphics g) {
        // Red flashing background style if alarm is tripped
        if (alarmTriggered && (System.currentTimeMillis() / 200) % 2 == 0) {
            g.setColor(new Color(255, 200, 200));
        } else {
            g.setColor(new Color(240, 248, 255));
        }
        g.fillRect(100, 50, 600, 500);
        
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 100, getHeight()); g.fillRect(700, 0, getWidth() - 700, getHeight());
        g.fillRect(100, 0, 600, 50); g.fillRect(100, 550, 600, getHeight() - 550);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(interiorExitMat.x, interiorExitMat.y, interiorExitMat.width, interiorExitMat.height);

        // Render Security Vault Component
        if (vaultHasCash) {
            g.setColor(new Color(218, 165, 32)); // Solid gold style
        } else {
            g.setColor(Color.GRAY); // Empty vault steel style
        }
        g.fillRect(bankVault.x, bankVault.y, bankVault.width, bankVault.height);
        g.setColor(Color.BLACK);
        g.drawRect(bankVault.x, bankVault.y, bankVault.width, bankVault.height);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.drawString(vaultHasCash ? "CASH DEPOT" : "CLEANED OUT", bankVault.x + 5, bankVault.y + 35);

        // Security Counters Rendering
        for (Enemy guard : bankSecurity) guard.draw(g);
        for (Bullet b : bullets) b.draw(g);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (player.health > 0) {
            updateMovement();
            updateProjectiles();
            
            // Execute threat loop structures depending on location
            if (currentLocation == LocationState.BANK_INTERIOR) {
                for (Enemy guard : bankSecurity) {
                    guard.updateAI(player, bullets);
                }
            }
        }
        if (shootCooldown > 0) shootCooldown--;
        repaint();
    }

    private void updateMovement() {
        int oldX = player.x;
        int oldY = player.y;

        if (activeKeys.contains(KeyEvent.VK_W)) { player.y -= player.speed; player.direction = "UP"; }
        if (activeKeys.contains(KeyEvent.VK_S)) { player.y += player.speed; player.direction = "DOWN"; }
        if (activeKeys.contains(KeyEvent.VK_A)) { player.x -= player.speed; player.direction = "LEFT"; }
        if (activeKeys.contains(KeyEvent.VK_D)) { player.x += player.speed; player.direction = "RIGHT"; }
        
        if (activeKeys.contains(KeyEvent.VK_SPACE) && shootCooldown <= 0) {
            // Player fired weapon -> false (meaning NOT an enemy bullet)
            bullets.add(new Bullet(player.x + 12, player.y + 12, player.direction, false));
            shootCooldown = 15;
        }

        if (currentLocation == LocationState.TOWN) {
            player.x = Math.max(0, Math.min(player.x, getWidth() - player.width));
            player.y = Math.max(0, Math.min(player.y, getHeight() - player.height));

            for (Building b : townBuildings) {
                if (player.getBounds().intersects(b.doorway)) {
                    returnTownX = b.doorway.x;
                    returnTownY = b.doorway.y + 25;
                    
                    bullets.clear(); // Wipe residual screen layout projectiles
                    if (b.name.equals("MY HOUSE")) currentLocation = LocationState.HOUSE_INTERIOR;
                    else if (b.name.equals("CITY BANK")) currentLocation = LocationState.BANK_INTERIOR;
                    
                    player.x = 385; player.y = 460;
                    activeKeys.clear();
                    return;
                }
                if (player.getBounds().intersects(b.getSolidBounds())) {
                    player.x = oldX; player.y = oldY;
                }
            }
        } else {
            player.x = Math.max(105, Math.min(player.x, 695 - player.width));
            player.y = Math.max(55, Math.min(player.y, 545 - player.height));

            // Bank vault collision detection & interaction trigger logic
            if (currentLocation == LocationState.BANK_INTERIOR && vaultHasCash && player.getBounds().intersects(bankVault)) {
                triggerHeistEvent();
            }

            if (player.getBounds().intersects(interiorExitMat)) {
                currentLocation = LocationState.TOWN;
                player.x = returnTownX; player.y = returnTownY;
                bullets.clear();
                activeKeys.clear();
            }
        }
    }

    private void triggerHeistEvent() {
        vaultHasCash = false;
        alarmTriggered = true;
        player.wallet += 10000; // Credited bank reserves 
        
        // Spawning Tactical Security Intercept Guard units around room array
        bankSecurity.add(new Enemy(150, 120, true));
        bankSecurity.add(new Enemy(580, 120, true));
        bankSecurity.add(new Enemy(250, 300, true));
        bankSecurity.add(new Enemy(480, 300, true));
    }

    private void updateProjectiles() {
        Iterator<Bullet> bIter = bullets.iterator();
        while (bIter.hasNext()) {
            Bullet b = bIter.next();
            b.update();
            
            if (b.x < 0 || b.x > getWidth() || b.y < 0 || b.y > getHeight()) {
                bIter.remove();
                continue;
            }

            // Target collision management by evaluating alignment properties
            if (b.isEnemyBullet) {
                // Threat targeting player checks
                if (b.getBounds().intersects(player.getBounds())) {
                    player.health--;
                    bIter.remove();
                    continue;
                }
            } else {
                // Player targeting local maps arrays threats
                ArrayList<Enemy> activeTargets = (currentLocation == LocationState.BANK_INTERIOR) ? bankSecurity : townEnemies;
                boolean hitRegistered = false;
                
                for (Enemy enemy : activeTargets) {
                    if (enemy.alive && b.getBounds().intersects(enemy.getBounds())) {
                        enemy.health--;
                        if (enemy.health <= 0) enemy.alive = false;
                        bIter.remove();
                        hitRegistered = true;
                        break;
                    }
                }
                if (hitRegistered) continue;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        switch (currentLocation) {
            case TOWN: drawTownOverworld(g); break;
            case HOUSE_INTERIOR: drawHouseInterior(g); break;
            case BANK_INTERIOR: drawBankInterior(g); break;
        }
        
        if (player.health > 0) {
            player.draw(g);
        } else {
            g.setColor(new Color(0, 0, 0, 220));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("HEIST FAILED - WASTED", 200, 300);
            return;
        }
        
        // Dynamic GUI HUD Tracking Matrix
        g.setColor(Color.DARK_GRAY);
        g.fillRect(10, 10, 240, 85);
        g.setColor(Color.WHITE);
        g.drawRect(10, 10, 240, 85);
        
        g.setFont(new Font("Courier New", Font.BOLD, 12));
        g.drawString("LOCATION: " + currentLocation, 15, 25);
        g.drawString("CASH:     $" + player.wallet, 15, 40);
        
        // HP tracking metrics
        g.drawString("VITALITY: ", 15, 55);
        g.setColor(Color.RED);
        g.fillRect(90, 46, 100, 10);
        g.setColor(Color.GREEN);
        g.fillRect(90, 46, player.health * 10, 10);

        g.setColor(Color.WHITE);
        if (currentLocation == LocationState.BANK_INTERIOR) {
            if (vaultHasCash) {
                g.drawString("STATUS:   READY TO RAID VAULT", 15, 75);
            } else {
                long aliveGuards = bankSecurity.stream().filter(u -> u.alive).count();
                g.setColor(aliveGuards > 0 ? Color.ORANGE : Color.GREEN);
                g.drawString(aliveGuards > 0 ? "ALARM:    ELIMINATE GUARDS (" + aliveGuards + ")" : "ALARM:    SECURE! EXIT OPEN", 15, 75);
            }
        } else {
            g.drawString("STATUS:   FREE ROAMING", 15, 75);
        }
    }
}

// --- APP FRAME ENTRY POINT ---
public class SyndicateApp {
    public static void main(String[] args) {
        String name = JOptionPane.showInputDialog("Agent Name:");
        JFrame frame = new JFrame("Syndicate: Crime Town Retro");
        GamePanel game = new GamePanel(name != null && !name.trim().isEmpty() ? name : "Agent", Color.CYAN);
        frame.add(game);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        game.requestFocusInWindow();
    }
}
