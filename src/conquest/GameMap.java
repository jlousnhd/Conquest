package conquest;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import javax.imageio.*;

public class GameMap extends JComponent implements MouseMotionListener, MouseListener {
	
	private static final long serialVersionUID = 1L;
	
	public static final int WIDTH = 1227;
	public static final int HEIGHT = 628;
	
	private static volatile BufferedImage worldImage;
	private static volatile EnumMap<Territory, BufferedImage> territoryImage;
	private static volatile EnumMap<Territory, Point> territoryPoint;
	
	private final HashSet<Integer> colorPool;
	private final HashMap<Integer, Integer> colorMapping;
	private BufferedImage buffered;
	private Territory previousClick;
	private Territory bufferedHighlighted;
	private Point mouseOver;
	private volatile GameData data;
	
	public GameMap(GameData data) {
		
		setPreferredSize(new Dimension(worldImage.getWidth(), worldImage.getHeight()));
		mouseOver = new Point(0, 0);
		
		addMouseListener(this);
		addMouseMotionListener(this);
		
		colorPool = new HashSet<Integer>();
		colorPool.add(0xffa04040);
		colorPool.add(0xff40a040);
		colorPool.add(0xff4040a0);
		colorPool.add(0xff707040);
		colorPool.add(0xff704070);
		colorPool.add(0xff407070);
		
		colorMapping = new HashMap<Integer, Integer>();
		
		setGameData(data);
		
	}
	
	public void setGameData(GameData data) {
		
		synchronized(colorPool) {
			
			this.previousClick = null;
			this.data = data;
			this.buffered = null;
			this.bufferedHighlighted = null;
			
		}
		
		repaint();
		
	}
	
	@Override
	public void paint(Graphics g) {
		
		Graphics2D g2 = (Graphics2D) g;
		
		if(buffered == null || bufferedHighlighted != getHighlightedTerritory())
			render();
		
		g2.drawImage(buffered, 0, 0, null);
		
		if(data == null)
			return;
		
		g2.setColor(Color.BLACK);
		
		for(Entry<Territory, Point> entry : territoryPoint.entrySet()) {
			
			Territory territory = entry.getKey();
			Point point = entry.getValue();
			
			g2.drawString("" + data.territoryArmies(territory), point.x, point.y);
			
		}
		
	}
	
	private void render() {
		
		GameData data = this.data;
		
		BufferedImage image = new BufferedImage(worldImage.getWidth(), worldImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g = (Graphics2D) image.createGraphics();
		g.drawImage(worldImage, 0, 0, null);
		
		g.dispose();
		
		Territory bufferedHighlighted = getHighlightedTerritory();
		
		int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		
		for(Map.Entry<Territory, BufferedImage> entry : territoryImage.entrySet()) {
			
			Territory territory = entry.getKey();
			BufferedImage timage = entry.getValue();
			
			PlayerData owner = data == null ? null : data.territoryOwner(territory);
			
			int color = owner == null ? 0xff808080 : getColorForPlayer(owner.getID());
			
			if(territory == bufferedHighlighted) {
				
				int red = (color >>> 16) & 0xff;
				int grn = (color >>>  8) & 0xff;
				int blu = (color       ) & 0xff;
				
				red = (red + 0xff) / 2;
				grn = (grn + 0xff) / 2;
				blu = (blu + 0xff) / 2;
				
				color = 0xff000000 | (red << 16) | (grn << 8) | blu;
				
			}
			
			byte[] tpixels = ((DataBufferByte) timage.getRaster().getDataBuffer()).getData();
			
			for(int i = 0; i < tpixels.length; ++i) {
				if(tpixels[i] != 0) {
					pixels[i] = color;
				}
			}
			
		}
		
		synchronized(colorPool) {
			
			this.bufferedHighlighted = bufferedHighlighted;
			this.buffered = image;
			
		}
		
	}
	
	private Territory getTerritoryAt(int x, int y) {
		
		if(x < 0 || x >= WIDTH)
			return null;
		
		if(y < 0 || y >= HEIGHT)
			return null;
		
		for(Map.Entry<Territory, BufferedImage> entry : territoryImage.entrySet()) {
			
			Territory territory = entry.getKey();
			BufferedImage image = entry.getValue();
			
			if((image.getRGB(x, y) & 0xffffff) != 0)
				return territory;
			
		}
		
		return null;
		
	}
	
	private Territory getHighlightedTerritory() {
		return getTerritoryAt(mouseOver.x, mouseOver.y);
	}
	
	public int getColorForPlayer(Integer playerID) {
		
		if(playerID == null)
			return 0xff808080;
		
		Integer color = colorMapping.get(playerID);
		if(color != null)
			return color;
		
		if(colorPool.isEmpty())
			return 0;
		
		color = colorPool.iterator().next();
		colorPool.remove(color);
		
		colorMapping.put(playerID, color);
		
		return color;
		
	}
	
	public static void loadImages() throws IOException {
		
		worldImage = ImageIO.read(new File("images/WORLD.png"));
		
		territoryImage = new EnumMap<Territory, BufferedImage>(Territory.class);
		territoryPoint = new EnumMap<Territory, Point>(Territory.class);
		
		for(int i = 0; i < Territory.TERRITORY_COUNT; ++i) {
			
			Territory territory = Territory.fromID(i);
			BufferedImage image = ImageIO.read(new File("images/" + territory.name() + ".png"));
			
			territoryImage.put(territory, image);
			territoryPoint.put(territory, calculateCenter(image));
			
		}
		
		// Manual label tweaks
		territoryPoint.get(Territory.NORTHWEST_TERRITORY).y += 10;
		territoryPoint.get(Territory.CENTRAL_AMERICA).x -= 20;
		territoryPoint.get(Territory.PERU).y += 10;
		territoryPoint.get(Territory.WESTERN_EUROPE).x += 10;
		territoryPoint.get(Territory.SOUTHERN_EUROPE).y += 5;
		territoryPoint.get(Territory.MIDDLE_EAST).x -= 15;
		territoryPoint.get(Territory.KAMCHATKA).y -= 10;
		territoryPoint.get(Territory.JAPAN).x += 10;
		territoryPoint.get(Territory.NEW_GUINEA).y += 5;
		territoryPoint.get(Territory.WESTERN_AUSTRALIA).x -= 10;
		territoryPoint.get(Territory.INDONESIA).y += 10;
		territoryPoint.get(Territory.GREAT_BRITAIN).x -= 5;
		territoryPoint.get(Territory.ICELAND).y -= 5;
		
		
	}
	
	public static Point calculateCenter(BufferedImage image) {
		
		int height = image.getHeight();
		int width = image.getWidth();
		
		DataBufferByte buffer = (DataBufferByte) image.getRaster().getDataBuffer();
		byte[] pixels = buffer.getData();
		
		int sumX = 0;
		int sumY = 0;
		int pixelCount = 0;
		int i = 0;
		
		for(int y = 0; y < height; ++y) {
			
			for(int x = 0; x < width; ++x, ++i) {
				
				if(pixels[i] != 0) {
					
					sumX += x;
					sumY += y;
					++pixelCount;
					
				}
				
			}
			
		}
		
		sumX /= pixelCount;
		sumY /= pixelCount;
		
		return new Point(sumX, sumY);
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {}

	@Override
	public void mouseMoved(MouseEvent e) {
		
		mouseOver = e.getPoint();
		Territory highlighted = getHighlightedTerritory();
		
		if(highlighted != bufferedHighlighted)
			repaint();
		
	}
	
	public boolean territoryClicked(Territory territory, GameData data, GamePhase phase) {
		return false;
	}
	
	public boolean territoryClicked(Territory first, Territory second, GameData data, GamePhase phase) {
		return false;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		
		if(!isEnabled())
			return;
		
		Point pt = e.getPoint();
		Territory territory = getTerritoryAt(pt.x, pt.y);
		
		if(territory != null) {
			
			GameData data = this.data;
			
			if(data != null) {
				
				GamePhase phase = data.phase();
				
				if(territoryClicked(territory, data, phase)) {
					
					previousClick = null;
					return;
					
				}
				
				if(previousClick != null) {
					
					// We could potentially do a pair click
					if(!previousClick.isAdjacentTo(territory)) {
						
						// Nevermind, we can't
						previousClick = territory;
						return;
						
					}
					
					// Let's try a pair click
					if(territoryClicked(previousClick, territory, data, phase))
						previousClick = null;
					
					else
						previousClick = territory;
					
				}
				
			}
			
		}
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

}
