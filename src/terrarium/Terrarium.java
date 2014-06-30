/*
 * Copyright (C) 2014 Tim Vaughan <tgvaughan@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package terrarium;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 * Class of objects representing terrarium simulations.
 * 
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class Terrarium {
    
    int width, height;

    InorganicCA inorganicCA;
    
    BufferedImage image;
    Image backgroundImage;
    
    /**
     * Create a new terrarium simulation.
     * 
     * @param width
     * @param height 
     */
    public Terrarium(int width, int height) {
        this.width = width;
        this.height = height;
        
        inorganicCA = new InorganicCAOneCell(width, height);
        
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }
    
    /**
     * Use given image for terrarium background.
     * 
     * @param image 
     */
    public void setBackgroundImage(Image image) {
        backgroundImage = image;
    }
    
    /**
     * Use a solid colour for the terrarium background.
     * 
     * @param colour 
     */
    public void setBackgroundColour(Color colour) {
        BufferedImage bgImage = new BufferedImage(1, 1,
                BufferedImage.TYPE_INT_ARGB);
        
        Graphics g = bgImage.getGraphics();
        g.setColor(colour);
        g.fillRect(0, 0, 1, 1);
        backgroundImage = bgImage;
    }

    /**
     * Render the current state of the terrarium to the chosen graphics object.
     * The image is rendered to the rectangle between (0,0) and (outputWidth,
     * outputHeight).
     * 
     * @param g
     * @param outputWidth
     * @param outputHeight 
     */
    public void render(Graphics g, int outputWidth, int outputHeight) {
        
        // Render inorganic
        for (int i=0; i<height; i++) {
            for (int j=0; j<width; j++) {
                int rgbCol = inorganicCA.getCellState(i, j).colour.getRGB();
                image.setRGB(j, i, rgbCol);
            }
        }
        
        g.drawImage(backgroundImage,0, 0, outputWidth, outputHeight, null);
        g.drawImage(image, 0, 0, outputWidth, outputHeight, null);
    }

    /**
     * Step terrarium state forward by one time unit.
     */
    public void tick() {
        inorganicCA.updateStates();
    }
    
    /**
     * Add circular patch of dirt with chosen radius to the given coordinates.
     * 
     * @param x
     * @param y
     * @param radius 
     */
    public void addDirt(int x, int y, int radius) {
        addState(InorganicCA.CellState.DIRT, x, y, radius);
    }
    
    /**
     * Add circular patch of water with chosen radius to the given coordinates.
     * 
     * @param x
     * @param y
     * @param radius 
     */
    public void addWater(int x, int y, int radius) {
        addState(InorganicCA.CellState.WATER, x, y, radius);
    }
    
    /**
     * Add a circular patch of some state with the chosen radius to the given
     * coordinates.  Note that only cells with states which are empty for the
     * new state are updated.
     * 
     * @param state
     * @param x
     * @param y
     * @param radius
     */
    private void addState(InorganicCA.CellState state, int x, int y, int radius) {
        
        for (int i=y-radius; i<y+radius; i++) {
            if (i<0 || i>=height)
                continue;
            
            for (int j=x-radius; j<x+radius; j++) {
                if (j<=0 || j>=width)
                    continue;
                
                if ((i-y)*(i-y) + (j-x)*(j-x) < radius*radius) {
                    if (inorganicCA.getCellState(i, j).isEmptyFor(state))
                        inorganicCA.setCellState(i, j, state);
                }
            }
        }
    }
    
    /**
     * Produce a string containing the information needed to reconstruct the
     * terrarium.  Used for saving to disk.
     * 
     * @return Serialized representation.
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"inorganic\": ").append(inorganicCA.serialize());
        sb.append("}");
        
        return sb.toString();
    }
}
